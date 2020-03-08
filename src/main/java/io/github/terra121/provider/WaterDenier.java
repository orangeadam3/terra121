package io.github.terra121.provider;

import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import io.github.terra121.EarthBiomeProvider;
import io.github.terra121.TerraConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent.CreateFluidSourceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WaterDenier {
	
	@SubscribeEvent
    public static void sourceCatch(CreateFluidSourceEvent event) {
		if(!TerraConfig.threeWater)
			return;
		
    	World world = event.getWorld();
    	
    	if(world.getBiomeProvider() instanceof EarthBiomeProvider) {
    		IBlockState state = event.getState();
    		Block b = state.getBlock();

    		if(b != Blocks.FLOWING_WATER)
    			return;
    		
    		
    		BlockDynamicLiquid block = (BlockDynamicLiquid)b;
    		BlockPos pos = event.getPos();
    		
    		int c = 0;
    		c += isLiquid(world, pos.north());
    		c += isLiquid(world, pos.south());
    		c += isLiquid(world, pos.east());
    		c += isLiquid(world, pos.west());
    		
    		if(c<3)
    			event.setResult(PopulateCubeEvent.Populate.Result.DENY);
    	}
    }
    
    private static int isLiquid(World world, BlockPos pos) {
    	return (world.getBlockState(pos).getBlock() instanceof BlockLiquid)?1:0;
    }
}
