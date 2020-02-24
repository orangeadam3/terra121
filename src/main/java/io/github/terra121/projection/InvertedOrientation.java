package io.github.terra121.projection;

public class InvertedOrientation extends ProjectionTransform {

	public InvertedOrientation(GeographicProjection input) {
		super(input);
	}
	
	public double[] toGeo(double x, double y) {
		return input.toGeo(y,x);
	}
	
	public double[] fromGeo(double lon, double lat) {
		double[] p = input.fromGeo(lon, lat);
		double t = p[0];
		p[0] = p[1];
		p[1] = t;
		return p;
	}
	
	public double[] bounds() {
		double[] b = input.bounds();
		return new double[] {b[1],b[0],b[3],b[2]};
	}
}
