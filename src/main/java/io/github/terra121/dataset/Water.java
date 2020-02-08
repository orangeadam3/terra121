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

		return region.states[(int)lon][idx];
	}
}
