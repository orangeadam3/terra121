package io.github.terra121.projection;

public class InvertedOrientation extends ProjectionTransform {

    public InvertedOrientation(GeographicProjection input) {
        super(input);
    }

    @Override
    public double[] toGeo(double x, double y) {
        return this.input.toGeo(y, x);
    }

    @Override
    public double[] fromGeo(double lon, double lat) {
        double[] p = this.input.fromGeo(lon, lat);
        double t = p[0];
        p[0] = p[1];
        p[1] = t;
        return p;
    }

    @Override
    public double[] bounds() {
        double[] b = this.input.bounds();
        return new double[]{ b[1], b[0], b[3], b[2] };
    }
}
