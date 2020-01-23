package io.github.terra121;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.opencubicchunks.cubicchunks.cubicgen.CWGEventFactory;
import io.github.terra121.dataset.Trees;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;

public class EarthTreePopulator implements ICubicPopulator {

	Trees trees;
	
	public Set<Block> extraSurface;
	
	public EarthTreePopulator() {
		trees = new Trees();
		extraSurface = new HashSet<Block>();
		extraSurface.add(Blocks.CLAY);
		extraSurface.add(Blocks.RED_SANDSTONE);
		extraSurface.add(Blocks.SANDSTONE);
		extraSurface.add(Blocks.STAINED_HARDENED_CLAY);
		extraSurface.add(Blocks.HARDENED_CLAY);
		extraSurface.add(Blocks.SAND);
		extraSurface.add(Blocks.SNOW);
	}
	
	@Override
	public void generate(World world, Random random, CubePos pos, Biome biome) {
	    int treeCount = (int)(trees.estimateLocal(pos.getX()*16/100000.0, pos.getZ()*16/100000.0)*15.0);

	    ICubicWorld cworld = (ICubicWorld)world;
	    		
	    if (CWGEventFactory.decorate(world, random, pos, DecorateBiomeEvent.Decorate.EventType.TREE)) {
	        for (int i = 0; i < treeCount; ++i) {
	            int xOffset1 = random.nextInt(ICube.SIZE) + ICube.SIZE / 2;
	            int zOffset1 = random.nextInt(ICube.SIZE) + ICube.SIZE / 2;
	            WorldGenAbstractTree treeGen = biome.getRandomTreeFeature(random);
	            treeGen.setDecorationDefaults();
	            BlockPos top1 = cworld.getSurfaceForCube(pos, xOffset1, zOffset1, 0, ICubicWorld.SurfaceType.OPAQUE);
	            if(top1!= null) {
	            	IBlockState topstate = world.getBlockState(top1.down());
	            	
	            	//plant a bit of dirt to make sure trees spawn when they are supposed to even in certain hostile environments
	            	if(extraSurface.contains(topstate.getBlock()) && !topstate.getBlock().canSustainPlant(topstate, world, top1.down(), net.minecraft.util.EnumFacing.UP, (net.minecraft.block.BlockSapling)Blocks.SAPLING))
	            		world.setBlockState(top1.down(), Blocks.GRASS.getDefaultState());
		            
	            	
	            	if (treeGen.generate(world, random, top1)) {
		                treeGen.generateSaplings(world, random, top1);
		            }
	            }
	        }
	    }
	}

}
