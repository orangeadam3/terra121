package io.github.terra121.projection;

public class Airocean extends GeographicProjection {

    private int newton = 5;
    private static final double TO_RADIANS = Math.PI/180.0;

    public double[] toGeo(double x, double y) {
        double[] c = reverseTriangleTransform(x, y);
        c[0] /= TO_RADIANS;
        c[1] /= TO_RADIANS;
        return c;
    }

    public double[] fromGeo(double lon, double lat) {
        return triangleTransform(lon*TO_RADIANS, lat*TO_RADIANS);
    }

    protected static final double ROOT3 = Math.sqrt(3);
    protected static final double Z = Math.sqrt(5 + 2*Math.sqrt(5)) / Math.sqrt(15);
    protected static final double EL6 = (Math.sqrt(8) / Math.sqrt(5 + Math.sqrt(5)))/6;
    protected static final double DVE = Math.sqrt(3 + Math.sqrt(5)) / Math.sqrt(5 + Math.sqrt(5));
    protected static final double R = -3*EL6/DVE;

    protected static double[] triangleTransform(double lambda, double phi) {
        double u = Z*Math.tan(-lambda);
        double v = Z*Math.tan(phi) / Math.cos(lambda);

        double a = Math.atan2(2*v/ROOT3 - EL6, DVE);
        double b = Math.atan2(u -v/ROOT3 - EL6, DVE);
        double c = Math.atan2(-u -v/ROOT3 - EL6, DVE);

        return new double[] {ROOT3*(b-c), 2*a - b - c};
    }

    protected static double[] reverseTriangleTransform(double x, double y) {
        double a, b, c;

        double boff = x/ROOT3;
        double aoff = (y + boff)/2;

        c = 0;
        a = c + aoff;
        b = c + boff;

        for(int i=0; i<5; i++) {
            double f = Math.tan(a) + Math.tan(b) + Math.tan(c) - R;
            double fp = sec2(a) + sec2(b) + sec2(c);

            c -= f/fp;
            a = c + aoff;
            b = c + boff;
        }

        double v = ROOT3*( DVE*Math.tan(a) + EL6 )/2;
        double u = DVE*Math.tan(b) + v/ROOT3 + EL6;

        double lambda = -Math.atan(u/Z);

        return new double[] { lambda, Math.atan(Math.cos(lambda)*v/Z) };
    }

    protected static double sec2(double n) {
        double s = 1/Math.cos(n);
        return s*s;
    }


    public double metersPerUnit() {
        return 100000;
    }

    public double[] bounds() {
        return new double[] {-Math.PI, -Math.PI, Math.PI, Math.PI};
    }

    public static void main(String[] args) {
        double[] xy = triangleTransform(0.3,0.8);

        System.out.println(xy[0]+" "+xy[1]);

        double[] rev = reverseTriangleTransform(0, Math.PI/2);
        System.out.println(rev[0]/TO_RADIANS+" "+rev[1]/TO_RADIANS);
    }
}
