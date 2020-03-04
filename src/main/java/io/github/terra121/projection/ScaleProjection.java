package io.github.terra121.projection;

public class ScaleProjection extends ProjectionTransform {

	double scaleX, scaleY;
	
	public ScaleProjection(GeographicProjection input, double scaleX, double scaleY) {
		super(input);
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	
	public ScaleProjection(GeographicProjection input, double scale) {
		this(input, scale, scale);
	}
	
	public double[] toGeo(double x, double y) {
		return input.toGeo(x/scaleX, y/scaleY);
	}
	
	public double[] fromGeo(double lon, double lat) {
		double[] p = input.fromGeo(lon, lat);
		p[0] *= scaleX;
		p[1] *= scaleY;
		return p;
	}
	
	public boolean upright() {
		return (scaleY<0)^input.upright();
	}
	
	public double[] bounds() {
		double[] b = input.bounds();
		b[0] *= scaleX;
		b[1] *= scaleY;
		b[2] *= scaleX;
		b[3] *= scaleY;
		return b;
	}
	
	public double metersPerUnit() {
		return input.metersPerUnit()/Math.sqrt((scaleX*scaleX + scaleY*scaleY)/2); //TODO: better transform
	}
}
