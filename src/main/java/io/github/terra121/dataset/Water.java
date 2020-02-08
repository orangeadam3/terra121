package io.github.terra121.dataset;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.terra121.TerraMod;
import io.github.terra121.dataset.TiledDataset.Coord;
import io.github.terra121.projection.GeographicProjection;

public class Water {
	public WaterGround grounding;
	public OpenStreetMaps osm;
	public int hres;
	
	public Water(OpenStreetMaps osm, int horizontalres) throws IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream("assets/terra121/data/ground.png");
		grounding = new WaterGround(is);
		this.osm = osm;
		this.hres = horizontalres;
	}
	
	public byte getState(double lon, double lat) {
		
		Region region = osm.regionCache(new double[] {lon,lat});
		
		//transform to water render res
		lon -= region.west;
		lat -= region.south;
		lon /= osm.TILE_SIZE / hres;
		lat /= osm.TILE_SIZE / hres;
		
		//System.out.println(lon + " " + lat);
		
		//TODO: range check
		int idx = region.getStateIdx((short)lon, (short)lat);

		return (byte) (region.states[(int)lon][idx]==0?0:1);
	}
	
	//TODO: more efficient
	public float estimateLocal(double lon, double lat) {
		//bound check
        if(lon > 180 || lon < -180 || lat > 85 || lat < -85) {
            return 0;
        }

        double oshift = osm.TILE_SIZE / hres;
        double ashift = osm.TILE_SIZE / hres;
        
        //rounding errors fixed by recalculating values from scratch (wonder if this glitch also causes the oddly strait terrain that sometimes appears)
        double Ob = Math.floor(lon/oshift)*oshift;
        double Ab = Math.floor(lat/ashift)*ashift;
        
        double Ot = Math.ceil(lon/oshift)*oshift;
        double At = Math.ceil(lat/ashift)*ashift;
        
        float u = (float) ((lon-Ob)/oshift);
        float v = (float) ((lat-Ab)/ashift);
        
        float ll = getState(Ob, Ab);
        float lr = getState(Ot, Ab);
        float ur = getState(Ot, At);
        float ul = getState(Ob, At);
        
        //get perlin style interpolation on this block
        return (1-v)*(ll*(1-u) + lr*u) + (ul*(1-u) + ur*u)*v;
	}
}
