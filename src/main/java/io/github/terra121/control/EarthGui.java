package io.github.terra121.control;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.gui.*;
import io.github.terra121.EarthGeneratorSettings;
import io.github.terra121.TerraMod;
import io.github.terra121.control.DynamicOptions.Element;
import io.github.terra121.projection.GeographicProjection;
import io.github.terra121.projection.SinusoidalProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EarthGui extends GuiScreen implements DynamicOptions.Handler {

	ResourceLocation leftmap = null;
	ResourceLocation rightmap = null;
	BufferedImage base;
	GeographicProjection projection;
	DynamicOptions settings;
	DynamicOptions.CycleButtonElement projectionType;
	DynamicOptions.CycleButtonElement orentationType;
	private DynamicOptions.Element[] settingElems;
	private GuiButton done;
	
	private EarthGeneratorSettings cfg;
	
	Map<String, String> alias;
	
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
		
		alias = new HashMap<String, String>();
		
		String[] projs = (String[])GeographicProjection.projections.keySet().toArray(new String[GeographicProjection.projections.size()]);
		
		settingElems = new DynamicOptions.Element[] {
						cycleButton(6969, "projection", projs, e -> {projectMap(); return e;}),
						cycleButton(6968, "orentation", GeographicProjection.Orentation.values(), e -> {projectMap(); cfg.settings.scaleX = cfg.settings.scaleY = cfg.getNormalizedProjection().metersPerUnit(); return e.toString();}),
						};
		
		projectMap();
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
	
	private void projectMap() {

		projection = cfg.getProjection();
		
		if(leftmap!=null)
			mc.renderEngine.deleteTexture(leftmap);
		else if(rightmap!=null)
			mc.renderEngine.deleteTexture(rightmap);
		
		BufferedImage left = new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
		BufferedImage right = new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
		
		double[] bounds = projection.bounds();
		
		int w = left.getWidth()*2;
		int h = left.getHeight();
		
		for(int x=0;x<w;x++) {
			for(int y=0;y<h;y++) {
				double X = (x/(double)w)*(bounds[2]-bounds[0])+bounds[0];
				double Y = (y/(double)h)*(bounds[3]-bounds[1])+bounds[1];
				
				double proj[] = projection.toGeo(X, Y);
				
				int lon = (int)((proj[0]/360 + 0.5)*base.getWidth());
				int lat = (int)((0.5 + proj[1]/180)*base.getHeight());
				
				//System.out.println(X + " " + Y+" "+lon+" "+lat);
				
				if(lon>=0 && lat>=0 && lat < base.getHeight() && lon < base.getWidth()) {
					if(x<w/2)
						left.setRGB(x, y, base.getRGB(lon, base.getHeight()-lat-1));
					else right.setRGB(x-w/2, y, base.getRGB(lon, base.getHeight()-lat-1));
				}
			}
		}
		
		leftmap = this.mc.renderEngine.getDynamicTextureLocation("leftmapdemo", new DynamicTexture(left));
		rightmap = this.mc.renderEngine.getDynamicTextureLocation("rightmapdemo", new DynamicTexture(right));
	}
	
	@Override
	public void initGui() {
		settings = new DynamicOptions(mc, width, height/2, height/2, height-32, 32, this, settingElems);
		done = new GuiButton(69, width-106, height-26, 100, 20, "Done");
    }
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		//this.drawDefaultBackground();
		
		settings.drawScreen(mouseX, mouseY, partialTicks);
		
		//todo remove seams
		this.mc.renderEngine.bindTexture(leftmap);
		this.drawScaledCustomSizeModalRect(this.height/2, 0, 0, 0, 512, 512, this.height/2, this.height/2, 512, 512);
		this.mc.renderEngine.bindTexture(rightmap);
		this.drawScaledCustomSizeModalRect(2*(this.height/2), 0, 0, 0, 512, 512, this.height/2, this.height/2, 512, 512);
		
		this.mc.renderEngine.bindTexture(Gui.OPTIONS_BACKGROUND);
		this.drawTexturedModalRect(0, height-32, 0, 0, width, 32);
		
		done.drawButton(mc, mouseX, mouseY, partialTicks);
		
		this.drawCenteredString(this.fontRenderer, "WORK IN PROGRESS", this.width/2, this.height/2, 0x00FF5555);
		
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

	public void mouseClicked(int mouseX, int mouseY, int mouseEvent)
    {
		if(done.mousePressed(mc, mouseX, mouseY)) {
			this.guiCreateWorld.chunkProviderSettingsJson = cfg.toString();
            this.mc.displayGuiScreen(this.guiCreateWorld);
			return;
		}
		settings.mouseClicked(mouseX, mouseY, mouseEvent);
    }
	
	@Override
	public void onDynOptClick(Element elem) {
	}
}
