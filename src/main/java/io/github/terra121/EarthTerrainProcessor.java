package io.github.terra121;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.CubePopulatorEvent;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import io.github.opencubicchunks.cubicchunks.cubicgen.BasicCubeGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.BiomeBlockReplacerConfig;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.CubicBiome;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacerProvider;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.structure.CubicCaveGenerator;
import io.github.terra121.dataset.Heights;
import io.github.terra121.dataset.OpenStreetMaps;
import io.github.terra121.populator.BuildingGenerator;
import io.github.terra121.populator.CliffReplacer;
import io.github.terra121.populator.EarthTreePopulator;
import io.github.terra121.populator.RoadGenerator;
import io.github.terra121.populator.SnowPopulator;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
//import io.github.opencubicchunks.cubicchunks.api.worldgen.structure.event.InitCubicStructureGeneratorEvent;

public class EarthTerrainProcessor extends BasicCubeGenerator {

    public Heights heights;
    public Heights depths;
    public OpenStreetMaps osm;
    public HashMap<Biome, List<IBiomeBlockReplacer>> biomeBlockReplacers;
    public BiomeProvider biomes;
    public GeographicProjection projection;

    public Set<Block> unnaturals;
    private CustomGeneratorSettings cubiccfg;
    private Set<ICubicPopulator> surfacePopulators;
    private Map<Biome, ICubicPopulator> biomePopulators;
    private BuildingGenerator buildingGenerator;
    private CubicCaveGenerator caveGenerator;
    private SnowPopulator snow;
	public EarthGeneratorSettings cfg;
	private boolean doRoads;
	private OpenStreetMaps.BuildingGenerationType buildingGenerationType;

    public EarthTerrainProcessor(World world) {
        super(world);

        cfg = new EarthGeneratorSettings(world.getWorldInfo().getGeneratorOptions());
    	projection = cfg.getProjection();
    	
    	doRoads = cfg.settings.roads && world.getWorldInfo().isMapFeaturesEnabled();
        buildingGenerationType = world.getWorldInfo().isMapFeaturesEnabled() ? cfg.settings.buildingGenerationType : OpenStreetMaps.BuildingGenerationType.NONE;
        
        biomes = world.getBiomeProvider(); //TODO: make this not order dependent

        osm = new OpenStreetMaps(projection, doRoads, cfg.settings.osmwater, buildingGenerationType);
        heights = new Heights(13, cfg.settings.smoothblend, cfg.settings.osmwater?osm.water:null);
        depths = new Heights(10, cfg.settings.osmwater?osm.water:null); //below sea level only generates a level 10, this shouldn't lag too bad cause a zoom 10 tile is frickin massive (64x zoom 13)
        
        unnaturals = new HashSet<Block>();
        unnaturals.add(Blocks.STONEBRICK);
        unnaturals.add(Blocks.CONCRETE);
        unnaturals.add(Blocks.BRICK_BLOCK);
        
        surfacePopulators = new HashSet<ICubicPopulator>();
        if(doRoads || cfg.settings.osmwater)surfacePopulators.add(new RoadGenerator(osm, heights, projection));
        if(buildingGenerationType != OpenStreetMaps.BuildingGenerationType.NONE) buildingGenerator = new BuildingGenerator(osm, heights, projection, cfg.settings.buildingMaterialSetting);
        surfacePopulators.add(new EarthTreePopulator(projection));
        snow = new SnowPopulator(); //this will go after the rest

        cubiccfg = cfg.getCustomCubic();
        
        //InitCubicStructureGeneratorEvent caveEvent = new InitCubicStructureGeneratorEvent(EventType.CAVE, new CubicCaveGenerator());
        caveGenerator = new CubicCaveGenerator();
        
        biomePopulators = new HashMap<Biome, ICubicPopulator>();
        
        for (Biome biome : ForgeRegistries.BIOMES) {
            CubicBiome cubicBiome = CubicBiome.getCubic(biome);
            biomePopulators.put(biome, cubicBiome.getDecorator(cubiccfg));
        }

        biomeBlockReplacers = new HashMap<Biome, List<IBiomeBlockReplacer>>();
        BiomeBlockReplacerConfig conf = cubiccfg.replacerConfig;
        CliffReplacer cliffs = new CliffReplacer();
        
        for (Biome biome : ForgeRegistries.BIOMES) {
            CubicBiome cubicBiome = CubicBiome.getCubic(biome);
            Iterable<IBiomeBlockReplacerProvider> providers = cubicBiome.getReplacerProviders();
            List<IBiomeBlockReplacer> replacers = new ArrayList<>();
            for (IBiomeBlockReplacerProvider prov : providers) {
                replacers.add(prov.create(world, cubicBiome, conf));
            }
            replacers.add(cliffs);

            biomeBlockReplacers.put(biome, replacers);
        }

    }

    //TODO: more efficient
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        CubePrimer primer = new CubePrimer();

        double heightarr[][] = new double[16][16];
        boolean surface = false;
        
       //null island
    	if(-5 < cubeX && cubeX < 5 && -5 < cubeZ && cubeZ < 5) {
    		for(int x=0; x<16; x++)
                for(int z=0; z<16; z++)
                	heightarr[x][z] = 1;
    	} else {
        
	        //get heights before hand
	        for(int x=0; x<16; x++) {
	            for(int z=0; z<16; z++) {
	            	
	            	double[] projected = projection.toGeo((cubeX*16 + x), (cubeZ*16 + z));
	                double Y = heights.estimateLocal(projected[0], projected[1]);
	                heightarr[x][z] = Y;
	                
	                if(Coords.cubeToMinBlock(cubeY)<Y && Coords.cubeToMinBlock(cubeY)+16>Y) {
	                    surface = true;
	                }
	            }
	        }
    	}

    	//fill in the world
        for(int x=0; x<16; x++) {
            for(int z=0; z<16; z++) {
            	double Y = heightarr[x][z];      	
            	
            	double[] projected = projection.toGeo((cubeX*16 + x), (cubeZ*16 + z));
            	double wateroff = 0;
            	if(cfg.settings.osmwater)wateroff = osm.water.estimateLocal(projected[0], projected[1]);
            	
            	//ocean?
            	if(-0.001 < Y && Y < 0.001) {
                    double depth = depths.estimateLocal(projected[0], projected[1]);
                    
                    if(depth < 0) {
                    	Y = depth;
                    }
            	}
            	
            	/*if(-5 < cubeX && cubeX < 5 && -5 < cubeZ && cubeZ < 5);
            	else if(wateroff>=1.4&&Y>=0) { //drop above sea level areas that are in the ocean
            		Y = -1;
            	}*/

                //estimate slopes
                double dx, dz;
                if(x == 16-1)
                    dx = heightarr[x][z]-heightarr[x-1][z];
                else dx = heightarr[x+1][z]-heightarr[x][z];

                if(z == 16-1)
                    dz = heightarr[x][z]-heightarr[x][z-1];
                else dz = heightarr[x][z+1]-heightarr[x][z];

                //get biome (thanks to 	z3nth10n for spoting this one)
                List<IBiomeBlockReplacer> reps = biomeBlockReplacers.get(biomes.getBiome(new BlockPos(cubeX*16 + x, 0, cubeZ*16 + z)));

                for (int y = 0; y < 16 && y < Y - Coords.cubeToMinBlock(cubeY); y++) {
                    IBlockState block = Blocks.STONE.getDefaultState();
                    for(IBiomeBlockReplacer rep : reps) {
                        block = rep.getReplacedBlock(block, cubeX*16 + x, cubeY*16 + y + 63, cubeZ*16 + z, dx, -1, dz, Y - (cubeY*16 + y));
                    }

                    primer.setBlockState(x, y, z, block);
                }

                int minblock = Coords.cubeToMinBlock(cubeY);

            	if(-5 < cubeX && cubeX < 5 && -5 < cubeZ && cubeZ < 5);//NULL ISLAND
            	else if (cfg.settings.osmwater){
            		if(wateroff>1) {
            		    int start = (int)(Y);
            		    if(start==0) start = -1; //elev 0 should still be treated as ocean when in ocean

            			start -= minblock;
            			if(start<0)start = 0;
            			for (int y = start; y < 16 && y <= -1-minblock; y++) primer.setBlockState(x, y, z, Blocks.WATER.getDefaultState());
            		}
            		else if(wateroff>0.4) {
	            		int start = (int) (Y - (wateroff-0.4)*4) - minblock;
	            		if(start<0)start = 0;
	            		for (int y = start; y < 16 && y < Y - minblock; y++) primer.setBlockState(x, y, z, Blocks.WATER.getDefaultState());
	            	}
            	}
            	else for (int y = (int)Math.max(Y - minblock,0); y < 16 && y < 0 - minblock; y++) primer.setBlockState(x, y, z, Blocks.WATER.getDefaultState());
            }
        }
        
        caveGenerator.generate(world, primer, new CubePos(cubeX, cubeY, cubeZ));

        if (buildingGenerator != null)
        buildingGenerator.generate(world, primer, new CubePos(cubeX, cubeY, cubeZ));

        //spawn roads
        if((doRoads || buildingGenerationType == OpenStreetMaps.BuildingGenerationType.OUTLINES || cfg.settings.osmwater) && surface) {
            Set<OpenStreetMaps.Edge> edges = osm.chunkStructures(cubeX, cubeZ);

            if(edges != null) {

                /*for(int x=0; x<16; x++) {
                    for(int z=0; z<16; z++) {
                        int y = heightarr[x][z] - Coords.cubeToMinBlock(cubeY);
                        if(y >= 0 && y < 16)
                            primer.setBlockState(x, y, z, Blocks.COBBLESTONE.getDefaultState());
                    }
                }*/

                //minor one block wide roads get plastered first
                for (OpenStreetMaps.Edge e: edges) if(e.type == OpenStreetMaps.Type.ROAD || e.type == OpenStreetMaps.Type.MINOR
                                                    || e.type == OpenStreetMaps.Type.STREAM || e.type == OpenStreetMaps.Type.BUILDING) {
                    double start = e.slon;
                    double end = e.elon;

                    if(start > end) {
                        double tmp = start;
                        start = end;
                        end = tmp;
                    }

                    int sx = (int)Math.floor(start) - cubeX*16;
                    int ex = (int)Math.floor(end) - cubeX*16;

                    if(ex >= 16)ex = 16-1;

                    for(int x=sx>0?sx:0; x<=ex; x++) {
                        double realx = (x+cubeX*16);
                        if(realx < start)
                            realx = start;

                        double nextx = realx + 1;
                        if(nextx > end)
                            nextx = end;

                        int from = (int)Math.floor((e.slope*realx + e.offset)) - cubeZ*16;
                        int to = (int)Math.floor((e.slope*nextx + e.offset)) - cubeZ*16;

                        if(from > to) {
                            int tmp = from;
                            from = to;
                            to = tmp;
                        }

                        if(to >= 16)to = 16-1;

                        for(int z=from>0?from:0; z<=to; z++) {
                            int y = (int)Math.floor(heightarr[x][z]) - Coords.cubeToMinBlock(cubeY);

                            if(y >= 0 && y < 16) {
                            	if(e.type == OpenStreetMaps.Type.STREAM) {
                            		if(primer.getBlockState(x, y, z).getBlock()!=Blocks.WATER)
                            			primer.setBlockState(x, y, z, Blocks.WATER.getDefaultState());
                            	}
                            	else primer.setBlockState(x, y, z, ( e.type == OpenStreetMaps.Type.ROAD ? Blocks.GRASS_PATH : e.type == OpenStreetMaps.Type.BUILDING ? Blocks.BRICK_BLOCK : Blocks.STONEBRICK).getDefaultState());
                            }
                        }
                    }
                }
            }
        }

        return primer;
    }


    @Override
    public void populate(ICube cube) {
        /**
         * If event is not canceled we will use cube populators from registry.
         **/
        if (!MinecraftForge.EVENT_BUS.post(new CubePopulatorEvent(world, cube))) {
            Random rand = Coords.coordsSeedRandom(world.getSeed(), cube.getX(), cube.getY(), cube.getZ());
            
            Biome biome = cube.getBiome(Coords.getCubeCenter(cube));

            if(cfg.settings.dynamicbaseheight) {
				double[] proj = projection.toGeo((cube.getX()*16 + 8), (cube.getZ()*16 + 8));
				cubiccfg.expectedBaseHeight = (float) heights.estimateLocal(proj[0], proj[1]);
            }

            MinecraftForge.EVENT_BUS.post(new PopulateCubeEvent.Pre(world, rand, cube.getX(), cube.getY(), cube.getZ(), false));

            CubePos pos = cube.getCoords();

            int surf = isSurface(world, cube);
            if(surf == 0) {
                for(ICubicPopulator pop: surfacePopulators)
                	pop.generate(world, rand, pos, biome);
            }
			
            biomePopulators.get(biome).generate(world, rand, pos, biome);
			
            if(surf==1)
            	snow.generate(world, rand, pos, biome);

            MinecraftForge.EVENT_BUS.post(new PopulateCubeEvent.Post(world, rand, cube.getX(), cube.getY(), cube.getZ(), false));
            CubeGeneratorsRegistry.generateWorld(world, rand, pos, biome);
        }
    }

    //TODO: so inefficient but it's the best i could think of, short of caching this state by coords
    //TODO: factor in if air right above solid cube
    private int isSurface(World world, ICube cube) {
        IBlockState defState = Blocks.AIR.getDefaultState();
        IBlockState type = null;
        for(int x=0; x<16; x++)
            for(int z=0; z<16; z++) {
            	type = world.getBlockState(new BlockPos(x + cube.getX()*16, 16 + cube.getY()*16, z + cube.getZ()*16));
                if(type == defState &&
                		cube.getBlockState(x, 0, z) != defState && !unnaturals.contains(cube.getBlockState(x, 0, z).getBlock()))
                    return 0;
            }
        return type==defState?1:-1;
    }

    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
        // eyes of ender are now compasses
        if(name.equals("Stronghold")) {
            double[] vec = projection.vector(pos.getX(), pos.getZ(), 1, 0); //direction's to one meter north of here

            //normalize vector
            double mag = Math.sqrt(vec[0]*vec[0] + vec[1]*vec[1]);
            vec[0] /= mag; vec[1] /= mag;

            //project vector 100 blocks out to get "stronghold" position
            return new BlockPos((int)(pos.getX() + vec[0]*100.0), pos.getY(), (int)(pos.getZ() + vec[1]*100.0));
        }
        return null;
    }
}
