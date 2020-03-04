package io.github.terra121.projection;

public class SinusoidalProjection extends GeographicProjection {
	
	private static final double TO_RADIANS = Math.PI/180.0;
	
	public double[] toGeo(double x, double y) {
		return new double[] {x/Math.cos(y*TO_RADIANS), y};
	}
	
	public double[] fromGeo(double lon, double lat) {
		return new double[] {lon*Math.cos(lat*TO_RADIANS), lat};
	}
	
	public double metersPerUnit() {
		return EARTH_CIRCUMFERENCE/360.0; //gotta make good on that exact area
	}
}
