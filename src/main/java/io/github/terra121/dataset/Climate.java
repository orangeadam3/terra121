package io.github.terra121.dataset;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Climate {
    public static final int COLS = 720;
    public static final int ROWS = 360;

    private ClimateData[] data;

    public Climate(InputStream input) throws IOException {
        try {
            DataInputStream out = new DataInputStream(new BufferedInputStream(input));

            data = new ClimateData[COLS * ROWS];

            for (int x = 0; x < data.length; x++) {
                out.readFloat();
                out.readFloat();
                data[x] = new ClimateData(out.readFloat(), out.readFloat());
            }
        } catch (IOException ioe) {
            throw new IOException("Failed to load climate data: "+ioe);
        }
    }

    public ClimateData getOfficial(int x, int y) {
        if(x>=COLS || x<0 || y>=ROWS || y<0)
            return new ClimateData(-50, 0);
        return data[x*ROWS + y];
    }

    public ClimateData getPoint(double x, double y) {
        x = (COLS*(x+180)/360);
        y = (ROWS*(90-y)/180);
        int X = (int)Math.floor(x);
        int Y = (int)Math.floor(y);

        double u = x-X;
        double v = y-Y;

        ClimateData ll = getOfficial(X,Y);
        ClimateData lr = getOfficial(X+1,Y);
        ClimateData ur = getOfficial(X+1,Y+1);
        ClimateData ul = getOfficial(X,Y+1);

        return new ClimateData((1-v)*(ll.temp*(1-u) + lr.temp*u) + (ul.temp*(1-u) + ur.temp*u)*v,
                (1-v)*(ll.precip*(1-u) + lr.precip*u) + (ul.precip*(1-u) + ur.precip*u)*v);
    }

    //rough estimate of snow cover
    public boolean isSnow(double x, double y, double alt) {
		return alt>5000 || getPoint(x,y).temp<0; //high elevations or freezing temperatures
	}
    
    public static class ClimateData {
        public double temp;
        public double precip;

        public ClimateData (double temp, double precip) {
            this.temp = temp;
            this.precip = precip;
        }
        
        public String toString() {
        	return temp + " " + precip;
        }
    }
}