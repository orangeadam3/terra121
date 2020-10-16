package io.github.terra121.projection;

public class ScaleProjection extends ProjectionTransform {

    final double scaleX;
    final double scaleY;

    public ScaleProjection(GeographicProjection input, double scaleX, double scaleY) {
        super(input);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public ScaleProjection(GeographicProjection input, double scale) {
        this(input, scale, scale);
    }

    public double[] toGeo(double x, double y) {
        return this.input.toGeo(x / this.scaleX, y / this.scaleY);
    }

    public double[] fromGeo(double lon, double lat) {
        double[] p = this.input.fromGeo(lon, lat);
        p[0] *= this.scaleX;
        p[1] *= this.scaleY;
        return p;
    }

    public boolean upright() {
        return (this.scaleY < 0) ^ this.input.upright();
    }

    public double[] bounds() {
        double[] b = this.input.bounds();
        b[0] *= this.scaleX;
        b[1] *= this.scaleY;
        b[2] *= this.scaleX;
        b[3] *= this.scaleY;
        return b;
    }

    public double metersPerUnit() {
        return this.input.metersPerUnit() / Math.sqrt((this.scaleX * this.scaleX + this.scaleY * this.scaleY) / 2); //TODO: better transform
    }
}
