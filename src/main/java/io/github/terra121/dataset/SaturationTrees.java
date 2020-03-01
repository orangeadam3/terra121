package io.github.terra121.dataset;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;

import io.github.terra121.TerraConfig;
import io.github.terra121.TerraMod;

public class SaturationTrees extends Trees {
	
	public String URL_PREFIX = TerraConfig.serverTree + "ForestCover_last/ImageServer/exportImage?f=image&bbox=";
	
	protected int[] request(Coord place) {
		int out[] = new int[256 * 256];

        for(int i=0; i<5; i++) {

            InputStream is = null;

            try {
                String urlText = URL_PREFIX + (place.x*REGION_SIZE - 180) + "," + (90-place.y*REGION_SIZE) + "," + ((place.x+1)*REGION_SIZE - 180) + "," + (90 - (place.y+1)*REGION_SIZE) +URL_SUFFIX;
                TerraMod.LOGGER.info(urlText);
                URL url = new URL(urlText);
                is = url.openStream();
                ByteSourceInputStream by = new ByteSourceInputStream(is, "shits and giggles");
                TiffImageParser p = new TiffImageParser();
                BufferedImage img = p.getBufferedImage(by, new HashMap<String,Object>());
                is.close();
                is = null;
                
                if(img == null) {
                    throw new IOException("Invalid image file");
                }

                //compile height data from image, stored in 256ths of a meter units
                img.getRGB(0, 0, 256, 256, out, 0, 256);

                for (int x = 0; x < img.getWidth(); x++) {
                    for (int y = 0; y < img.getHeight(); y++) {
                        int c = y * 256 + x;
                        int rgb = out[c];
                        
                        if(rgb==0xffffffff||rgb==0||rgb==0xff000000)out[c] = 0; //white black or transparent means no trees
                        else {
                        	//TODO: faster solution
                        	//get saturation value to use as base for tree
                        	int max = Math.max(Math.max(rgb&0xff0000, rgb&0xff00), rgb&0xff);
                        	int min = Math.min(Math.min(rgb&0xff0000, rgb&0xff00), rgb&0xff);
                        	float sat = max/(float)(max-min);
                        	out[c] = (int)(sat*256);
                        }
                    }
                }

                return out;

            } catch (IOException | ImageReadException ioe) {
                if(is!=null) {
                    try {
                        is.close();
                    } catch (IOException e) {}
                }

                TerraMod.LOGGER.error("Failed to get tree cover data " + place.x + " " + place.y + " : " + ioe);
            }
        }

        TerraMod.LOGGER.error("Failed too many times, trees will not spawn");
        return out;
	}
	
	protected double dataToDouble(int data) {
		return data/256.0;
	}
}
