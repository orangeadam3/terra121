package io.github.terra121.populator;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.terra121.EarthBiomeProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class SnowPopulator implements ICubicPopulator {
	
	public static boolean canSnow(BlockPos pos, World world, boolean air) {
		IBlockState blockstate = world.getBlockState(pos); 
		
		if (air || (blockstate.getBlock().isAir(blockstate, world, pos) && Blocks.SNOW_LAYER.canPlaceBlockAt(world, pos)))
        {
			//this cast could fail but this function should only be called in earth anyways
			EarthBiomeProvider ebp = (EarthBiomeProvider) world.getBiomeProvider();
			double[] proj = ebp.projection.toGeo(pos.getX(), pos.getZ());
			return ebp.climate.isSnow(proj[0], proj[1], pos.getY());
        }
		return false;
	}
	
	
	@Override
	public void generate(World world, Random random, CubePos pos, Biome biome) {
		int baseX = pos.getX()*16, baseY = pos.getY()*16, baseZ = pos.getZ()*16;
		
		/*EarthBiomeProvider ebp = (EarthBiomeProvider) world.getBiomeProvider();
		double[] proj = ebp.projection.toGeo(pos.getX()/100000.0, pos.getY()/100000.0);
		*/
		
		if(canSnow(new BlockPos(baseX+8, baseY+8, baseZ+8), world, true)) {
			IBlockState snow = Blocks.SNOW_LAYER.getDefaultState();
			
			for(int x=0; x<16; x++)
				for(int z=0; z<16; z++) {
					int y = quickElev(world, baseX, baseZ, baseY, baseY+16-1);
					BlockPos bpos = new BlockPos(baseX + x, y, baseZ + z);
					
					if(canSnow(bpos, world, false))
						 world.setBlockState(bpos, snow);
				}
		}
	}
	
    private int quickElev(World world, int x, int z, int low, int high) {
    	high++;

        IBlockState defState = Blocks.AIR.getDefaultState();

        while(low < high-1) {
            int y = low + (high - low) / 2;
            if(world.getBlockState(new BlockPos(x, y, z))==defState)
                high = y;
            else low = y;
        }

        return low;
    }
}
