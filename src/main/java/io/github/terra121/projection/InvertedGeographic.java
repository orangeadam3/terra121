package io.github.terra121.projection;

public class InvertedGeographic extends GeographicProjection {
	public double[] toGeo(double x, double y) {
		return new double[] {y,x};
	}
	
	public double[] fromGeo(double lon, double lat) {
		return new double[] {lat, lon};
	}
}
