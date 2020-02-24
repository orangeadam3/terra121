package io.github.terra121.projection;

public class UprightOrientation extends ProjectionTransform {

	double base;
	
	public UprightOrientation(GeographicProjection input) {
		super(input);
		double[] b = input.bounds();
		base = b[1]+b[3];
	}
	
	public double[] toGeo(double x, double y) {
		return input.toGeo(x, base-y);
	}
	
	public double[] fromGeo(double lon, double lat) {
		double[] p = input.toGeo(lon, lat);
		p[1] = base-p[1];
		return p;
	}
	
	public boolean upright() {
		return !input.upright();
	}
}
