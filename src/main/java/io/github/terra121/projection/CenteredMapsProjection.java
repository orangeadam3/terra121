package io.github.terra121.projection;

public class CenteredMapsProjection extends GeographicProjection {
	private static final double TO_RADIANS = Math.PI/180.0;
	
    public double[] toGeo(double x, double y) {
		return new double[] {
				x*180.0,
				(Math.atan(Math.exp(-y*Math.PI))*2 - Math.PI/2)/TO_RADIANS
			};
	}
	
	public double[] fromGeo(double lon, double lat) {
		return new double[] {
				lon/180.0,
				-(Math.log( Math.tan((Math.PI/2 + lat*TO_RADIANS)/2) ) ) / Math.PI
			};
	}
	
	public double[] bounds() {
		return new double[]{-1,-1,1,1};
	}
	
	@Override
	public double metersPerUnit() {
		return Math.cos(30*Math.PI/180)*EARTH_CIRCUMFERENCE/2; //Accurate at about 30 degrees
	}
	
	public boolean upright() {
		return true;
	}
}
