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
	
	public static void main(String args[]) {
		MapsProjection mp = new MapsProjection();
		double[] out = mp.fromGeo(0, -30);
		System.out.println((out[0]*1024) + " " + (out[1]*1024));
		
		double[] reout = mp.toGeo(out[0], out[1]);
		System.out.println(reout[0] + " " + reout[1]);
	}
}
