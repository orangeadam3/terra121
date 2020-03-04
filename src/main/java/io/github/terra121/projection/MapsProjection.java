package io.github.terra121.projection;

public class MapsProjection extends GeographicProjection {
	
	private static final double TO_RADIANS = Math.PI/180.0;
	private static final double TAU = (2*Math.PI);
	
    public double[] toGeo(double x, double y) {
		return new double[] {
				(x*TAU - Math.PI)/TO_RADIANS,
				(Math.atan(Math.exp(Math.PI - y*TAU))*2 - Math.PI/2)/TO_RADIANS
			};
	}
	
	public double[] fromGeo(double lon, double lat) {
		return new double[] {
				(lon*TO_RADIANS + Math.PI)/TAU,
				(Math.PI - Math.log( Math.tan((Math.PI/2 + lat*TO_RADIANS)/2) ) ) / TAU
			};
	}
	
	public double[] bounds() {
		return new double[]{0,0,1,1};
	}
	
	public boolean upright() {
		return true;
	}
}
