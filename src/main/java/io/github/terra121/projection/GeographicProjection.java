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
		projections.put("classic", new InvertedGeographic());
		projections.put("geographic", new MinecraftGeographic());
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
}
