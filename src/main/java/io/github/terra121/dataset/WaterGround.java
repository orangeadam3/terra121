package io.github.terra121.dataset;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class WaterGround {
	public RandomAccessRunlength<Byte> data;
	private int width;
	private int height;
	
	public WaterGround(InputStream input) throws IOException {
		data = new RandomAccessRunlength<Byte>();
		
		BufferedImage img = ImageIO.read(input);
		
		width = img.getWidth();
		height = img.getHeight();
		int len = width*height;
		
		//save some memory by tying the same bytes to the same object (idk if java does this already) //TODO: static share with Soil.java
        Byte[] bytes = new Byte[256];
        for (int x = 0; x < bytes.length; x++) {
            bytes[x] = (byte) x;
        }
		
		for(int i=0; i<len; i++) {
			int col = img.getRGB(i%width, height - i/width - 1);
			int res = col==0xff0000a0?0:col==0xff00ffff?1:2;
			data.add(bytes[res]);
		}
	}
	
    public byte getOfficial(int x, int y) {
        if(x>=width || x<0 || y>=height || y<0)
            return 0;
        return data.get(x + y*width);
    }
    
    public byte state(int x, int y) {
		return getOfficial(x-(width/2), y-(height/2));
    }
}
