package io.github.terra121.projection;

public class UprightOrientation extends ProjectionTransform {

    public UprightOrientation(GeographicProjection input) {
        super(input);
    }

    public double[] toGeo(double x, double y) {
        return this.input.toGeo(x, -y);
    }

    public double[] fromGeo(double lon, double lat) {
        double[] p = this.input.fromGeo(lon, lat);
        p[1] = -p[1];
        return p;
    }

    public boolean upright() {
        return !this.input.upright();
    }

    public double[] bounds() {
        double[] b = this.input.bounds();
        return new double[]{ b[0], -b[3], b[2], -b[1] };
    }
}
