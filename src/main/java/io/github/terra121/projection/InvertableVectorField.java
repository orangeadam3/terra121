package io.github.terra121.projection;

public class InvertableVectorField {
    protected double[][] VECTOR_X;
    protected double[][] VECTOR_Y;

    protected static double ROOT3 = Math.sqrt(3);

    public int sideLength;

    public InvertableVectorField(double[][] vx, double[][] vy) {
        sideLength = vx.length-1;
        VECTOR_X = vx;
        VECTOR_Y = vy;
    }


    public double[] getInterpolatedVector(double x, double y) {
        //scale up triangle to be triangleSize across
        x *= sideLength;
        y *= sideLength;

        //convert to triangle units
        double v = 2*y/ROOT3;
        double u = x - v*0.5;

        int u1 = (int)u;
        int v1 = (int)v;

        if(u1<0)u1 = 0;
        else if(u1>=sideLength)u1 = sideLength-1;

        if(v1<0)v1 = 0;
        else if(v1>=sideLength-u1)v1 = sideLength-u1-1;

        double valx1, valy1, valx2, valy2, valx3, valy3;
        double y3, x3;

        double flip = 1;

        if(y < -ROOT3*(x-u1-v1-1) || v1 == sideLength-u1-1) {
            valx1 = VECTOR_X[u1][v1];
            valy1 = VECTOR_Y[u1][v1];
            valx2 = VECTOR_X[u1][v1+1];
            valy2 = VECTOR_Y[u1][v1+1];
            valx3 = VECTOR_X[u1+1][v1];
            valy3 = VECTOR_Y[u1+1][v1];

            y3 = 0.5*ROOT3*v1;
            x3 = (u1+1) + 0.5*v1;
        } else {
            valx1 = VECTOR_X[u1][v1+1];
            valy1 = VECTOR_Y[u1][v1+1];
            valx2 = VECTOR_X[u1+1][v1];
            valy2 = VECTOR_Y[u1+1][v1];
            valx3 = VECTOR_X[u1+1][v1+1];
            valy3 = VECTOR_Y[u1+1][v1+1];

            flip = -1;
            y = -y;

            y3 = -(0.5*ROOT3*(v1+1));
            x3 = (u1+1) + 0.5*(v1+1);
        }

        //TODO: not sure if weights are right (but weirdly mirrors stuff so there may be simplifcation yet)
        double w1 = -(y-y3)/ROOT3-(x-x3);
        double w2 = 2*(y-y3)/ROOT3;
        double w3 = 1-w1-w2;

        return new double[] {valx1*w1 + valx2*w2 + valx3*w3, valy1*w1 + valy2*w2 + valy3*w3,
                (valx3 - valx1)*sideLength, sideLength*flip*(2*valx2 - valx1 - valx3)/ROOT3,
                (valy3 - valy1)*sideLength, sideLength*flip*(2*valy2 - valy1 - valy3)/ROOT3};
    }

    public double[] applyNewtonsMethod(double expectedf, double expectedg, double xest, double yest, int iter) {
        for(int i=0; i<iter; i++) {
            double[] c = getInterpolatedVector(xest, yest);

            double f = c[0] - expectedf,    g = c[1] - expectedg;
            double dfdx = c[2], dfdy = c[3];
            double dgdx = c[4], dgdy = c[5];

            double determinant = 1/(dfdx*dgdy - dfdy*dgdx);

            xest -= determinant*(dgdy*f - dfdy*g);
            yest -= determinant*(-dgdx*f + dfdx*g);
        }

        return new double[] {xest, yest};
    }
}