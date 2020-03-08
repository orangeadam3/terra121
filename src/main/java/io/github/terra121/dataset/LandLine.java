package io.github.terra121.dataset;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LandLine {
	public TreeMap<Double, Long> breaks;
	
	public LandLine() {
		 breaks = new TreeMap<Double, Long>();
	}
	
	public void add(double pos, long type) {
		if(breaks.containsKey(pos))
			pos += 0.00000001;
		
		breaks.put(pos, type);
	}
	
	/*public void convert(double lower, double upper) {
		boolean notstarted = true;
		
		ArrayList<Double> poses;
		ArrayList<Byte> codes;
		
		for(Entry<Double, Byte> e: breaks.entrySet()) {
			double pos = e.getKey();
			if(pos>=lower) {
				if(pos>upper)
					break;
			}
		}
		
		double[] outp;
		byte[] outc;
	}*/
	
	public void run(int size, Set<Long> current, BiConsumer<Set<Long>, Integer> consumer) {
		boolean notstarted = true;

		int idx = 0;
		
		for(Entry<Double, Long> e: breaks.entrySet()) {
			double pos = e.getKey();
			
			if(pos>=0) {
				while(pos > idx) {
					if(idx<size)
						consumer.accept(current, idx);
					idx++;
				}
				
				Long flag = e.getValue();
				if(current.contains(flag))
					current.remove(flag);
				else current.add(flag);
			}
		}
		
		while(idx<size) {
			consumer.accept(current, idx++);
		}
	}
	
	public Object[] compileBreaks(Set<Long> current, int max) {
		
		short[] index = new short[breaks.size()+1];
		byte[] value = new byte[breaks.size()+1];
		
		index[0] = 0;
		value[0] = (byte) (current.size()==0?0:current.contains(-1L)?2:1);
		
		int idx = 1;
		
		for(Entry<Double, Long> e: breaks.entrySet()) {
			double pos = e.getKey();
			if(pos>=0 && pos < max) {
				
				Long flag = e.getValue();
				if(current.contains(flag))
					current.remove(flag);
				else current.add(flag);
				
				index[idx] = (short) pos;
				value[idx] = (byte) (current.size()==0?0:current.contains(-1L)?2:1);
				
				idx++;
			}
		}
		
		//trim extra capacity
		if(index.length>idx) {
			short[] oindex = index;
			index = new short[idx];
			System.arraycopy(oindex, 0, index, 0, idx);
			
			byte[] oval = value;
			value = new byte[idx];
			System.arraycopy(oval, 0, value, 0, idx);
		}
		
		return new Object[] {index, value};
	}
}
