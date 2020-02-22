package io.github.terra121.populator;

import java.util.HashSet;
import java.util.Set;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

//shear cliff faces should not be grass or dirt
public class CliffReplacer implements IBiomeBlockReplacer {
	
	public static Set<Block> badSlope = new HashSet<Block>();
	static {
		badSlope.add(Blocks.GRASS);
		badSlope.add(Blocks.DIRT);
	}
	
	
	@Override
	public IBlockState getReplacedBlock(IBlockState prev, int x, int y, int z, double dx, double dy,
			double dz, double density) {
		
		double slopeSquared = dx*dx + dz*dz;
		
		if((slopeSquared > 4||y>6000) && badSlope.contains(prev.getBlock())) {
			return Blocks.STONE.getDefaultState();
		}
		
		return prev;	
	}
	
}
