package io.github.terra121.control;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

import io.github.terra121.dataset.Building;
import io.github.terra121.dataset.OpenStreetMaps;
import org.apache.commons.io.IOUtils;

import io.github.terra121.EarthGeneratorSettings;
import io.github.terra121.TerraMod;
import io.github.terra121.control.DynamicOptions.Element;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class EarthGui extends GuiScreen implements DynamicOptions.Handler {

	ResourceLocation map = null;
	ResourceLocation rightmap = null;
	BufferedImage base;
	GeographicProjection projection;
	DynamicOptions settings;
	private DynamicOptions.Element[] settingElems;
	private GuiButton done, cancel, biomemapbutt;
	private BiomeMap biomemap = null;
	
	private int mapsize;
	
	private EarthGeneratorSettings cfg;
	
	GuiCreateWorld guiCreateWorld;
	
	public EarthGui(GuiCreateWorld guiCreateWorld, Minecraft mc) {
		
		cfg = new EarthGeneratorSettings(guiCreateWorld.chunkProviderSettingsJson);
		
		this.mc = mc;
		this.guiCreateWorld = guiCreateWorld;

		InputStream is = getClass().getClassLoader().getResourceAsStream("assets/terra121/data/map.png");
		try {
			base = ImageIO.read(is);
		} catch (IOException e) {
			base = new BufferedImage(512,256,0);
		} finally {
			IOUtils.closeQuietly(is);
		}
		
		String[] projs = (String[])GeographicProjection.projections.keySet().toArray(new String[GeographicProjection.projections.size()]);
		
		settingElems = new DynamicOptions.Element[] {
						cycleButton(6969, "projection", projs, e -> {projectMap(true); return I18n.format("terra121.gui.projection")+": "+I18n.format("terra121.projection."+e);}),
						cycleButton(6968, "orentation", GeographicProjection.Orientation.values(), e -> {projectMap(true); return I18n.format("terra121.gui.orientation")+": "+I18n.format("terra121.orientation."+e.toString());}),
						toggleButton(6967, "smoothblend", null),
						toggleButton(6966, "roads", null),
						toggleButton(6965, "osmwater", null),
						toggleButton(6964, "dynamicbaseheight", null),
						cycleButton(6963, "buildingGenerationType", OpenStreetMaps.BuildingGenerationType.values(), e -> I18n.format("terra121.buildings." + e.toString().toLowerCase())),
						conditionalShowElement(
								cycleButton(6963, "buildingMaterialSetting", Building.BuildingMaterial.values(), e -> I18n.format("terra121.building_material." + e.toString().toLowerCase())),
								nothing -> cfg.settings.buildingGenerationType == OpenStreetMaps.BuildingGenerationType.SHELLS
						),
		};
		projectMap(false);
	}

	private DynamicOptions.ConditionalShowElement conditionalShowElement(DynamicOptions.Element element, Function<Void, Boolean> shouldDraw) {
		return new DynamicOptions.ConditionalShowElement(element, shouldDraw);
	}
	
	private <E> DynamicOptions.CycleButtonElement<E> cycleButton(int id, String field, E[] list, Function<E, String> tostring) {
		try {
			return new DynamicOptions.CycleButtonElement<E>(id, list, EarthGeneratorSettings.JsonSettings.class.getField(field), cfg.settings, tostring);
		} catch (NoSuchFieldException | SecurityException e) {
			TerraMod.LOGGER.error("This should never happen, but find field reflection error");
			e.printStackTrace();
		}
		return null;
	}
	
	//auto format based on field name
	private <E> DynamicOptions.ToggleElement toggleButton(int id, String field, Consumer<Boolean> notify) {
		return toggleButton(id, I18n.format("terra121.gui."+field), field, notify);
	}
	
	private <E> DynamicOptions.ToggleElement toggleButton(int id, String name, String field, Consumer<Boolean> notify) {
		try {
			return new DynamicOptions.ToggleElement(id, name, field==null?null:EarthGeneratorSettings.JsonSettings.class.getField(field), cfg.settings, notify);
		} catch (NoSuchFieldException | SecurityException e) {
			TerraMod.LOGGER.error("This should never happen, but find field reflection error");
			e.printStackTrace();
		}
		return null;
	}
	
	private void projectMap(boolean change) {

		projection = cfg.getNormalizedProjection();
		
		cfg.settings.scaleX = cfg.settings.scaleY = projection.metersPerUnit();
		
		if(map!=null)
			mc.renderEngine.deleteTexture(map);
		else if(rightmap!=null)
			mc.renderEngine.deleteTexture(rightmap);
		
		BufferedImage img = new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);
		
		//scale should be able to fit whole earth inside texture
		double[] bounds = projection.bounds();
		double scale = Math.max(Math.abs(bounds[2]-bounds[0]), Math.abs(bounds[3]-bounds[1]));
		
		int w = img.getWidth();
		int h = img.getHeight();
		
		for(int x=0;x<w;x++) {
			for(int y=0;y<h;y++) {
				//image coords to projection coords
				double X = (x/(double)w)*scale+bounds[0];
				double Y = (y/(double)h)*scale+bounds[1];
				
				//not out of bounds
				if(bounds[0]<=X&&X<=bounds[2]&&bounds[1]<=Y&&Y<=bounds[3]) {
					
					double proj[] = projection.toGeo(X, Y); //projection coords to lon lat

					if(!( proj[0]>=-180 && proj[0]<=180 && proj[1]>=-90 && proj[1]<=90 )) continue; //out of bounds gets a transparent

					if(biomemap!=null)img.setRGB(x,y, biomemap.getColor(proj)); //biome map
					else { //image map
						//lat lon to reference image coords
						int lon = (int) ((proj[0] / 360 + 0.5) * base.getWidth());
						int lat = (int) ((0.5 + proj[1] / 180) * base.getHeight());

						//get pixel from reference image if possible
						if (lon >= 0 && lat >= 0 && lat < base.getHeight() && lon < base.getWidth()) {
							img.setRGB(x, y, base.getRGB(lon, base.getHeight() - lat - 1));
						}
					}
				}
			}
		}
		
		map = this.mc.renderEngine.getDynamicTextureLocation("mapdemo", new DynamicTexture(img));
	}
	
	@Override
	public void initGui() {
		mapsize = height-64;
		if(width-mapsize<200)
			mapsize = width-200;
		if(mapsize<32)
			mapsize = 0;
		/*if(mapsize>0.75*width)
			mapsize = (int)(0.75*width);
		if(mapsize<300)
			mapsize = 0;*/

		settings = new DynamicOptions(mc, width-mapsize, height-32, 32, height-32, 32, this, settingElems);
		done = new GuiButton(69, width-106, height-26, 100, 20, I18n.format("gui.done"));
		cancel = new GuiButton(69, 6, height-26, 100, 20, I18n.format("gui.cancel"));
		biomemapbutt = new GuiButton(69, width-106, 6, 100, 20, I18n.format("terra121.gui.biomemap"));
    }
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawBackground(0xff);
		
		settings.drawScreen(mouseX, mouseY, partialTicks);
		
		//render map texture
		this.mc.renderEngine.bindTexture(map);
		drawScaledCustomSizeModalRect(width-mapsize, (height-mapsize)/2, 0, 0, 1024, 1024, mapsize, mapsize, 1024, 1024);
		
		this.mc.renderEngine.bindTexture(Gui.OPTIONS_BACKGROUND);
		//this.drawTexturedModalRect(0, height-32, 0, 0, width, 32);
		drawScaledCustomSizeModalRect(0, height-32, 0, 0, width, 32, width, 32, 32, 32); //footer, TODO: make not bad
		drawScaledCustomSizeModalRect(0, 0, 0, 0, width, 32, width, 32, 32, 32); //header, TODO: make not bad
		
		done.drawButton(mc, mouseX, mouseY, partialTicks);
		cancel.drawButton(mc, mouseX, mouseY, partialTicks);
		biomemapbutt.drawButton(mc, mouseX, mouseY, partialTicks);
		
		//this.drawCenteredString(this.fontRenderer, "WORK IN PROGRESS", this.width/2, this.height/2, 0x00FF5555);
		
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

	public void mouseClicked(int mouseX, int mouseY, int mouseEvent)
    {
		if(done.mousePressed(mc, mouseX, mouseY)) {
			biomemap = null; //delete biome map
			this.guiCreateWorld.chunkProviderSettingsJson = cfg.toString(); //save settings
            this.mc.displayGuiScreen(this.guiCreateWorld); ///exit
			return;
			
		} else if(cancel.mousePressed(mc, mouseX, mouseY)) {
			biomemap = null; //delete biome map
			this.mc.displayGuiScreen(this.guiCreateWorld); //exit without saving
			return;
		} else if(biomemapbutt.mousePressed(mc,mouseX, mouseY)) {
			biomemap = biomemap==null?new BiomeMap():null; //create biomemap or destroy based on boolean
			projectMap(false);
		}
		
		settings.mouseClicked(mouseX, mouseY, mouseEvent);
    }
	
	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		settings.handleMouseInput();
	}
	
	@Override
	public void onDynOptClick(Element elem) {
	}
}