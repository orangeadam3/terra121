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

    protected static double ARC = 2*Math.asin(Math.sqrt(5-Math.sqrt(5))/Math.sqrt(10));

    protected static final double TO_RADIANS = Math.PI/180.0;
    protected static final double ROOT3 = Math.sqrt(3);

    private int newton = 5;

    protected static double[] VERT = new double[] {
        10.536199, 64.700000,
        -5.245390, 2.300882,
        58.157706, 10.447378,
        122.300000, 39.100000,
        -143.478490, 50.103201,
        -67.132330, 23.717925,
        36.521510, -50.103200,
        112.867673, -23.717930,
        174.754610, -2.300882,
        -121.842290, -10.447350,
        -57.700000, -39.100000,
        -169.463800, -64.700000,
    };

    protected static final int[] ISO = new int[] {
            2, 1, 6,
            1, 0, 2,
            0, 1, 5,
            1, 5, 10,
            1, 6, 10,
            7, 2, 6,
            2, 3, 7,
            3, 0, 2,
            0, 3, 4,
            4, 0, 5, //9, qubec
            5, 4, 9,
            9, 5, 10,
            10, 9, 11,
            11, 6, 10,
            6, 7, 11,
            8, 3, 7,
            8, 3, 4,
            8, 4, 9,
            9, 8, 11,
            7, 8, 11,
            11, 6, 7, //child of 14
            3, 7, 8, //child of 15
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
            5,-1, //14, left side, right to be cut
            -3,-5,
            -1,-5,
            1,-5,
            2,-7,
            -4, -7,
            -5,-5, //20, pseudo triangle, child of 14
            -2,-7, //21 , pseudo triangle, child of 15
    };

    public static byte[] FLIP_TRIANGLE = new byte[] {
            1,0,1,0,0,
            1,0,1,0,1,0,1,0,1,0,
            1,1,1,0,0,
            1,0,
    };

    protected static final double[] CENTROID = new double[66];
    protected static final double[] ROTATION_MATRIX = new double[198];
    protected static final double[] INVERSE_ROTATION_MATRIX = new double[198];

    static {

        for(int i=0; i<22; i++) {
            CENTER_MAP[2*i] *= 0.5*ARC;
            CENTER_MAP[2*i+1] *= ARC*ROOT3/12;
        }
    }

    static {

        for(int i=0; i< 12; i++) {
            VERT[2*i+1] = 90-VERT[2*i+1];

            VERT[2*i] *= TO_RADIANS;
            VERT[2*i+1] *= TO_RADIANS;
        }

        for (int i = 0; i < 22; i++) {
            double[] a = cart(VERT[2*ISO[i*3]], VERT[2*ISO[i*3]+1]);
            double[] b = cart(VERT[2*ISO[i*3+1]], VERT[2*ISO[i*3+1]+1]);
            double[] c = cart(VERT[2*ISO[i*3+2]], VERT[2*ISO[i*3+2]+1]);

            double xsum = a[0] + b[0] + c[0];
            double ysum = a[1] + b[1] + c[1];
            double zsum = a[2] + b[2] + c[2];

            double mag = Math.sqrt(xsum*xsum + ysum*ysum + zsum*zsum);

            CENTROID[3*i] = xsum/mag;
            CENTROID[3*i+1] = ysum/mag;
            CENTROID[3*i+2] = zsum/mag;

            double clon = Math.atan2(ysum,xsum);
            double clat = Math.atan2(Math.sqrt(xsum*xsum + ysum*ysum),zsum);

            double v[] = new double[] {VERT[2*ISO[i*3]],VERT[2*ISO[i*3]+1]};
            v = yRot(v[0] - clon, v[1], -clat);

            produceZYZRotationMatrix(ROTATION_MATRIX, i*9, -clon, -clat, (Math.PI/2) - v[0]);
            produceZYZRotationMatrix(INVERSE_ROTATION_MATRIX, i*9, v[0] - (Math.PI/2), clat, clon);
        }
    }

    public static void produceZYZRotationMatrix(double[] out, int offset, double a, double b, double c) {

        double sina = Math.sin(a), cosa = Math.cos(a), sinb = Math.sin(b), cosb = Math.cos(b), sinc = Math.sin(c), cosc = Math.cos(c);

        out[offset+0] = cosa*cosb*cosc - sinc*sina;
        out[offset+1] = - sina*cosb*cosc - sinc*cosa;
        out[offset+2] = cosc*sinb;

        out[offset+3] = sinc*cosb*cosa + cosc*sina;
        out[offset+4] = cosc*cosa - sinc*cosb*sina;
        out[offset+5] = sinc*sinb;

        out[offset+6] = -sinb*cosa;
        out[offset+7] = sinb*sina;
        out[offset+8] = cosb;
    }


    protected static double[] cart(double lambda, double phi) {
        double sinphi = Math.sin(phi);
        return new double[]{sinphi * Math.cos(lambda), sinphi * Math.sin(lambda), Math.cos(phi)};
    }

    protected static int findTriangle(double x, double y, double z) {

        double min = Double.MAX_VALUE;
        int face = 0;

        for(int i=0; i<20; i++) {
            double xd = CENTROID[3*i]-x;
            double yd = CENTROID[3*i+1]-y;
            double zd = CENTROID[3*i+2]-z;

            double dissq = xd*xd + yd*yd + zd*zd;
            if(dissq<min) {

                if(dissq<0.1) //TODO: enlarge radius
                    return i;

                face = i;
                min = dissq;
            }
        }

        return face;
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

    protected static final int[] FACE_ON_GRID = new int[] {
        -1, -1,  0,  1,  2, -1, -1,  3, -1,  4, -1,
        -1,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
        20, 19, 15, 21, 16, -1, 17, 18, -1, -1, -1,
    };

    protected static int findTriangleGrid(double x, double y) {

        //cast equiladeral triangles to 45 degreee right triangles (side length of root2)
        double xp = x/ARC;
        double yp = y/(ARC*ROOT3);

        int row;
        if(yp>-0.25) {
            if(yp<0.25) { //middle
                row = 1;
            }
            else if(yp<=0.75){ //top
                row = 0;
                yp = 0.5-yp; //translate to middle and flip
            }
            else return -1;
        } else if (yp>=-0.75) { //bottom
            row = 2;
            yp = -yp-0.5; //translate to middle and flip
        } else return -1;

        yp += 0.25; //change origin to vertex 4, to allow grids to allign

        //rotate coords 45 degrees so left and right sides of the triangle become the x/y axies (also side lengths are now 1)
        double xr = xp - yp;
        double yr = xp + yp;

        //assign a order to what grid along the y=x line it is
        int gx = (int)Math.floor(xr);
        int gy = (int)Math.floor(yr);

        int col = 2*gx + (gy != gx ? 1 : 0) + 6;

        //out of bounds
        if(col<0 || col>=11)
            return -1;

        return FACE_ON_GRID[row*11 + col]; //get face at this position
    }

    protected static final double Z = Math.sqrt(5 + 2*Math.sqrt(5)) / Math.sqrt(15);
    protected static final double EL = Math.sqrt(8) / Math.sqrt(5 + Math.sqrt(5));
    protected static final double EL6 = EL/6;
    protected static final double DVE = Math.sqrt(3 + Math.sqrt(5)) / Math.sqrt(5 + Math.sqrt(5));
    protected static final double R = -3*EL6/DVE;

    protected double[] triangleTransform(double x, double y, double z) {

        double S = Z/z;

        double xp = S*x;
        double yp = S*y;

        double a = Math.atan((2*yp/ROOT3 - EL6)/ DVE); //ARC/2 terms cancel
        double b = Math.atan((xp - yp/ROOT3 - EL6)/ DVE);
        double c = Math.atan((- xp - yp/ROOT3 - EL6)/ DVE);

        return new double[] {0.5*(b-c), (2*a - b - c)/(2*ROOT3)};
    }

    protected double[] inverseTriangleTransformNewton(double xpp, double ypp) {

        //a & b are linearly related to c, so using the tan of sum formula we know: tan(c+off) = (tanc + tanoff)/(1-tanc*tanoff)
        double tanaoff = Math.tan(ROOT3*ypp + xpp); // a = c + root3*y'' + x''
        double tanboff = Math.tan(2*xpp); // b = c + 2x''

        double anumer = tanaoff*tanaoff + 1;
        double bnumer = tanboff*tanboff + 1;

        //we will be solving for tanc, starting at t=0, tan(0) = 0
        double tana = tanaoff, tanb = tanboff, tanc = 0;

        double adenom = 1, bdenom = 1;

        //double fp = anumer + bnumer + 1; //derivative relative to tanc

        //int i = newton;
        for(int i=0; i<newton; i++) {
            double f = tana + tanb + tanc - R; //R = tana + tanb + tanc
            double fp = anumer*adenom*adenom + bnumer*bdenom*bdenom + 1; //derivative relative to tanc

            //TODO: fp could be simplified on first loop: 1 + anumer + bnumer

            tanc -= f/fp;

            adenom = 1/(1-tanc*tanaoff);
            bdenom = 1/(1-tanc*tanboff);

            tana = (tanc + tanaoff)*adenom;
            tanb = (tanc + tanboff)*bdenom;
        }

        //simple reversal algebra based on tan values
        double yp = ROOT3*( DVE*tana + EL6 )/2;
        double xp = DVE*tanb + yp/ROOT3 + EL6;

        //x = z*xp/Z, y = z*yp/Z, x^2 + y^2 + z^2 = 1
        double xpoZ = xp/Z;
        double ypoZ = yp/Z;

        double z = 1/Math.sqrt(1 + xpoZ*xpoZ + ypoZ*ypoZ);

        return new double[] {z*xpoZ, z*ypoZ, z};
    }

    protected double[] inverseTriangleTransformCbrt(double xpp, double ypp) {
        //a & b are linearly related to c, so using the tan of sum formula we know: tan(c+off) = (tanc + tanoff)/(1-tanc*tanoff)
        double tanaoff = Math.tan(ROOT3*ypp + xpp); // a = c + root3*y'' + x''
        double tanboff = Math.tan(2*xpp); // b = c + 2x''

        //using a derived cubic equation and cubic formula
        double l = tanboff * tanaoff;
        double m = -(R * tanboff * tanaoff + 2 * tanboff +  2 * tanaoff);
        double n = 3 + R * tanboff + R * tanaoff - 2 * tanboff * tanaoff;
        double o = tanboff + tanaoff - R;

        double p = - m / (3*l);
        double q = p*p*p + (m * n - 3 * l * o) / (6 * l*l);
        double r = n / (3*l);

        double rmpp = r - p*p;
        double imag = Math.sqrt(-(q*q + rmpp*rmpp*rmpp));
        double mag = Math.sqrt(imag*imag+q*q);

        double b = Math.atan2(imag,q);

        double tanc = 2*Math.cbrt(mag)*Math.cos((b/3)) + p;

        double tana = (tanc + tanaoff)/(1-tanc*tanaoff);
        double tanb = (tanc + tanboff)/(1-tanc*tanboff);

        //simple reversal algebra based on tan values
        double yp = ROOT3*( DVE*tana + EL6 )/2;
        double xp = DVE*tanb + yp/ROOT3 + EL6;

        //x = z*xp/Z, y = z*yp/Z, x^2 + y^2 + z^2 = 1
        double xpoZ = xp/Z;
        double ypoZ = yp/Z;

        double z = 1/Math.sqrt(1 + xpoZ*xpoZ + ypoZ*ypoZ);

        return new double[] {z*xpoZ, z*ypoZ, z};
    }

    protected double[] inverseTriangleTransformCbrtNewton(double xpp, double ypp) {
        //a & b are linearly related to c, so using the tan of sum formula we know: tan(c+off) = (tanc + tanoff)/(1-tanc*tanoff)
        double tanaoff = Math.tan(ROOT3*ypp + xpp); // a = c + root3*y'' + x''
        double tanboff = Math.tan(2*xpp); // b = c + 2x''
        double sumtmp = tanaoff + tanboff;

        //using a derived cubic equation and cubic formula
        double l = tanboff * tanaoff;
        double m = -(R * l + 2 * tanboff +  2 * tanaoff);
        double n = 3 + R * sumtmp - 2 * l;
        double o = sumtmp - R;

        double l3 = 3*l, m2 = 2*m;

        double x = -o/n; //x = tanc

        for(int i=0; i<newton; i++) {
            double x2 = x*x;

            double f = l*x2*x + m*x2 + n*x + o;
            double fp = l3*x2 + m2*x + n;

            x -= f/fp;
        }

        double tana = (x + tanaoff)/(1-x*tanaoff);
        double tanb = (x + tanboff)/(1-x*tanboff);

        //simple reversal algebra based on tan values
        double yp = ROOT3*( DVE*tana + EL6 )/2;
        double xp = DVE*tanb + yp/ROOT3 + EL6;

        //x = z*xp/Z, y = z*yp/Z, x^2 + y^2 + z^2 = 1
        double xpoZ = xp/Z;
        double ypoZ = yp/Z;

        double z = 1/Math.sqrt(1 + xpoZ*xpoZ + ypoZ*ypoZ);

        return new double[] {z*xpoZ, z*ypoZ, z};
    }

    protected double[] inverseTriangleTransform(double x, double y) {
        return inverseTriangleTransformNewton(x,y);
    }

    static double[] yRot(double lambda, double phi, double rot) {
        double c[] = cart(lambda, phi);

        double x = c[0];
        c[0] = c[2]*Math.sin(rot) + x*Math.cos(rot);
        c[2] = c[2]*Math.cos(rot) - x*Math.sin(rot);

        double mag = Math.sqrt(c[0]*c[0] + c[1]*c[1] + c[2]*c[2]);
        c[0] /= mag; c[1] /= mag; c[2] /= mag;

        return new double[] {
                Math.atan2(c[1],c[0]),
                Math.atan2(Math.sqrt(c[0]*c[0] + c[1]*c[1]), c[2])
        };
    }

    public double[] fromGeo(double lon, double lat) {

        lat = 90 - lat;
        lon *= TO_RADIANS;
        lat *= TO_RADIANS;

        double sinphi = Math.sin(lat);

        double x = Math.cos(lon)*sinphi;
        double y = Math.sin(lon)*sinphi;
        double z = Math.cos(lat);

        int face = findTriangle(x, y, z);

        //apply rotation matrix (move triangle onto template triangle)
        int off = 9*face;
        double xp = x*ROTATION_MATRIX[off + 0] + y*ROTATION_MATRIX[off + 1] + z*ROTATION_MATRIX[off + 2];
        double yp = x*ROTATION_MATRIX[off + 3] + y*ROTATION_MATRIX[off + 4] + z*ROTATION_MATRIX[off + 5];
        double zp = x*ROTATION_MATRIX[off + 6] + y*ROTATION_MATRIX[off + 7] + z*ROTATION_MATRIX[off + 8];

        double[] out = triangleTransform(xp, yp, zp);

        //flip triangle to correct orientation
        if(FLIP_TRIANGLE[face]!=0) {
            out[0] = - out[0];
            out[1] = - out[1];
        }

        x = out[0];
        //deal with special snowflakes (child faces 20, 21)
        if(((face == 15 && x > out[1]*ROOT3) || face == 14) && x > 0) {
                out[0] = 0.5*x - 0.5*ROOT3*out[1];
                out[1] = 0.5*ROOT3*x + 0.5*out[1];
                face += 6; //shift 14->20 & 15->21
        }

        out[0] += CENTER_MAP[face * 2];
        out[1] += CENTER_MAP[face * 2 + 1];

        return out;
    }

    public static double[] OUT_OF_BOUNDS = new double[]{0.0/0, 0.0/0};

    public double[] toGeo(double x, double y) {
        int face = findTriangleGrid(x,y);

        if(face==-1)
            return OUT_OF_BOUNDS;

        x -= CENTER_MAP[face*2];
        y -= CENTER_MAP[face*2 + 1];

        //deal with bounds of special snowflakes
        switch (face) {
            case 14:
                if(x>0) return OUT_OF_BOUNDS;
                break;

            case 20:
                if(-y*ROOT3 > x) return OUT_OF_BOUNDS;
                break;

            case 15:
                if(x>0 && x > y*ROOT3) return OUT_OF_BOUNDS;
                break;

            case 21:
                if(x<0 || -y*ROOT3 > x) return OUT_OF_BOUNDS;
                break;
        }

        //flip triangle to upright orientation (if not already)
        if(FLIP_TRIANGLE[face]!=0) {
            x = -x;
            y = -y;
        }

        //invert triangle transform
        double[] c = inverseTriangleTransform(x, y);
        x = c[0];
        y = c[1];
        double z = c[2];

        //apply inverse rotation matrix (move triangle from template triangle to correct position on globe)
        int off = 9*face;
        double xp = x*INVERSE_ROTATION_MATRIX[off + 0] + y*INVERSE_ROTATION_MATRIX[off + 1] + z*INVERSE_ROTATION_MATRIX[off + 2];
        double yp = x*INVERSE_ROTATION_MATRIX[off + 3] + y*INVERSE_ROTATION_MATRIX[off + 4] + z*INVERSE_ROTATION_MATRIX[off + 5];
        double zp = x*INVERSE_ROTATION_MATRIX[off + 6] + y*INVERSE_ROTATION_MATRIX[off + 7] + z*INVERSE_ROTATION_MATRIX[off + 8];

        //convert back to spherical coordinates
        return new double[] {Math.atan2(yp, xp)/TO_RADIANS, 90-Math.acos(zp)/TO_RADIANS};
    }

    public double[] bounds() {
        return new double[] {-3*ARC, -0.75*ARC*ROOT3, 2.5*ARC, 0.75*ARC*ROOT3};
    }

    public boolean upright() {return false;}

    public double metersPerUnit() {
        return Math.sqrt(510100000000000.0/(20*ROOT3*ARC*ARC/4));
    }

    /*public static void main(String[] args) throws IOException {
        Airocean projection = new ModifiedAirocean();

        double[] c = projection.fromGeo(12.4900422, 41.8902102);//-73.985821, 40.723241);
        System.out.println(c[0] + " " + c[1]);
        c = projection.toGeo(c[0], c[1]);
        System.out.println(c[0] + " " + c[1]);


        System.out.println((new ConformalEstimate()).fromGeo(170.185772, 53.611924)[0]);
        System.out.println((new ConformalEstimate()).fromGeo(170.185772, 53.611924)[1]);

        System.out.println(ARC);

        BufferedImage base;

        InputStream is = new FileInputStream("../../../../../resources/assets/terra121/data/map.png");
        base = ImageIO.read(is);

        BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);

        //scale should be able to fit whole earth inside texture
        double[] bounds = projection.bounds();
        double scale = Math.max(Math.abs(bounds[2] - bounds[0]), Math.abs(bounds[3] - bounds[1]));

        int w = img.getWidth();
        int h = img.getHeight();

        for (int lon = 0; lon < base.getWidth(); lon++) {
            for (int lat = 0; lat < base.getHeight(); lat++) {

                //lat lon to reference image coords
                double Lon = (lon / (double) base.getWidth() - 0.5) * 360;
                double Lat = (lat / (double) base.getHeight() - 0.5) * 180;

                double proj[] = projection.fromGeo(Lon, Lat); //projection coords to x y

                int x = (int) (w * (proj[0] - bounds[0]) / scale);
                int y = (int) (h * (proj[1] - bounds[1]) / scale);

                //get pixel from reference image if possible
                if (x >= 0 && y >= 0 && x < img.getHeight() && y < img.getWidth()) {
                    img.setRGB(x, h - y - 1, base.getRGB(lon, base.getHeight() - lat - 1));
                }
            }
        }

        ImageIO.write(img, "png", new File("out.png"));
    }*/

public static void main(String[] args) throws IOException {
    Airocean projection = new ModifiedAirocean();

        //System.out.println(projection.metersPerUnit());
        //System.out.println((projection.bounds()[0])*projection.metersPerUnit());
        //System.out.println((projection.bounds()[2]-projection.bounds()[0])*projection.metersPerUnit());
        //System.out.println((projection.bounds()[3]-projection.bounds()[1])*projection.metersPerUnit());

        double[] f = (new ConformalEstimate()).fromGeo(-169.245937, 65.865726);
        f = projection.toGeo(f[0], f[1]);

        //System.out.println(f[0] + " " + f[1]);

        BufferedImage base;

        InputStream is = new FileInputStream("../resources/assets/terra121/data/map.png");
        base = ImageIO.read(is);

        BufferedImage img = new BufferedImage(4096,4096,BufferedImage.TYPE_INT_ARGB);

        //scale should be able to fit whole earth inside texture
        double[] bounds = projection.bounds();
        double scale = Math.max(Math.abs(bounds[2]-bounds[0]), Math.abs(bounds[3]-bounds[1]));

        int w = img.getWidth();
        int h = img.getHeight();

        long og = System.nanoTime();

        ConformalEstimate cp = new ConformalEstimate();

        double[] oc = cp.toGeo(0,ARC*ROOT3/12);
        f = cp.fromGeo(oc[0],oc[1]+360.0*0.001/40075017);
        double[] g = cp.fromGeo(oc[0], oc[1]);
        System.out.println(Math.sqrt((f[0]-g[0])*(f[0]-g[0]) + (f[1]-g[1])*(f[1]-g[1]))*cp.metersPerUnit());

        System.out.println(cp.metersPerUnit()/40075017);
        System.out.println(ARC);

        System.out.println((System.nanoTime()-og)/1000000000.0);

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

            if(proj[0]!=proj[0] || proj[1]!=proj[1]){
                lon = 0; lat=0;
            }

        //get pixel from reference image if possible
        if(lon>=0 && lat>=0 && lat < base.getHeight() && lon < base.getWidth()) {
        //if(lon!=1024 || lat!=512)
        //System.out.println(lon+" "+lat);
        img.setRGB(x, h-y-1, base.getRGB(lon, base.getHeight()-lat-1));
        } //else System.out.println(proj[0]+" "+proj[1]+" "+lon+" "+lat);
        }
        }
        }

    System.out.println((System.nanoTime()-og)/1000000000.0);

        ImageIO.write(img, "png", new File("out.png"));
        }
}