package io.github.terra121.projection;

public class ImageProjection extends GeographicProjection {
	public double[] toGeo(double x, double y) {
		return new double[] {x-180, 90-y};
	}
	
	public double[] fromGeo(double lon, double lat) {
		return new double[] {lon+180, 90-lat};
	}
}
