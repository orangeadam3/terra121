package io.github.terra121.projection;

import java.util.HashMap;
import java.util.Map;

//support for various projection types
//the definition used here is something that converts long lat coords to coords on a map (and vice versa)
//this base class does nothing so lon lat is the same as x y
public class GeographicProjection {
	
	public static Map<String, GeographicProjection> projections;
	
	static {
		projections = new HashMap<String, GeographicProjection>();
		projections.put("maps", new MapsProjection());
		projections.put("equirectangular", new GeographicProjection());
		projections.put("sinusoidal", new SinusoidalProjection());
	}
	
	public static enum Orentation {
		none, upright, swapped
	};
	
	public static GeographicProjection orientProjection(GeographicProjection base, Orentation o) {
		if(base.upright()) {
			if(o==Orentation.upright)
				return base;
			base = new UprightOrientation(base);
		}
		
		if(o==Orentation.swapped) {
			return new InvertedOrientation(base);
		} else if(o==Orentation.upright) {
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
		return new double[]{-180,-90,180,90};
	}
	
	public boolean upright() {
		return false;
	}
}
