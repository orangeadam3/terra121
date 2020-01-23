package io.github.terra121.dataset;

import java.util.Iterator;
import java.util.LinkedHashMap;

public abstract class TiledDataset {
	
	protected abstract double latToY(double lat);
	protected abstract double lonToX(double lon);
	protected abstract double dataToDouble(int data);
	protected abstract int[] request(Coord tile);
	
	//TODO: better datatypes
    private LinkedHashMap<Coord, int[]> cache;
    protected int numcache;
    protected final int width;
    protected final int height;

    public TiledDataset(int width, int height, int numcache) {
        cache = new LinkedHashMap<Coord, int[]>();
        this.numcache = numcache;
        this.width = width;
        this.height = height;
    }
	
    public double estimateLocal(double lat, double lon) {
        //bound check
        if(lon > 180 || lon < -180 || lat > 85.6 || lat < -85.6) {
            return 0;
        }

        //project coords
        double X = lonToX(lon);
        double Y = latToY(lat);

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
    
    public double[] estimateWithSlope(double lat, double lon) {
        //bound check
        if(lon > 180 || lon < -180 || lat > 85.6 || lat < -85.6) {
            return new double[] {0,0,0};
        }

        //project coords
        double X = lonToX(lon);
        double Y = latToY(lat);

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
        
        double dxl = lr-ll;
        double dxu = ur-ul;
        
        double dyl = ul-ll;
        double dyr = ur-lr;
        
        //outputs
        double[] out = new double[] {
        	(1-v) *(ll*(1-u) + lr*u) + (ul*(1-u) + ur*u)*v,
        	dxl*(1-v) + dxu*v,
        	dyl*(1-u) + dyr*u
        };

        return out;
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
