package io.github.terra121.dataset;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;

import io.github.opencubicchunks.cubicchunks.cubicgen.CustomCubicMod;
import io.github.terra121.projection.ImageProjection;

public class Trees extends TiledDataset {
	public static final String SERVER = "http://50.18.182.188:6080";
	public static final String URL_PREFIX = SERVER + "/arcgis/rest/services/TreeCover2000/ImageServer/exportImage?f=image&bbox=";
	public static final String URL_SUFFIX = "&imageSR=4152&bboxSR=4152&format=tiff&adjustAspectRatio=false&&interpolation=RSP_CubicConvolution&size=256,256";
	
	public static final double BLOCK_SIZE = 16/100000.0;
	public static final double REGION_SIZE = BLOCK_SIZE*256;
	
	public Trees() {
		super(256, 256, 10, new ImageProjection(), 1.0/BLOCK_SIZE, 1.0/BLOCK_SIZE);
	}
	
    /*private static final double TO_RADIANS = Math.PI/180.0;
    private static final double MAP_MULT = (2*Math.PI);
    protected double lonToX(double lon) {
        return MAP_MULT* (lon*TO_RADIANS + Math.PI);
    }

    protected double latToY(double lat) {
        return MAP_MULT * (Math.PI - Math.log(Math.tan((Math.PI/2 + lat*TO_RADIANS)/2)));
    }*/

	protected double dataToDouble(int data) {
		return data/100.0;
	}

	protected int[] request(Coord place) {
		int out[] = new int[256 * 256];

        for(int i=0; i<5; i++) {

            InputStream is = null;

            try {
                String urlText = URL_PREFIX + (place.x*REGION_SIZE - 180) + "," + (90-place.y*REGION_SIZE) + "," + ((place.x+1)*REGION_SIZE - 180) + "," + (90 - (place.y+1)*REGION_SIZE) +URL_SUFFIX;
                System.out.println(urlText);
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

                CustomCubicMod.LOGGER.error("Failed to get tree cover data " + place.x + " " + place.y + " : " + ioe);
            }
        }

        CustomCubicMod.LOGGER.error("Failed too many times, trees will not spawn");
        return out;
	}

}
