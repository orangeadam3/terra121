package io.github.terra121.dataset;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

import javax.imageio.ImageIO;

public class WaterGround {
	public RandomAccessRunlength<Byte> data;
	private int width;
	private int height;
	
	public WaterGround(InputStream input) throws IOException {
		data = new RandomAccessRunlength<Byte>();
		DataInputStream in = new DataInputStream(input);

		//save some memory by tying the same bytes to the same object (idk if java does this already) //TODO: static share with Soil.java
		Byte[] bytes = new Byte[256];
		for (int x = 0; x < bytes.length; x++) {
			bytes[x] = (byte) x;
		}

		while(in.available()>0) {
			int v = in.readInt();
			data.addRun(bytes[v>>>30], v&((1<<30)-1));
		}

		in.close();
		input.close();

		height = (int)Math.sqrt(data.size()/2);
		width = height*2;

		//System.out.println(data.size()+" "+height);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
    public byte getOfficial(int x, int y) {
        if(x>=width || x<0 || y>=height || y<0)
            return 0;
        return data.get(x + y*width);
    }
    
    public byte state(int x, int y) {
		return getOfficial(x+(width/2), y+(height/2));
    }
}
