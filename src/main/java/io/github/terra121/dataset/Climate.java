package io.github.terra121.dataset;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Climate {
    public static final int COLS = 720;
    public static final int ROWS = 360;

    private final ClimateData[] data;

    public Climate(InputStream input) throws IOException {
        try {
            DataInputStream out = new DataInputStream(new BufferedInputStream(input));

            this.data = new ClimateData[COLS * ROWS];

            for (int x = 0; x < this.data.length; x++) {
                out.readFloat();
                out.readFloat();
                this.data[x] = new ClimateData(out.readFloat(), out.readFloat());
            }
        } catch (IOException ioe) {
            throw new IOException("Failed to load climate data: " + ioe);
        }
    }

    public ClimateData getOfficial(int x, int y) {
        if (x >= COLS || x < 0 || y >= ROWS || y < 0) {
            return new ClimateData(-50, 0);
        }
        return this.data[x * ROWS + y];
    }

    public ClimateData getPoint(double x, double y) {
        x = (COLS * (x + 180) / 360);
        y = (ROWS * (90 - y) / 180);
        int X = (int) Math.floor(x);
        int Y = (int) Math.floor(y);

        double u = x - X;
        double v = y - Y;

        ClimateData ll = this.getOfficial(X, Y);
        ClimateData lr = this.getOfficial(X + 1, Y);
        ClimateData ur = this.getOfficial(X + 1, Y + 1);
        ClimateData ul = this.getOfficial(X, Y + 1);

        return new ClimateData((1 - v) * (ll.temp * (1 - u) + lr.temp * u) + (ul.temp * (1 - u) + ur.temp * u) * v,
                (1 - v) * (ll.precip * (1 - u) + lr.precip * u) + (ul.precip * (1 - u) + ur.precip * u) * v);
    }

    //rough estimate of snow cover
    public boolean isSnow(double x, double y, double alt) {
        return alt > 5000 || this.getPoint(x, y).temp < 0; //high elevations or freezing temperatures
    }

    public static class ClimateData {
        public final double temp;
        public final double precip;

        public ClimateData(double temp, double precip) {
            this.temp = temp;
            this.precip = precip;
        }

        public String toString() {
            return this.temp + " " + this.precip;
        }
    }
}