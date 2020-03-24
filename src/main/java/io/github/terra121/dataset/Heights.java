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

public class Heights extends TiledDataset{
    private int zoom;
    private String url_prefix = TerraConfig.serverTerrain;

    private Water water;
    
    private double oceanRadius = 2.0/(60*60);
	
    public Heights(int zoom, boolean smooth, Water water) {
    	super(256, 256, TerraConfig.cacheSize, new MapsProjection(), 1<<(zoom+8), 1<<(zoom+8), smooth);
    	this.zoom = zoom;
    	url_prefix += zoom+"/";
    	this.water = water;
    }
    
    public Heights(int zoom, Water water) {
    	this(zoom, false, water);
    }

    //request a mapzen tile from amazon, this should only be needed evrey 2 thousand blocks or so if the cache is large enough
    //TODO: better error handle
    protected int[] request(Coord place) {
        int out[] = new int[256 * 256];

        for(int i=0; i<5; i++) {

            InputStream is = null;

            try {
                String urlText = url_prefix + place.x + "/" + place.y + ".png";
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
                        out[c] = (out[c] & 0x00ffffff) - 8388608;
                        if(zoom > 10 && out[c]<-1500*256) out[c] = 0; //terrain glitch (default to 0), comment this for fun dataset glitches
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

    protected double getOfficialHeight(Coord coord) {
    	double ret = super.getOfficialHeight(coord);
    	
    	//shoreline smoothing
        if(water!=null && ret>-1 && ret != 0 && ret < 200) {
	        double[] proj = projection.toGeo(coord.x/scaleX, coord.y/scaleY); //another projection, i know (horrendous)
	        double mine = water.estimateLocal(proj[0], proj[1]);
	        
	        if(mine>1.4 || ( ret>10 & ( mine>1 ||
	        		water.estimateLocal(proj[0]+oceanRadius, proj[1])>1 || water.estimateLocal(proj[0]-oceanRadius, proj[1])>1 ||
	        		water.estimateLocal(proj[0], proj[1]+oceanRadius)>1 || water.estimateLocal(proj[0], proj[1]-oceanRadius)>1))) {
	            return -1;
            }
    	}
    	return ret;
    }
    
	protected double dataToDouble(int data) {
		return data/256.0;
	}
	
	/*public static void main(String args[]) {
		TerraMod.LOGGER = LogManager.getLogger();
		OpenStreetMaps osm = new OpenStreetMaps(new InvertedGeographic());
		Heights h = new Heights(13, osm.water);
		
		double south = 21.271325;
		double west = -157.694122;
		double north = 21.292964;
		double east = -157.677113;
		int n = 1000;
		
		System.out.println("Rendering...");
    	BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
		
		for(int y=0; y<n; y++) {
			for(int x=0; x<n; x++) {
				double X = west + x*(east-west)/n;
				double Y = south + (n-y-1)*(north-south)/n;
				
				int c = (int) (0xff000000 + ((int)((h.estimateLocal(X, Y))*256)<<16));
				//int c = h.estimateLocal(X, Y)<0?0xff0080ff:0xff00ff00;
				img.setRGB(x, y, c);
				
				//osm.water.getState(-98.923594, 29.564990)
			}
		}
		
	    File outputfile = new File("saved.png");
	    try {
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/
}
