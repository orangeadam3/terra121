package io.github.terra121.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

public class Water {
    public WaterGround grounding;
    public OpenStreetMaps osm;
    public int hres;

    public HashSet<OpenStreetMaps.Coord> inverts;
    public boolean doingInverts;

    public Water(OpenStreetMaps osm, int horizontalres) throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("assets/terra121/data/ground.dat");
        this.grounding = new WaterGround(is);
        this.osm = osm;
        this.hres = horizontalres;
        this.inverts = new HashSet<>();
        this.doingInverts = false;
    }

    public byte getState(double lon, double lat) {

        Region region = this.osm.regionCache(new double[]{ lon, lat });

        //default if download failed
        if (region == null) {
            return 0;
        }

        //transform to water render res
        lon -= region.west;
        lat -= region.south;
        lon /= OpenStreetMaps.TILE_SIZE / this.hres;
        lat /= OpenStreetMaps.TILE_SIZE / this.hres;

        //System.out.println(lon + " " + lat);

        //TODO: range check
        int idx = region.getStateIdx((short) lon, (short) lat);

        byte state = region.states[(int) lon][idx];

        if (this.doingInverts && (state == 0 || state == 1) && this.inverts.contains(region.coord)) {
            state = state == 1 ? (byte) 0 : (byte) 1; //invert state if in an inverted region
        }

        return state;
    }

    //TODO: more efficient
    public float estimateLocal(double lon, double lat) {
        //bound check
        if (!(lon <= 180 && lon >= -180 && lat <= 80 && lat >= -80)) {
            if (lat < -80) //antartica is land
            {
                return 0;
            }
            return 2; //all other out of bounds is water
        }

        double oshift = OpenStreetMaps.TILE_SIZE / this.hres;
        double ashift = OpenStreetMaps.TILE_SIZE / this.hres;

        //rounding errors fixed by recalculating values from scratch (wonder if this glitch also causes the oddly strait terrain that sometimes appears)
        double Ob = Math.floor(lon / oshift) * oshift;
        double Ab = Math.floor(lat / ashift) * ashift;

        double Ot = Math.ceil(lon / oshift) * oshift;
        double At = Math.ceil(lat / ashift) * ashift;

        float u = (float) ((lon - Ob) / oshift);
        float v = (float) ((lat - Ab) / ashift);

        float ll = this.getState(Ob, Ab);
        float lr = this.getState(Ot, Ab);
        float ur = this.getState(Ot, At);
        float ul = this.getState(Ob, At);

        //all is ocean
        if (ll == 2 || lr == 2 || ur == 2 || ul == 2) {
            if (ll < 2) {
                ll += 1;
            }
            if (lr < 2) {
                lr += 1;
            }
            if (ur < 2) {
                ur += 1;
            }
            if (ul < 2) {
                ul += 1;
            }
        }

        //get perlin style interpolation on this block
        return (1 - v) * (ll * (1 - u) + lr * u) + (ul * (1 - u) + ur * u) * v;
    }
	
	/*public static void main(String args[]) {
		TerraMod.LOGGER = LogManager.getLogger();
		
		OpenStreetMaps osm = new OpenStreetMaps(new GeographicProjection());
		
		double south = 21.2938987;
		double north = 21.3280017;
		double west = -157.6564764;
		double east = -157.6393124;
		int n = 1000;
		
		System.out.println("Rendering...");
    	BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
		
		for(int y=0; y<n; y++) {
			for(int x=0; x<n; x++) {
				double X = west + x*(east-west)/n;
				double Y = south + (n-y-1)*(north-south)/n;
				
				Region b = osm.regionCache(new double[] {X,Y});
				int c = osm.water.getState(X,Y)==2?0xff0000ff:osm.water.getState(X,Y)>0.4?0xffffffff:0;
				//int c = osm.water.grounding.state(b.coord.x, b.coord.y)==0?0xff0000ff:0xffffffff;
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

		
		for(LandLine line: osm.regions.iterator().next().lines) {
			System.out.println(line.breaks);
		}
	}*/
}
