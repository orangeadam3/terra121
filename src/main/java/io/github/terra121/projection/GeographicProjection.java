package io.github.terra121.projection;

import java.util.HashMap;
import java.util.Map;

//support for various projection types
//the definition used here is something that converts long lat coords to coords on a map (and vice versa)
//this base class does nothing so lon lat is the same as x y
public class GeographicProjection {
	
	public static double EARTH_CIRCUMFERENCE = 40075017;
	
	public static Map<String, GeographicProjection> projections;
	
	static {
		projections = new HashMap<String, GeographicProjection>();
		projections.put("web_mercator", new CenteredMapsProjection());
		projections.put("equirectangular", new GeographicProjection());
		projections.put("sinusoidal", new SinusoidalProjection());
		projections.put("equal_earth", new EqualEarth());
		projections.put("airocean", new Airocean());
		projections.put("transverse_mercator", new TransverseMercatorProjection());
	}
	
	public static enum Orientation {
		none, upright, swapped
	};
	
	public static GeographicProjection orientProjection(GeographicProjection base, Orientation o) {
		if(base.upright()) {
			if(o==Orientation.upright)
				return base;
			base = new UprightOrientation(base);
		}
		
		if(o==Orientation.swapped) {
			return new InvertedOrientation(base);
		} else if(o==Orientation.upright) {
			base = new UprightOrientation(base);
		}
		
		return base;
	}
	
	public double[] toGeo(double x, double y) {
		return new double[] {x,y};
	}
	
	public double[] fromGeo(double lon, double lat) {
		return new double[] {lon, lat};
	}
	
	public double metersPerUnit() {
		return 100000;
	}
	
	public double[] bounds() {
		
		//get max in by using extreme coordinates
		double[] b = new double[] { 
				fromGeo(-180,0)[0],
				fromGeo(0,-90)[1],
				fromGeo(180,0)[0],
				fromGeo(0,90)[1]
			};

		if(b[0]>b[2]) {
			double t = b[0];
			b[0] = b[2];
			b[2] = t;
		}
		
		if(b[1]>b[3]) {
			double t = b[1];
			b[1] = b[3];
			b[3] = t;
		}
		
		return b;
	}
	
	public boolean upright() {
		return fromGeo(0,90)[1]<=fromGeo(0,-90)[1];
	}
}
