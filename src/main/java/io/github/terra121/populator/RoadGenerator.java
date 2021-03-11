package io.github.terra121.populator;

import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.terra121.TerraMod;
import io.github.terra121.dataset.Heights;
import io.github.terra121.dataset.OpenStreetMaps;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRail;

public class RoadGenerator implements ICubicPopulator {
	
    private static final IBlockState ASPHALT = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.GRAY);
    private static final IBlockState WATER_SOURCE = Blocks.WATER.getDefaultState();
    //private static final IBlockState WATER_RAMP = Blocks.WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, );
    private static final IBlockState WATER_BEACH = Blocks.DIRT.getDefaultState();
	private static final IBlockState TRACK = Blocks.RAIL.getDefaultState();

    private OpenStreetMaps osm;
    private Heights heights;
    private GeographicProjection projection;

    // only use for roads with markings
    public double calculateRoadWidth(int w, int l) {
        return Math.ceil(((1+w)*l+l)/2);
    }

    public RoadGenerator(OpenStreetMaps osm, Heights heights, GeographicProjection proj) {
        this.osm = osm;
        this.heights = heights;
        projection = proj;
    }

	private boolean isRailBend(IBlockState state) {
		
		if(state.getBlock()!=TRACK.getBlock())
			return false;
			
		BlockRailBase.EnumRailDirection dir = state.getValue(BlockRail.SHAPE);
		
		return dir==BlockRailBase.EnumRailDirection.SOUTH_EAST || dir==BlockRailBase.EnumRailDirection.SOUTH_WEST ||
		       dir==BlockRailBase.EnumRailDirection.NORTH_EAST || dir==BlockRailBase.EnumRailDirection.NORTH_WEST ;
	}

    public void generate(World world, Random rand, CubePos pos, Biome biome) {
    	
    	int cubeX = pos.getX(), cubeY = pos.getY(), cubeZ = pos.getZ();
    	
        Set<OpenStreetMaps.Edge> edges = osm.chunkStructures(cubeX, cubeZ);
		
        if(edges!=null) { 
        	
        	// rivers done before roads
        	for(OpenStreetMaps.Edge e: edges) {
	            if(e.type == OpenStreetMaps.Type.RIVER) {
	            	placeEdge(e, world, cubeX, cubeY, cubeZ, 5, (dis, bpos) -> riverState(world, dis, bpos));
	            }
	        }

        	// (1+w)l+l is the equation to calculate road width, where "w" is the width and "l" is the amount of lanes

            // i only use this for roads that need road markings, because if there are no road markings, the extra place is not needed,
            // and it can simply be w*l

            // TODO add generation of road markings

            // TODO simplify road width

	        for(OpenStreetMaps.Edge e: edges) {
	            // this will obviously be deleted once the levels actually do something
                // System.out.println("Generating road on level: " + e.layer_number);
                if (e.attribute != OpenStreetMaps.Attributes.ISTUNNEL) {
                    switch (e.type) {
                        case MINOR:
                            placeEdge(e, world, cubeX, cubeY, cubeZ, Math.ceil((2 * e.lanes) / 2), (dis, bpos) -> ASPHALT);
                            break;
                        case SIDE:
                            placeEdge(e, world, cubeX, cubeY, cubeZ, Math.ceil((3 * e.lanes + 1) / 2), (dis, bpos) -> ASPHALT);
                            break;
                        case MAIN:
                            placeEdge(e, world, cubeX, cubeY, cubeZ, calculateRoadWidth(2, e.lanes), (dis, bpos) -> ASPHALT);
                            break;
                        case FREEWAY:
                        case LIMITEDACCESS:
                            placeEdge(e, world, cubeX, cubeY, cubeZ, calculateRoadWidth(4, e.lanes) + 2, (dis, bpos) -> ASPHALT);
                            break;
                        case INTERCHANGE:
                            placeEdge(e, world, cubeX, cubeY, cubeZ, Math.ceil((3 * e.lanes) / 2), (dis, bpos) -> ASPHALT);
                            break;
						case RAIL:
							osm.placeThin(cubeX, cubeZ, e, (x, z) -> {
								double[] geo = projection.toGeo(x + cubeX*(16), z + cubeZ*(16));
								int y = 1+(int)Math.floor(heights.estimateLocal(geo[0], geo[1]) - cubeY*16);
								if(!(y > 0 && y <= 16))
									return;
								
								BlockPos bpos = new BlockPos(x + cubeX * 16, y + cubeY * 16, z + cubeZ * 16);
								
								world.setBlockState(bpos, TRACK);
							});
							
							osm.placeThin(cubeX, cubeZ, e, (x, z) -> {
								double[] geo = projection.toGeo(x + cubeX*(16), z + cubeZ*(16));
								int y = 1+(int)Math.floor(heights.estimateLocal(geo[0], geo[1]) - cubeY*16);
								if(!(y > 0 && y <= 16))
									return;
								
								BlockPos bpos = new BlockPos(x + cubeX * 16, y + cubeY * 16, z + cubeZ * 16);
								
								if(world.getBlockState(bpos).getBlock()==TRACK.getBlock()) {
									int neighbors = 0;
									boolean north = world.getBlockState(bpos.north().down()).getBlock()==TRACK.getBlock();
									boolean south = world.getBlockState(bpos.south().down()).getBlock()==TRACK.getBlock();
									boolean east = world.getBlockState(bpos.east().down()).getBlock()==TRACK.getBlock();
									boolean west = world.getBlockState(bpos.west().down()).getBlock()==TRACK.getBlock();
									neighbors += north?1:0;
									neighbors += south?1:0;
									neighbors += east?1:0;
									neighbors += west?1:0;
									
									System.out.println("pass A " + neighbors + " " +bpos);
									
									if(neighbors==2) {
										
										if(north)world.setBlockState(bpos.north().down(), Blocks.AIR.getDefaultState());
										if(south)world.setBlockState(bpos.south().down(), Blocks.AIR.getDefaultState());
										if(east)world.setBlockState(bpos.east().down(), Blocks.AIR.getDefaultState());
										if(west)world.setBlockState(bpos.west().down(), Blocks.AIR.getDefaultState());
										
										world.setBlockState(bpos, Blocks.AIR.getDefaultState());
										world.setBlockState(bpos.down().down(), Blocks.BRICK_BLOCK.getDefaultState());
										world.setBlockState(bpos.down(), TRACK);
										
										if(north)world.setBlockState(bpos.north().down(), TRACK);
										if(south)world.setBlockState(bpos.south().down(), TRACK);
										if(east)world.setBlockState(bpos.east().down(), TRACK);
										if(west)world.setBlockState(bpos.west().down(), TRACK);
									}
										
								}
							});
							
							osm.placeThin(cubeX, cubeZ, e, (x, z) -> {
								double[] geo = projection.toGeo(x + cubeX*(16), z + cubeZ*(16));
								int y = 1+(int)Math.floor(heights.estimateLocal(geo[0], geo[1]) - cubeY*16);
								if(!(y > 0 && y <= 16))
									return;
								
								BlockPos bpos = new BlockPos(x + cubeX * 16, y + cubeY * 16, z + cubeZ * 16);
								
								if(world.getBlockState(bpos).getBlock()==TRACK.getBlock()) {
									int neighbors = 0;
									neighbors += world.getBlockState(bpos.north().up()).getBlock()==TRACK.getBlock()?1:0;
									neighbors += world.getBlockState(bpos.south().up()).getBlock()==TRACK.getBlock()?1:0;
									neighbors += world.getBlockState(bpos.east().up()).getBlock()==TRACK.getBlock()?1:0;
									neighbors += world.getBlockState(bpos.west().up()).getBlock()==TRACK.getBlock()?1:0;
									
                  System.out.println("pass B " + neighbors + " " +bpos);
									
									if(neighbors==2) {
										
										world.setBlockState(bpos, Blocks.BRICK_BLOCK.getDefaultState());
										world.setBlockState(bpos.up(), TRACK);
									}
								}
							});
              
              osm.placeThin(cubeX, cubeZ, e, (x, z) -> {
								double[] geo = projection.toGeo(x + cubeX*(16), z + cubeZ*(16));
								int y = 1+(int)Math.floor(heights.estimateLocal(geo[0], geo[1]) - cubeY*16);
								if(!(y > 0 && y <= 16))
									return;
								
								BlockPos bpos = new BlockPos(x + cubeX * 16, y + cubeY * 16, z + cubeZ * 16);
								
								IBlockState mystate = world.getBlockState(bpos);
								
								if(mystate.getBlock()==TRACK.getBlock() && isRailBend(mystate)) {
									int bentneighbors = 0;
									bentneighbors += isRailBend(world.getBlockState(bpos.north().down()))?1:0;
									bentneighbors += isRailBend(world.getBlockState(bpos.south().down()))?1:0;
									bentneighbors += isRailBend(world.getBlockState(bpos.east().down()))?1:0;
									bentneighbors += isRailBend(world.getBlockState(bpos.west().down()))?1:0;
									
                  System.out.println("pass C " + bentneighbors + " " +bpos);
                  
									if(bentneighbors==1) {
										BlockPos newtrack = null;
										BlockRailBase.EnumRailDirection mydir = mystate.getValue(BlockRail.SHAPE);
										
										switch(mydir) {
											case SOUTH_EAST:
												newtrack = bpos.south().east();
												break;
												
											case SOUTH_WEST:
												newtrack = bpos.south().west();
												break;
											
											case NORTH_EAST:
												newtrack = bpos.north().east();
												break;
												
											case NORTH_WEST:
												newtrack = bpos.north().west();
												break;
												
											default:
												return;
										}
										System.out.println(bpos+"fix em up" + bpos);
										
										world.setBlockState(bpos, Blocks.AIR.getDefaultState());
										world.setBlockState(newtrack.down(), Blocks.BRICK_BLOCK.getDefaultState());
										world.setBlockState(newtrack, TRACK);
									}
								}
							});
							
							
							break;
							
                        default:
                            // might be a tunnel or a bridge, mainly for debugging purposes
                            break;
                    }
                }
	        }
        }
    }

    private IBlockState riverState(World world, double dis, BlockPos pos) {
        IBlockState prev = world.getBlockState(pos);
        if(dis>2) {
            if(!prev.getBlock().equals(Blocks.AIR))
                return null;
            IBlockState under = world.getBlockState(pos.down());
            if(under.getBlock() instanceof BlockLiquid)
                return null;
            return WATER_BEACH;
        }
        else return WATER_SOURCE;
    }
    
    private void placeEdge(OpenStreetMaps.Edge e, World world, int cubeX, int cubeY, int cubeZ, double r, BiFunction<Double, BlockPos, IBlockState> state) {
        double x0 = 0;
        double b = r;
        if(Math.abs(e.slope)>=0.000001) {
            x0 = r/Math.sqrt(1 + 1 / (e.slope * e.slope));
            b = (e.slope < 0 ? -1 : 1) * x0 * (e.slope + 1.0 / e.slope);
        }

        double j = e.slon - (cubeX*16);
        double k = e.elon - (cubeX*16);
        double off = e.offset - (cubeZ*16) + e.slope*(cubeX*16);
        
        if(j>k) {
            double t = j;
            j = k;
            k = t;
        }

        double ij = j-r;
        double ik = k+r;
        
        if(j<=0) {
        	j=0;
        	//ij=0;
        }
        if(k>=16) {
        	k=16;
        	//ik = 16;
        }

        int is = (int)Math.floor(ij);
        int ie = (int)Math.floor(ik);

        for(int x=is; x<=ie; x++) {
            double X = x;
            double ul = bound(X, e.slope, j, k, r, x0, b, 1) + off; //TODO: save these repeated values
            double ur = bound(X+1, e.slope, j, k, r, x0, b, 1) + off;
            double ll = bound(X, e.slope, j, k, r, x0, b, -1) + off;
            double lr = bound(X+1, e.slope, j, k, r, x0, b,-1) + off;

            double from = Math.min(Math.min(ul,ur),Math.min(ll,lr));
            double to = Math.max(Math.max(ul,ur),Math.max(ll,lr));
            
            if(from==from) {
                int ifrom = (int)Math.floor(from);
                int ito = (int)Math.floor(to);

                if(ifrom <= -1*16)
                    ifrom = 1 - 16;
                if(ito >= 16*2)
                    ito = 16*2-1;

                for(int z=ifrom; z<=ito; z++) {
                    //get the part of the center line i am tangent to (i hate high school algebra!!!)
                    double Z = z;
                    double mainX = X;
                    if(Math.abs(e.slope)>=0.000001)
                        mainX = (Z + X/e.slope - off)/(e.slope + 1/e.slope);

                    /*if(mainX<j) mainX = j;
                    else if(mainX>k) mainX = k;*/

                    double mainZ = e.slope*mainX + off;
                    
                    //get distance to closest point
                    double distance = mainX-X;
                	distance *= distance;
                	double t = mainZ-Z;
                	distance += t*t;
                	distance = Math.sqrt(distance);

                    double[] geo = projection.toGeo(mainX + cubeX*(16), mainZ + cubeZ*(16));
                    int y = (int)Math.floor(heights.estimateLocal(geo[0], geo[1]) - cubeY*16);

                    if (y >= 0 && y < 16) { //if not in this range, someone else will handle it
                    	
                    	BlockPos surf = new BlockPos(x + cubeX * 16, y + cubeY * 16, z + cubeZ * 16);
                    	IBlockState bstate = state.apply(distance, surf);
                    	
                    	if(bstate!=null) {
		                	world.setBlockState(surf, bstate);
		
		                    //clear the above blocks (to a point, we don't want to be here all day)
		                    IBlockState defState = Blocks.AIR.getDefaultState();
		                    for (int ay = y + 1; ay < 16 * 2 && world.getBlockState(new BlockPos(x + cubeX * 16, ay + cubeY * 16, z + cubeZ * 16)) != defState; ay++) {
		                        world.setBlockState(new BlockPos(x + cubeX * 16, ay + cubeY * 16, z + cubeZ * 16), defState);
		                    }
                        }
                    }
                }
            }
        }
    }

    private static double bound(double x, double slope, double j, double k, double r, double x0, double b, double sign) {
        double slopesign = sign*(slope<0?-1:1);

        if(x < j - slopesign*x0) { //left circle
            return slope*j + sign*Math.sqrt(r*r-(x-j)*(x-j));
        }
        if(x > k - slopesign*x0) { //right circle
            return slope*k + sign*Math.sqrt(r*r-(x-k)*(x-k));
        }
        return slope*x + sign*b;
    }
}