package io.github.terra121.dataset;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Region {
	public boolean failedDownload = false;
	public OpenStreetMaps.Coord coord;
	public Water water;
	public LandLine southLine;
	public LandLine[] lines;
	public double south, west;
	
	public short[][] indexes;
	public byte[][] states;
	
	public static enum BoundaryType {
		water
	}
	
	public Region(OpenStreetMaps.Coord coord, Water water) {
		this.coord = coord;
		this.water = water;
		
		lines = new LandLine[water.hres];
		for(int i=0; i<lines.length; i++)
			lines[i] = new LandLine();
		
		southLine = new LandLine();
		
		south = coord.y*water.osm.TILE_SIZE;
		west = coord.x*water.osm.TILE_SIZE;
	}
	
	public void addWaterEdge(double slon, double slat, double elon, double elat, long type) {
		
		slat -= south;
		elat -= south;
		slon -= west;
		elon -= west;
		
		slon /= water.osm.TILE_SIZE / water.hres;
		elon /= water.osm.TILE_SIZE / water.hres;
		
		slat /= water.osm.TILE_SIZE / water.hres;
		elat /= water.osm.TILE_SIZE / water.hres;
		
		if(slat<=0 || elat<=0 && (slat>=0 || elat>=0)) {
			if(slat==0)slat = 0.00000001;
			if(elat==0)elat = 0.00000001;
			
			if(elat!=slat) {
				double islope = (elon-slon)/(elat-slat);
				southLine.add(elon - islope*elat, type);
			}
		}
		
		if(slon!=elon) {
			double slope = (elat-slat)/(elon-slon);
			
			int beg = (int) Math.ceil(Math.min(slon, elon));
			int end = (int) Math.floor(Math.max(slon, elon));
			
			if(beg<0)beg = 0;
			if(end>=water.hres) end = water.hres-1;
			
			for(int x=beg; x<=end; x++) {
				lines[x].add(slope*x + (elat - slope*elon), type);
			}
		}
	}
	
	private void addComp(Object[] line, int x) {
		indexes[x] = (short[])line[0];
		states[x] = (byte[])line[1];
	}
	
	public void renderWater(Set<Long> ground) {
		double size = water.osm.TILE_SIZE;
		double ressize = water.osm.TILE_SIZE/water.hres;
		
		indexes = new short[water.hres][];
		states = new byte[water.hres][];
		
		southLine.run(water.hres, ground, (status,x) -> addComp(lines[x].compileBreaks(new HashSet<Long>(status), water.hres),x));
		
		//we are done with these resources, now that they are compiled
		lines = null;
		southLine = null;
		
		/*for(int y=0;y<test.length;y++) {
			for(int x=0;x<test[0].length;x++) {
				char c = test[test.length-y-1][x]>0?' ':'#';
				System.out.print(c+""+c);
			}
			System.out.println();
		}*/
	}
	
	//another fliping binary search, why can't java have a decent fuzzy one built in
	public int getStateIdx(short x, short y) {
		short[] index = indexes[x];
		
		int min = 0;
		int max = index.length;
		
		while(min<max-1) {
			int mid = min + (max-min)/2;
			if(index[mid]<y)
				min = mid;
			else if(index[mid]>y)
				max = mid;
			else return mid;
		}
		return min;
	}
	
	public int hashCode() {
		return coord.hashCode();
	}
	
	public boolean equals(Object other) {
		return (other instanceof Region) && coord.equals(((Region)other).coord);
	}
}
