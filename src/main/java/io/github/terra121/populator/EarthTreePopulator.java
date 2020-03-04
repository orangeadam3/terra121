package io.github.terra121.populator;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.terra121.dataset.Trees;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

public class EarthTreePopulator implements ICubicPopulator {

	Trees trees;
	
	public Set<Block> extraSurface;
	private GeographicProjection projection;
	
	public EarthTreePopulator(GeographicProjection proj) {
		trees = new Trees();
		extraSurface = new HashSet<Block>();
		extraSurface.add(Blocks.CLAY);
		extraSurface.add(Blocks.RED_SANDSTONE);
		extraSurface.add(Blocks.SANDSTONE);
		extraSurface.add(Blocks.STAINED_HARDENED_CLAY);
		extraSurface.add(Blocks.HARDENED_CLAY);
		extraSurface.add(Blocks.SAND);
		extraSurface.add(Blocks.SNOW);
		extraSurface.add(Blocks.MYCELIUM);
		projection = proj;
	}
	
	private double atanh(double x) {
		return (Math.log(1+x) - Math.log(1-x))/2;
	}
	
	@Override
	public void generate(World world, Random random, CubePos pos, Biome biome) {

		double[] projected = projection.toGeo(pos.getX()*16, pos.getZ()*16);
		
	    double canopy = trees.estimateLocal(projected[0], projected[1]);
	    
	    //got this fun formula messing around with data on desmos, estimate of tree cover -> number
	    int treeCount = 30; //max so it doesn't go to infinity (which would technically be required to guarantee full coverage, but no)
	    if(canopy < 0.95)
	    	treeCount = (int) (20*atanh(Math.pow(canopy,1.5)));
	    
	    //null island
	    if(pos.getX()==0 && pos.getZ()==0)
	    	treeCount = 10;

	    if(treeCount!=0 && random.nextFloat() < biome.decorator.extraTreeChance)
	    	treeCount++;
	    
	    //we are special, and this event is being canceled to control the default populators
	    //CWGEventFactory.decorate(world, random, pos, DecorateBiomeEvent.Decorate.EventType.TREE);
	    
	        for (int i = 0; i < treeCount; ++i) {
	            int xOffset = random.nextInt(ICube.SIZE);
	            int zOffset = random.nextInt(ICube.SIZE);
	            WorldGenAbstractTree treeGen = biome.getRandomTreeFeature(random);
	            treeGen.setDecorationDefaults();
	            
	            int actualX = xOffset + pos.getMinBlockX();
	            int actualZ = zOffset + pos.getMinBlockZ();
	            BlockPos top1 = new BlockPos(actualX, quickElev(world, actualX, actualZ, pos.getMinBlockY()-1, pos.getMaxBlockY())+1, actualZ);
	            
	            if(top1!= null && pos.getMinBlockY() <= top1.getY() && top1.getY() <= pos.getMaxBlockY() && world.getBlockState(top1).getBlock() == Blocks.AIR) {
	            	IBlockState topstate = world.getBlockState(top1.down());
	            	boolean spawn = true;
	            	
	            	if(topstate.getBlock()!=Blocks.GRASS && topstate.getBlock()!=Blocks.DIRT) {
		            	//plant a bit of dirt to make sure trees spawn when they are supposed to even in certain hostile environments
		            	if(extraSurface.contains(topstate.getBlock()))
		            		world.setBlockState(top1.down(), Blocks.GRASS.getDefaultState());
		            	else spawn = false;
	            	}
	            	
	            	if (spawn && treeGen.generate(world, random, top1)) {
		                treeGen.generateSaplings(world, random, top1);
		            }
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
