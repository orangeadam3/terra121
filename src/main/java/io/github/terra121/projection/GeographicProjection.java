package io.github.terra121.projection;

public class GeographicProjection {
	public double[] toGeo(double x, double y) {
		return new double[] {x,y};
	}
	
	public double[] fromGeo(double lon, double lat) {
		return new double[] {lon, lat};
	}
	
	public double metersPerDegree() {
		return 100000;
	}
}
