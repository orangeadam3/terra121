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
    
    //TODO: scales are obsolete with new ScaleProjection type
    protected double scaleX;
    protected double scaleY;

    protected double[] bounds;
    
    //enable smooth interpolation?
    public boolean smooth;

    public TiledDataset(int width, int height, int numcache, GeographicProjection proj, double projScaleX, double projScaleY, boolean smooth) {
        cache = new LinkedHashMap<Coord, int[]>();
        this.numcache = numcache;
        this.width = width;
        this.height = height;
        this.projection = proj;
        this.scaleX = projScaleX;
        this.scaleY = projScaleY;
        this.smooth = smooth;

        bounds = proj.bounds();
        bounds[0] *= scaleX;
        bounds[1] *= scaleY;
        bounds[2] *= scaleX;
        bounds[3] *= scaleY;
    }
    
    public TiledDataset(int width, int height, int numcache, GeographicProjection proj, double projScaleX, double projScaleY) {
    	this(width, height, numcache, proj, projScaleX, projScaleY, false);
    }
	
    public double estimateLocal(double lon, double lat) {

        //basic bound check
        if(!(lon <= 180 && lon >= -180 && lat <= 85 && lat >= -85)) {
            return -2;
        }

        //project coords
        double[] floatCoords = projection.fromGeo(lon, lat);

        if(smooth)
        	return estimateSmooth(floatCoords);
        return estimateBasic(floatCoords);
    }
    
    //new style
    protected double estimateSmooth(double[] floatCoords) {
        
        double X = floatCoords[0]*scaleX - 0.5;
        double Y = floatCoords[1]*scaleY - 0.5;

        //get the corners surrounding this block
        Coord coord = new Coord((int)X, (int)Y);
        
        double u = X-coord.x;
        double v = Y-coord.y;

        double v00 = getOfficialHeight(coord);
        coord.x++;
        double v10 = getOfficialHeight(coord);
        coord.x++;
        double v20 = getOfficialHeight(coord);
        coord.y++;
        double v21 = getOfficialHeight(coord);
        coord.x--;
        double v11 = getOfficialHeight(coord);
        coord.x--;
        double v01 = getOfficialHeight(coord);
        coord.y++;
        double v02 = getOfficialHeight(coord);
        coord.x++;
        double v12 = getOfficialHeight(coord);
        coord.x++;
        double v22 = getOfficialHeight(coord);
        
        //Compute smooth 9-point interpolation on this block
        double result = SmoothBlend.compute(u, v, v00, v01, v02, v10, v11, v12, v20, v21, v22);
        
        if(result>0&&v00<=0&&v10<=0&&v20<=0&&v21<=0&&v11<=0&&v01<=0&&v02<=0&&v12<=0&&v22<=0)
        	return 0; //anti ocean ridges
        
        return result;
    }
    
    //old style
    protected double estimateBasic(double[] floatCoords) {
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

	protected double getOfficialHeight(Coord coord) {

        Coord tile = coord.tile();

        //proper bound check for x
        if(coord.x<=bounds[0] || coord.x>=bounds[2])
            return 0;

        //is the tile that this coord lies on already downloaded?
        int[] img = cache.get(tile);

        if(img == null) {
            //download tile
            img = request(tile);
            cache.put(tile, img); //save to cache cause chances are it will be needed again soon

            //cache is too large, remove the least recent element
            if(cache.size() > numcache) {
                Iterator<?> it = cache.values().iterator();
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
