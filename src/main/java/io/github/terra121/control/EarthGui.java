package io.github.terra121.control;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.gui.*;
import io.github.terra121.projection.GeographicProjection;
import io.github.terra121.projection.SinusoidalProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

public class EarthGui extends GuiScreen {

	ResourceLocation leftmap;
	ResourceLocation rightmap;
	BufferedImage base;
	GeographicProjection projection = new SinusoidalProjection();
	
	public EarthGui(GuiCreateWorld guiCreateWorld, Minecraft mc) {
		this.mc = mc;
		InputStream is = getClass().getClassLoader().getResourceAsStream("assets/terra121/data/map.png");
		try {
			base = ImageIO.read(is);
		} catch (IOException e) {
			base = new BufferedImage(512,256,0);
		} finally {
			IOUtils.closeQuietly(is);
		}
		
		projectMap();
	}

	private void projectMap() {
		BufferedImage left = new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
		BufferedImage right = new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
		
		double[] bounds = projection.bounds();
		
		int w = left.getWidth()*2;
		int h = left.getHeight();
		
		for(int x=0;x<w;x++) {
			for(int y=0;y<h;y++) {
				double X = (x/(double)w)*(bounds[2]-bounds[0])+bounds[0];
				double Y = ((h-y-1)/(double)h)*(bounds[3]-bounds[1])+bounds[1];
				
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
		
		leftmap = this.mc.renderEngine.getDynamicTextureLocation("shejan", new DynamicTexture(left));
		rightmap = this.mc.renderEngine.getDynamicTextureLocation("shejan", new DynamicTexture(right));
	}
	
	@Override
	public void initGui() {
    }
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		
		this.mc.renderEngine.bindTexture(leftmap);
		this.drawTexturedModalRect(0, 0, 0, 0, 256, 256);
		this.mc.renderEngine.bindTexture(rightmap);
		this.drawTexturedModalRect(256, 0, 0, 0, 256, 256);
		this.drawCenteredString(this.fontRenderer, "BRUH", this.width/2, this.height/2, 0x00FF5555);
		
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
