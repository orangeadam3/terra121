package io.github.terra121.dataset;

import io.github.opencubicchunks.cubicchunks.cubicgen.CustomCubicMod;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;

public class Heights extends TiledDataset{
    private static final int ZOOM = 13;
    private static final String URL_PREFIX = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/"+ZOOM+"/";

    public Heights() {
    	super(256, 256, 10);
    }

    private static final double TO_RADIANS = Math.PI/180.0;
    private static final double MAP_MULT = (1<<(ZOOM+8))/(2*Math.PI);

    //latitude and longitude to EPSG:3857 tileing format
    protected double lonToX(double lon) {
        return MAP_MULT* (lon*TO_RADIANS + Math.PI);
    }

    protected double latToY(double lat) {
        return MAP_MULT * (Math.PI - Math.log(Math.tan((Math.PI/2 + lat*TO_RADIANS)/2)));
    }

    //request a mapzen tile from amazon, this should only be needed evrey 2 thousand blocks or so if the cache is large enough
    //TODO: better error handle
    protected int[] request(Coord place) {
        int out[] = new int[256 * 256];

        for(int i=0; i<5; i++) {

            InputStream is = null;

            try {
                String urlText = URL_PREFIX + place.x + "/" + place.y + ".png";
                //CustomCubicMod.LOGGER.error(urlText);
                URL url = new URL(urlText);
                is = url.openStream();
                BufferedImage img = ImageIO.read(is);
                is.close();
                is = null;

                if(img == null) {
                    throw new IOException("Invalid image file");
                }

                //compile height data from image, stored in 256ths of a meter units
                img.getRGB(0, 0, 256, 256, out, 0, 256);

                for (int x = 0; x < img.getWidth(); x++) {
                    for (int y = 0; y < img.getHeight(); y++) {
                        int c = y * 256 + x;
                        out[c] = (out[c] & 0x00ffffff) - 8388608;
                    }
                }

                return out;

            } catch (IOException ioe) {
                if(is!=null) {
                    try {
                        is.close();
                    } catch (IOException e) {}
                }

                CustomCubicMod.LOGGER.error("Failed to get elevation " + place.x + " " + place.y + " : " + ioe);
            }
        }

        CustomCubicMod.LOGGER.error("Failed too many times chunks will be set to 0");
        return out;
    }

	protected double dataToDouble(int data) {
		return data/256.0;
	}

    public static void main(String[] args) throws IOException, ImageReadException {
    	Heights h = new Heights();
    	double X = h.lonToX(-112.11720)/256;
    	double Y = h.latToY(36.06600)/256;
    	double[] arr = h.estimateWithSlope(36.070646, -112.110249);
    	System.out.println(arr[0] + " " + arr[1] + " " + arr[2]);
    }
}
