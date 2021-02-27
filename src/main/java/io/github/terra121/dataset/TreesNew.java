package io.github.terra121.dataset;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.logging.log4j.LogManager;
import io.github.terra121.TerraConfig;
import io.github.terra121.TerraMod;
import io.github.terra121.projection.MapsProjection;

public class TreesNew extends TiledDataset{
    private int zoom;
	
    public TreesNew(int zoom, boolean smooth) {
    	super(256, 256, TerraConfig.cacheSize, new MapsProjection(), 1<<(zoom+8), 1<<(zoom+8), smooth);
    	this.zoom = zoom;
    }
    
    public TreesNew(int zoom) {
    	this(zoom, false);
    }

    //request a mapzen tile from amazon, this should only be needed evrey 2 thousand blocks or so if the cache is large enough
    //TODO: better error handle
    protected int[] request(Coord place) {
        int out[] = new int[256 * 256];

        for(int i=0; i<5; i++) {

            InputStream is = null;

            try {
                String urlText = TerraConfig.serverTree + zoom + "/" + place.x + "/" + place.y + ".png";
                TerraMod.LOGGER.info(urlText);
                
                URL url = new URL(urlText);
                URLConnection con = url.openConnection();
                con.addRequestProperty("User-Agent", TerraMod.USERAGENT);
                is = con.getInputStream();
                
                BufferedImage img = ImageIO.read(is);
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

            } catch (IOException ioe) {
                if(is!=null) {
                    try {
                        is.close();
                    } catch (IOException e) {}
                }

                TerraMod.LOGGER.error("Failed to get elevation " + place.x + " " + place.y + " : " + ioe);
            }
        }

        TerraMod.LOGGER.error("Failed too many times chunks will be set to 0");
        return out;
    }
    
	protected double dataToDouble(int data) {
		return data/256.0;
	}
}
