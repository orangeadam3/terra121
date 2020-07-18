package io.github.terra121.dataset;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.io.File;

import javax.imageio.ImageIO;

import io.github.terra121.EarthTerrainProcessor;
import io.github.terra121.TerraConfig;
import io.github.terra121.TerraMod;
import io.github.terra121.projection.MapsProjection;
import scala.xml.Null;

public class Heights extends TiledDataset{
    private int zoom;
    private String url_prefix = TerraConfig.serverTerrain;
    private String file_prefix = EarthTerrainProcessor.localTerrain;
    private boolean lidar = false;

    private Water water;
    
    private double oceanRadius = 2.0/(60*60);
	
    public Heights(int zoom, boolean lidar, boolean smooth, Water water) {
    	super(256, 256, TerraConfig.cacheSize, new MapsProjection(), 1<<(zoom+8), 1<<(zoom+8), smooth);
    	this.zoom = zoom;
    	url_prefix += zoom+"/";
    	file_prefix += zoom+"/";
    	this.water = water;
    	this.lidar = lidar;
    }
    
    public Heights(int zoom, Water water) {
    	this(zoom, false, false, water);
    }

    //request a mapzen tile from amazon, this should only be needed every 2 thousand blocks or so if the cache is large enough
    //TODO: better error handle
    protected int[] request(Coord place, boolean lidar) {
	    	
	    int out[] = new int[256 * 256];

	    for(int i=0; i<5; i++) {
	
	    	
	    	
	    	BufferedImage img = null;
	        InputStream is = null;
	        int lidarzoom = zoom;
	
	        try {
	        	
	        	if(lidar) { //Check if LIDAR data is enabled
	        		
	        		File data = new File(file_prefix + place.x + "/" + place.y + ".png");
	        		if(data.exists()) img = ImageIO.read(data); //img == null if there is no LIDAR data for that coord.
	        		
	        	}
	        	
	        	if(img == null) { //Catches if LIDAR data is disabled or if there is no LIDAR data for that coord.
		        	
		            String urlText = url_prefix + place.x + "/" + place.y + ".png";
		            TerraMod.LOGGER.info(urlText);
		            
		            URL url = new URL(urlText);
		            URLConnection con = url.openConnection();
		            con.addRequestProperty("User-Agent", TerraMod.USERAGENT);
		            is = con.getInputStream();
		            img = ImageIO.read(is);
		            is.close();
		            is = null;
		            
	        	}	            
	
	            if(img == null) { //If img is still null at this point, that's a problem
	                throw new IOException("Invalid image file");
	            }
	            /*
	            if(!lidar) {
		            //Get some sample data files
		            BufferedImage img2 = null;
			        InputStream is2 = null;
		            String urlText2 = url_prefix.substring(0,url_prefix.length()-3) + "15/" + place.x + "/" + place.y + ".png";
		            TerraMod.LOGGER.info(urlText2);
		            URL url2 = new URL(urlText2);
		            URLConnection con2 = url2.openConnection();
		            con2.addRequestProperty("User-Agent", TerraMod.USERAGENT);
		            is2 = con2.getInputStream();
		            img2 = ImageIO.read(is2);
		            is2.close();
		            is2 = null;
		            File outputfile = new File("C:\\Terrain\\15-" + place.x + "-" + place.y + ".png");
		            ImageIO.write(img2, "png", outputfile);
	            }
	            */
	            
	            //compile height data from image, stored in 256ths of a meter units
	            img.getRGB(0, 0, 256, 256, out, 0, 256);
	
	            /*
	            for (int x = 0; x < img.getWidth(); x++) {
	                for (int y = 0; y < img.getHeight(); y++) {
	                    int c = y * 256 + x;
	                    out[c] = (out[c] & 0x00ffffff) - 8388608;
	                    if(zoom > 10 && out[c]<-1500*256) out[c] = 0; //terrain glitch (default to 0), comment this for fun dataset glitches
	                }
	            }
	            */



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

    protected double getOfficialHeight(Coord coord, boolean lidar) {
    	double ret = super.getOfficialHeight(coord, lidar);
    	
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
    	if (data >> 24 != 0){ //check for alpha value
    	data = (data & 0x00ffffff) - 8388608;
			if (zoom > 10 && data<-1500*256)data = 0;
		return data/256.0;
    	}
    	return -10000000;
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
