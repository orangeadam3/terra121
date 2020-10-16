package io.github.terra121.control;

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
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

public class EarthGui extends GuiScreen implements DynamicOptions.Handler {

    ResourceLocation map;
    ResourceLocation rightmap;
    BufferedImage base;
    GeographicProjection projection;
    DynamicOptions settings;
    private final DynamicOptions.Element[] settingElems;
    private GuiButton done, cancel, biomemapbutt;
    private BiomeMap biomemap;

    private int mapsize;

    private final EarthGeneratorSettings cfg;

    GuiCreateWorld guiCreateWorld;

    public EarthGui(GuiCreateWorld guiCreateWorld, Minecraft mc) {

        this.cfg = new EarthGeneratorSettings(guiCreateWorld.chunkProviderSettingsJson);

        this.mc = mc;
        this.guiCreateWorld = guiCreateWorld;

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("assets/terra121/data/map.png");
        try {
            this.base = ImageIO.read(is);
        } catch (IOException e) {
            this.base = new BufferedImage(512, 256, 0);
        } finally {
            IOUtils.closeQuietly(is);
        }

        String[] projs = GeographicProjection.projections.keySet().toArray(new String[GeographicProjection.projections.size()]);

        this.settingElems = new DynamicOptions.Element[]{
                this.cycleButton(6969, "projection", projs, e -> {
                    this.projectMap(true);
                    return I18n.format("terra121.gui.projection") + ": " + I18n.format("terra121.projection." + e);
                }),
                this.cycleButton(6968, "orentation", GeographicProjection.Orientation.values(), e -> {
                    this.projectMap(true);
                    return I18n.format("terra121.gui.orientation") + ": " + I18n.format("terra121.orientation." + e.toString());
                }),
                this.toggleButton(6967, "smoothblend", null),
                this.toggleButton(6966, "roads", null),
                this.toggleButton(6965, "osmwater", null),
                this.toggleButton(6964, "dynamicbaseheight", null),
                this.toggleButton(6963, "buildings", null),
        };
        this.projectMap(false);
    }

    private <E> DynamicOptions.CycleButtonElement<E> cycleButton(int id, String field, E[] list, Function<E, String> tostring) {
        try {
            return new DynamicOptions.CycleButtonElement<E>(id, list, EarthGeneratorSettings.JsonSettings.class.getField(field), this.cfg.settings, tostring);
        } catch (NoSuchFieldException | SecurityException e) {
            TerraMod.LOGGER.error("This should never happen, but find field reflection error");
            e.printStackTrace();
        }
        return null;
    }

    //auto format based on field name
    private <E> DynamicOptions.ToggleElement toggleButton(int id, String field, Consumer<Boolean> notify) {
        return this.toggleButton(id, I18n.format("terra121.gui." + field), field, notify);
    }

    private <E> DynamicOptions.ToggleElement toggleButton(int id, String name, String field, Consumer<Boolean> notify) {
        try {
            return new DynamicOptions.ToggleElement(id, name, field == null ? null : EarthGeneratorSettings.JsonSettings.class.getField(field), this.cfg.settings, notify);
        } catch (NoSuchFieldException | SecurityException e) {
            TerraMod.LOGGER.error("This should never happen, but find field reflection error");
            e.printStackTrace();
        }
        return null;
    }

    private void projectMap(boolean change) {

        this.projection = this.cfg.getNormalizedProjection();

        this.cfg.settings.scaleX = this.cfg.settings.scaleY = this.projection.metersPerUnit();

        if (this.map != null) {
            this.mc.renderEngine.deleteTexture(this.map);
        } else if (this.rightmap != null) {
            this.mc.renderEngine.deleteTexture(this.rightmap);
        }

        BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);

        //scale should be able to fit whole earth inside texture
        double[] bounds = this.projection.bounds();
        double scale = Math.max(Math.abs(bounds[2] - bounds[0]), Math.abs(bounds[3] - bounds[1]));

        int w = img.getWidth();
        int h = img.getHeight();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                //image coords to projection coords
                double X = (x / (double) w) * scale + bounds[0];
                double Y = (y / (double) h) * scale + bounds[1];

                //not out of bounds
                if (bounds[0] <= X && X <= bounds[2] && bounds[1] <= Y && Y <= bounds[3]) {

                    double[] proj = this.projection.toGeo(X, Y); //projection coords to lon lat

                    if (!(proj[0] >= -180 && proj[0] <= 180 && proj[1] >= -90 && proj[1] <= 90)) {
                        continue; //out of bounds gets a transparent
                    }

                    if (this.biomemap != null) {
                        img.setRGB(x, y, this.biomemap.getColor(proj)); //biome map
                    } else { //image map
                        //lat lon to reference image coords
                        int lon = (int) ((proj[0] / 360 + 0.5) * this.base.getWidth());
                        int lat = (int) ((0.5 + proj[1] / 180) * this.base.getHeight());

                        //get pixel from reference image if possible
                        if (lon >= 0 && lat >= 0 && lat < this.base.getHeight() && lon < this.base.getWidth()) {
                            img.setRGB(x, y, this.base.getRGB(lon, this.base.getHeight() - lat - 1));
                        }
                    }
                }
            }
        }

        this.map = this.mc.renderEngine.getDynamicTextureLocation("mapdemo", new DynamicTexture(img));
    }

    @Override
    public void initGui() {
        this.mapsize = this.height - 64;
        if (this.width - this.mapsize < 200) {
            this.mapsize = this.width - 200;
        }
        if (this.mapsize < 32) {
            this.mapsize = 0;
        }
		/*if(mapsize>0.75*width)
			mapsize = (int)(0.75*width);
		if(mapsize<300)
			mapsize = 0;*/

        this.settings = new DynamicOptions(this.mc, this.width - this.mapsize, this.height - 32, 32, this.height - 32, 32, this, this.settingElems);
        this.done = new GuiButton(69, this.width - 106, this.height - 26, 100, 20, I18n.format("gui.done"));
        this.cancel = new GuiButton(69, 6, this.height - 26, 100, 20, I18n.format("gui.cancel"));
        this.biomemapbutt = new GuiButton(69, this.width - 106, 6, 100, 20, I18n.format("terra121.gui.biomemap"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0xff);

        this.settings.drawScreen(mouseX, mouseY, partialTicks);

        //render map texture
        this.mc.renderEngine.bindTexture(this.map);
        drawScaledCustomSizeModalRect(this.width - this.mapsize, (this.height - this.mapsize) / 2, 0, 0, 1024, 1024, this.mapsize, this.mapsize, 1024, 1024);

        this.mc.renderEngine.bindTexture(Gui.OPTIONS_BACKGROUND);
        //this.drawTexturedModalRect(0, height-32, 0, 0, width, 32);
        drawScaledCustomSizeModalRect(0, this.height - 32, 0, 0, this.width, 32, this.width, 32, 32, 32); //footer, TODO: make not bad
        drawScaledCustomSizeModalRect(0, 0, 0, 0, this.width, 32, this.width, 32, 32, 32); //header, TODO: make not bad

        this.done.drawButton(this.mc, mouseX, mouseY, partialTicks);
        this.cancel.drawButton(this.mc, mouseX, mouseY, partialTicks);
        this.biomemapbutt.drawButton(this.mc, mouseX, mouseY, partialTicks);

        //this.drawCenteredString(this.fontRenderer, "WORK IN PROGRESS", this.width/2, this.height/2, 0x00FF5555);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseEvent) {
        if (this.done.mousePressed(this.mc, mouseX, mouseY)) {
            this.biomemap = null; //delete biome map
            this.guiCreateWorld.chunkProviderSettingsJson = this.cfg.toString(); //save settings
            this.mc.displayGuiScreen(this.guiCreateWorld); ///exit
            return;

        } else if (this.cancel.mousePressed(this.mc, mouseX, mouseY)) {
            this.biomemap = null; //delete biome map
            this.mc.displayGuiScreen(this.guiCreateWorld); //exit without saving
            return;
        } else if (this.biomemapbutt.mousePressed(this.mc, mouseX, mouseY)) {
            this.biomemap = this.biomemap == null ? new BiomeMap() : null; //create biomemap or destroy based on boolean
            this.projectMap(false);
        }

        this.settings.mouseClicked(mouseX, mouseY, mouseEvent);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.settings.handleMouseInput();
    }

    @Override
    public void onDynOptClick(Element elem) {
    }
}