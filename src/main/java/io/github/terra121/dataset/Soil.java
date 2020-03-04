package io.github.terra121.dataset;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Soil {
    RandomAccessRunlength<Byte> data;
    public static final int COLS = 10800;
    public static final int ROWS = 5400;

    public Soil(InputStream input) throws IOException {
        //save some memory by tying the same bytes to the same object (idk if java does this already)
        Byte[] bytes = new Byte[256];
        for (int x = 0; x < bytes.length; x++) {
            bytes[x] = (byte) x;
        }

        //save in a random access run lenth to save ram at the slight cost of efficiency
        //this works because one soil type tends to stretch more than 4km
        data = new RandomAccessRunlength<Byte>();

        BufferedInputStream is = new BufferedInputStream(input);

        int i;
        while ((i = is.read()) >= 0) {
            data.add(bytes[i]);
        }


        if(data.size() != COLS*ROWS) {
            throw new IOException("Soil data invalid, " + data.size());
        }
    }

    public byte getOfficial(int x, int y) {
        if(x>=COLS || x<0 || y>=ROWS || y<0)
            return 0;
        return data.get(x + y*COLS);
    }

    public byte getPoint(double x, double y) {
        int X = (int)(COLS*(x+180)/360);
        int Y = (int)(ROWS*(90-y)/180);

        return getOfficial(X, Y);
    }
}