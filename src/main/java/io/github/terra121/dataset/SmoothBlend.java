package io.github.terra121.dataset;

/**
 * Created by K.jpg on 2/24/2020.
 */
public class SmoothBlend {

    // Lerping produces visible square patches.
    // Fade-curve lerping doesn't work well on steep slopes.
    // Standard splines require 16 control points.
    // This requires only 9 control points to produce a smooth interpolation.
    public static double compute(double x, double y,
            double v00, double v01, double v02,
            double v10, double v11, double v12,
            double v20,double v21, double v22) {

        // Smooth fade curve. Using this directly in a lerp wouldn't work well for steep slopes.
        // But using it here with gradient ramps does a better job.
        double xFade = x * x * (3 - 2 * x);
        double yFade = y * y * (3 - 2 * y);

        // Centerpoints of each square. The interpolator meets these values exactly.
        double vAA = (v00 + v01 + v10 + v11) / 4.0;
        double vAB = (v01 + v02 + v11 + v12) / 4.0;
        double vBA = (v10 + v20 + v11 + v21) / 4.0;
        double vBB = (v11 + v21 + v12 + v22) / 4.0;

        // Slopes at each centerpoint.
        // We "should" divide by 2. But we divide x and y by 2 instead for the same result.
        double vAAx = ((v10 + v11) - (v00 + v01));
        double vAAy = ((v01 + v11) - (v00 + v10));
        double vABx = ((v11 + v12) - (v01 + v02));
        double vABy = ((v02 + v12) - (v01 + v11));
        double vBAx = ((v20 + v21) - (v10 + v11));
        double vBAy = ((v11 + v21) - (v10 + v20));
        double vBBx = ((v21 + v22) - (v11 + v12));
        double vBBy = ((v12 + v22) - (v11 + v21));

        // This is where we correct for the doubled slopes.
        // Note that it means we need x-0.5 instead of x-1.
        x /= 2.0;
        y /= 2.0;
        double ix = x - 0.5;
        double iy = y - 0.5;

        // extrapolate gradients and blend
        double blendXA = (1 - xFade) * (vAA + vAAx * x + vAAy * y) + xFade * (vBA + vBAx * ix + vBAy * y);
        double blendXB = (1 - xFade) * (vAB + vABx * x + vABy * iy) + xFade * (vBB + vBBx * ix + vBBy * iy);
        return (1 - yFade) * blendXA + yFade * blendXB;
    }

}
