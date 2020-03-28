package io.github.terra121.dataset;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;

import io.github.terra121.TerraConfig;
import io.github.terra121.TerraMod;
import io.github.terra121.projection.ImageProjection;

public class Trees extends TiledDataset {
	public String URL_PREFIX = TerraConfig.serverTree + "TreeCover2000/ImageServer/exportImage?f=image&bbox=";
	public String URL_SUFFIX = "&imageSR=4152&bboxSR=4152&format=tiff&adjustAspectRatio=false&&interpolation=RSP_CubicConvolution&size=256,256";
	
	public static final double BLOCK_SIZE = 16/100000.0;
	public static final double REGION_SIZE = BLOCK_SIZE*256;
	
	public Trees() {
		super(256, 256, TerraConfig.cacheSize, new ImageProjection(), 1.0/BLOCK_SIZE, 1.0/BLOCK_SIZE);
	}

	protected double dataToDouble(int data) {
		return data/100.0;
	}

	protected int[] request(Coord place) {
		int out[] = new int[256 * 256];

        for(int i=0; i<5; i++) {

            InputStream is = null;

            try {
                String urlText = URL_PREFIX + (place.x*REGION_SIZE - 180) + "," + (90-place.y*REGION_SIZE) + "," + ((place.x+1)*REGION_SIZE - 180) + "," + (90 - (place.y+1)*REGION_SIZE) +URL_SUFFIX;
                TerraMod.LOGGER.info(urlText);

                URL url = new URL(urlText);
                URLConnection con = url.openConnection();
                con.addRequestProperty("User-Agent", TerraMod.USERAGENT);
                is = con.getInputStream();
                
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
                        out[c] = out[c]&0xff;
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

        TerraMod.LOGGER.error("Failed too many times, trees will not spawn. ");
        return out;
	}

}
