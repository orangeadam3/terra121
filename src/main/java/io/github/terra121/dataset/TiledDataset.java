package io.github.terra121.dataset;

import java.util.Iterator;
import java.util.LinkedHashMap;

import io.github.terra121.projection.GeographicProjection;

public abstract class TiledDataset {
	protected abstract double dataToDouble(int data);
	protected abstract int[] request(Coord tile);
	
	//TODO: better datatypes
    private LinkedHashMap<Coord, int[]> cache;
    protected int numcache;
    protected final int width;
    protected final int height;
    
    protected GeographicProjection projection;
    protected double scaleX;
    protected double scaleY;

    public TiledDataset(int width, int height, int numcache, GeographicProjection proj, double projScaleX, double projScaleY) {
        cache = new LinkedHashMap<Coord, int[]>();
        this.numcache = numcache;
        this.width = width;
        this.height = height;
        this.projection = proj;
        this.scaleX = projScaleX;
        this.scaleY = projScaleY;
    }
	
    public double estimateLocal(double lon, double lat) {
        //bound check
        if(lon > 180 || lon < -180 || lat > 85 || lat < -85) {
            return 0;
        }

        //project coords
        double[] floatCoords = projection.fromGeo(lon, lat);
        double X = floatCoords[0]*scaleX;
        double Y = floatCoords[1]*scaleY;

        //get the corners surrounding this block
        Coord coord = new Coord((int)X, (int)Y);
        
        double u = X-coord.x;
        double v = Y-coord.y;
        
        double ll = getOfficialHeight(coord);
        coord.x++;
        double lr = getOfficialHeight(coord);
        coord.y++;
        double ur = getOfficialHeight(coord);
        coord.x--;
        double ul = getOfficialHeight(coord);

        //get perlin style interpolation on this block
        return (1-v)*(ll*(1-u) + lr*u) + (ul*(1-u) + ur*u)*v;
    }

	private double getOfficialHeight(Coord coord) {
        Coord tile = coord.tile();

        //is the tile that this coord lies on already downloaded?
        int[] img = cache.get(tile);

        if(img == null) {
            //download tile
            img = request(tile);
            cache.put(tile, img); //save to cache cause chances are it will be needed again soon

            //cache is too large, remove the least recent element
            if(cache.size() > numcache) {
                Iterator it = cache.values().iterator();
                it.next();
                it.remove();
            }
        }
        
        //get coord from tile and convert to meters (divide by 256.0)
        return dataToDouble(img[width*(coord.y%height) + coord.x%width]);
    }

	//integer coordinate class for tile coords and pixel coords
    protected class Coord {
        public int x;
        public int y;

        private Coord tile() {
            return new Coord(x/width, y/height);
        }

        private Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int hashCode() {
            return (x * 79399) + (y * 100000);
        }

        public boolean equals(Object o) {
            Coord c = (Coord) o;
            return c.x == x && c.y == y;
        }
        
        public String toString() {
        	return "("+x+", "+y+")";
        }

    }
}
