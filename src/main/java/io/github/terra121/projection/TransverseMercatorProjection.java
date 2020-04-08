package io.github.terra121.projection;

public class TransverseMercatorProjection extends GeographicProjection {
    public static final double zoneWidth = Math.toRadians(6.0);

    public static double getCentralMeridian(double longitude) {
        return (Math.floor(longitude / zoneWidth) + 0.5) * zoneWidth;
    }

    public double[] fromGeo(double lon, double lat) {
        double lam = Math.toRadians(lon);
        double phi = Math.toRadians(lat);
        double centralMeridian = getCentralMeridian(lam);
        lam -= centralMeridian;

        double b = Math.cos(phi) * Math.sin(lam);
        double x = Math.log((1.0 + b) / (1.0 - b)) / 2;
        double y = Math.atan2(Math.tan(phi), Math.cos(lam));
        x += centralMeridian;
        return new double[] {x, y};
    }

    public double[] toGeo(double x, double y) {
        double centralMeridian = getCentralMeridian(x);
        x -= centralMeridian;
        double lam = Math.atan2(Math.sinh(x), Math.cos(y)) + centralMeridian;
        double phi = Math.asin(Math.sin(y) / Math.cosh(x));
        double lon = Math.toDegrees(lam);
        double lat = Math.toDegrees(phi);
        return new double[] {lon, lat};
    }

    private static final double metersPerUnit = EARTH_CIRCUMFERENCE / (2 * Math.PI);

    @Override
    public double metersPerUnit() {
        return metersPerUnit;
    }
}
