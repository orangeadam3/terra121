package io.github.terra121.projection;

public class UprightOrientation extends ProjectionTransform {

	public UprightOrientation (GeographicProjection input) {
		super(input);
	}

	public double[] toGeo(double x, double y) {
		return input.toGeo(x, -y);
	}
	
	public double[] fromGeo(double lon, double lat) {
		double[] p = input.fromGeo(lon, lat);
		p[1] = -p[1];
		return p;
	}
	
	public boolean upright() {
		return !input.upright();
	}
}
