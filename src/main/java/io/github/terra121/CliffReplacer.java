package io.github.terra121;

import java.util.HashSet;
import java.util.Set;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

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
		
		if(slopeSquared > 4 && badSlope.contains(prev.getBlock())) {
			return Blocks.STONE.getDefaultState();
		}
		
		return prev;	
	}
	
}
