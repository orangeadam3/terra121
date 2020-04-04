package io.github.terra121.projection;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

public class Airocean extends GeographicProjection {

    private int newton = 5;
    private static final double TO_RADIANS = Math.PI/180.0;
    protected static final double ROOT3 = Math.sqrt(3);

    protected static double[] VERT = new double[24];
    static {

        VERT[0] = 0; VERT[1] = 90; VERT[2] = 0; VERT[3] = -90;
        double phi = Math.atan(0.5);

        for(int i=0; i<10; i++) {
            VERT[4 + 2*i] = (( i*36 + 180 ) % 360 - 180)*TO_RADIANS;
            VERT[4 + 2*i + 1] = (i & 1) == 1 ? phi : -phi;
        }
    }

    protected static final int[] ISO = new int[] {
    0, 3, 11,
    0, 5, 3,
    0, 7, 5,
    0, 9, 7,
    0, 11, 9, // North
    2, 11, 3,
    3, 4, 2,
    4, 3, 5,
    5, 6, 4,
    6, 5, 7,
    7, 8, 6,
    8, 7, 9,
    9, 10, 8,
    10, 9, 11,
    11, 2, 10, // Equator
    1, 2, 4,
    1, 4, 6,
    1, 6, 8,
    1, 8, 10,
    1, 10, 2};

    protected static final double[] CENTERS = new double[60];
    protected static final double[] CENTERS_SPHERE = new double[40];

    static {
        for (int i = 0; i < 20; i++) {
            double[] a = cart(VERT[2*ISO[i*3]], VERT[2*ISO[i*3]+1]);
            double[] b = cart(VERT[2*ISO[i*3+1]], VERT[2*ISO[i*3+1]+1]);
            double[] c = cart(VERT[2*ISO[i*3+2]], VERT[2*ISO[i*3+2]+1]);

            double xsum = a[0] + b[0] + c[0];
            double ysum = a[1] + b[1] + c[1];
            double zsum = a[2] + b[2] + c[2];

            double mag = Math.sqrt(xsum*xsum + ysum*ysum + zsum*zsum);

            CENTERS[3*i] = xsum/mag;
            CENTERS[3*i+1] = ysum/mag;
            CENTERS[3*i+2] = zsum/mag;

            CENTERS_SPHERE[2*i] = Math.atan2(ysum,xsum);
            CENTERS_SPHERE[2*i+1] = Math.atan2(Math.sqrt(xsum*xsum + ysum*ysum),zsum) - Math.PI/2; //why add extra pi? tan symetry or some?
        }
    }

    protected static double[] cart(double lambda, double phi) {
        phi += Math.PI / 2;
        double sinphi = Math.sin(phi);
        return new double[]{sinphi * Math.cos(lambda), sinphi * Math.sin(lambda), Math.cos(phi)};
    }

    protected static int findTriangle(double lambda, double phi) {

        double[] xyz = cart(lambda, phi);

        double min = Double.MAX_VALUE;
        int face = 0;

        for(int i=0; i<20; i++) {
            double xd = CENTERS[3*i]-xyz[0];
            double yd = CENTERS[3*i+1]-xyz[1];
            double zd = CENTERS[3*i+2]-xyz[2];

            double dissq = xd*xd + yd*yd + zd*zd;
            if(dissq<min) {
                face = i;
                min = dissq;
            }
        }

        return face;
    }

    public static double[] MAP_ROTATION = new double[] {
            0,1,0,1,1,
            0,1,0,1,0,1,0,1,0,1,
            0,0,0,1,1,
    };

    public static double[] CENTER_MAP = new double[] {
            -3,7,
            -2,5,
            -1,7,
            2,5,
            4,5,
            -4,1,
            -3,-1,
            -2,1,
            -1,-1,
            0,1,
            1,-1,
            2,1,
            3,-1,
            4,1,
            5,-1, //14, inaccurate
            -3,-3,
            -1,-3,
            1,-3,
            2,-7,
            -4, -7, //19, inaccurate
    };

    static {

        double unit = 4.462860956070224; //TODO: wah, dis

        for(int i=0; i<20; i++) {
            CENTER_MAP[2*i] *= 0.5*unit;
            CENTER_MAP[2*i+1] *= unit/6;
            MAP_ROTATION[i] *= Math.PI;
        }
    }

    protected static int findMapTriangle(double x, double y) {

        double min = Double.MAX_VALUE;
        int face = 0;

        for(int i=0; i<20; i++) {
            double xd = CENTER_MAP[2*i]-x;
            double yd = CENTER_MAP[2*i+1]-y;

            double dissq = xd*xd + yd*yd;
            if(dissq<min) {
                face = i;
                min = dissq;
            }
        }

        return face;
    }

    double[] xRot(double lambda, double phi, double rot) {
        double c[] = cart(lambda, phi);

        double y = c[1];
        c[1] = y*Math.cos(rot) - c[2]*Math.sin(rot);
        c[2] = y*Math.sin(rot) + c[2]*Math.cos(rot);

        double mag = Math.sqrt(c[0]*c[0] + c[1]*c[1] + c[2]*c[2]);
        c[0] /= mag; c[1] /= mag; c[2] /= mag;

        return new double[] {
                Math.atan2(c[1],c[0]),
                Math.atan2(Math.sqrt(c[0]*c[0] + c[1]*c[1]), c[2]) - Math.PI/2
        };
    }

    public double[] fromGeo(double lon, double lat) {
        lon *= TO_RADIANS;
        lat *= TO_RADIANS;

        int face = findTriangle(lon, lat);

        //rotate center to origin
        lon -= CENTERS_SPHERE[2*face];
        lat -= CENTERS_SPHERE[2*face+1];

        //TODO: rotate right amount

        double[] out = triangleTransform(lon, lat);

        //flip triangle to correct orientation
        double theta = MAP_ROTATION[face];
        out[0] = out[0]*Math.cos(theta) - out[1]*Math.sin(theta);
        out[1] = out[1]*Math.cos(theta) + out[0]*Math.sin(theta);

        //translate face to right pos
        out[0] += CENTER_MAP[2*face];
        out[1] += CENTER_MAP[2*face+1];

        return out;
    }

    public double[] toGeo(double x, double y) {

        //get face
        int face = findMapTriangle(x,y);

        double theta = (Math.abs(CENTER_MAP[2*face+1] - 52.62) < 1 || Math.abs(CENTER_MAP[2*face+1] + 10.81) < 1 ? 0 : 60)*TO_RADIANS;

        //translate face to origin
        x -= CENTER_MAP[2*face];
        y -= CENTER_MAP[2*face+1];

        double t = x;
        x = x*Math.cos(-theta) - y*Math.sin(-theta);
        y = y*Math.cos(-theta) + t*Math.sin(-theta);

        //TODO: flip triangle to correct orientation

        //project back onto sphere (sloooooow)
        double[] c = reverseTriangleTransform(x, y);

        theta = Math.atan2(VERT[2 * ISO[30 + 0]] - CENTERS_SPHERE[20], VERT[2 * ISO[30 + 0] + 1] - CENTERS_SPHERE[21])+Math.PI/6;

        //TODO: rotate right amount
        //c = xRot(c[0],c[1], (Math.PI/6)*TO_RADIANS);

        //move back into place on globe
        c[0] += CENTERS_SPHERE[2*face];
        c[1] += CENTERS_SPHERE[2*face+1];

        //c[0] += 71.467459*TO_RADIANS;
        //c[1] += 34.417935*TO_RADIANS;

        while (c[0] > Math.PI) c[0] -= 2 * Math.PI;
        while (c[0] < -Math.PI) c[0] += 2 * Math.PI;
        //while (c[1] > Math.PI/2) c[1] -= 2 * Math.PI;
        //while (c[1] < -Math.PI/2) c[1] += 2 * Math.PI;

        c[0] /= TO_RADIANS;
        c[1] /= TO_RADIANS;
        return c;
    }

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
        return new double[] {-2*Math.PI, -2*Math.PI, 2*Math.PI, 2*Math.PI};
    }

    public static void main(String[] args) throws IOException {

        /*System.out.println(CENTERS_SPHERE[20]/TO_RADIANS);
        System.out.println(CENTERS_SPHERE[21]/TO_RADIANS);
        System.out.println(VERT[2*ISO[30]]+" "+VERT[2*ISO[30]+1]);
        System.out.println(VERT[2*ISO[31]]+" "+VERT[2*ISO[31]+1]);
        System.out.println(VERT[2*ISO[32]]+" "+VERT[2*ISO[32]+1]);*/

        /*for(int x=0; x<3; x++) {

            System.out.println(VERT[2 * ISO[30 + x]]/TO_RADIANS+" "+VERT[2 * ISO[30 + x] + 1]/TO_RADIANS);
            System.out.println(Math.atan2(VERT[2 * ISO[30 + x]], VERT[2 * ISO[30 + x] + 1]) / TO_RADIANS);
        }*/

        Airocean projection = new Airocean();

        BufferedImage base;

        InputStream is = new FileInputStream("../../../../../resources/assets/terra121/data/map.png");
        base = ImageIO.read(is);

        BufferedImage img = new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);

        //scale should be able to fit whole earth inside texture
        double[] bounds = projection.bounds();
        double scale = Math.max(Math.abs(bounds[2]-bounds[0]), Math.abs(bounds[3]-bounds[1]));

        int w = img.getWidth();
        int h = img.getHeight();

        for(int x=0;x<w;x++) {
            for(int y=0;y<h;y++) {
                //image coords to projection coords
                double X = (x/(double)w)*scale+bounds[0];
                double Y = (y/(double)h)*scale+bounds[1];

                //not out of bounds
                if(bounds[0]<=X&&X<=bounds[2]&&bounds[1]<=Y&&Y<=bounds[3]) {

                    double proj[] = projection.toGeo(X, Y); //projection coords to lon lat

                    //lat lon to reference image coords
                    int lon = (int)((proj[0]/360 + 0.5)*base.getWidth());
                    int lat = (int)((0.5 + proj[1]/180)*base.getHeight());

                    //get pixel from reference image if possible
                    if(lon>=0 && lat>=0 && lat < base.getHeight() && lon < base.getWidth()) {
                        //if(lon!=1024 || lat!=512)
                        //System.out.println(lon+" "+lat);
                        img.setRGB(x, y, base.getRGB(lon, base.getHeight()-lat-1));
                    } //else System.out.println(proj[0]+" "+proj[1]+" "+lon+" "+lat);
                }
            }
        }

        ImageIO.write(img, "png", new File("out.png"));
    }
}
