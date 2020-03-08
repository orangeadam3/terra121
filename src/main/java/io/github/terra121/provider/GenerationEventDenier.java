package io.github.terra121.provider;

import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.DecorateCubeBiomeEvent;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import io.github.terra121.EarthBiomeProvider;
import io.github.terra121.EarthWorldType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

//deny default tree and snow event because we have some of those already
public class GenerationEventDenier {
    @SubscribeEvent
    public static void populateCatcher(PopulateCubeEvent.Populate event) {
    	if(event.getType()==PopulateChunkEvent.Populate.EventType.ICE && event.getGenerator() instanceof EarthWorldType) {
    		event.setResult(PopulateCubeEvent.Populate.Result.DENY);
    	}
    }
    
    @SubscribeEvent
    public static void decorateCatcher(DecorateCubeBiomeEvent.Decorate event) {
    	if(event.getType()==DecorateBiomeEvent.Decorate.EventType.TREE && event.getWorld().getBiomeProvider() instanceof EarthBiomeProvider) {
    		event.setResult(PopulateCubeEvent.Populate.Result.DENY);
    	}
    }
}
