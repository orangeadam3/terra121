package io.github.terra121;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.CubePopulatorEvent;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.opencubicchunks.cubicchunks.cubicgen.BasicCubeGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.BiomeBlockReplacerConfig;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.CubicBiome;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacerProvider;
import io.github.terra121.dataset.Heights;
import io.github.terra121.dataset.OpenStreetMaps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import net.minecraft.world.biome.BiomeProvider;

public class EarthTerrainProcessor extends BasicCubeGenerator {

    Heights heights;
    OpenStreetMaps osm;
    HashMap<Biome, List<IBiomeBlockReplacer>> biomeBlockReplacers;
    BiomeProvider biomes;
    RoadGenerator roads;

    public Set<IBlockState> unnaturals;
    private Set<ICubicPopulator> populators;

    private static final double SCALE = 100000.0;

    public EarthTerrainProcessor(World world) {
        super(world);
        heights = new Heights();
        osm = new OpenStreetMaps();
        roads = new RoadGenerator(osm, heights);
        biomes = world.getBiomeProvider();
        unnaturals = new HashSet<IBlockState>();
        unnaturals.add(Blocks.STONEBRICK.getDefaultState());
        
        populators = new HashSet<ICubicPopulator>();
        populators.add(new EarthTreePopulator());

        biomeBlockReplacers = new HashMap<Biome, List<IBiomeBlockReplacer>>();
        BiomeBlockReplacerConfig conf = BiomeBlockReplacerConfig.defaults();

        for (Biome biome : ForgeRegistries.BIOMES) {
            CubicBiome cubicBiome = CubicBiome.getCubic(biome);
            Iterable<IBiomeBlockReplacerProvider> providers = cubicBiome.getReplacerProviders();
            List<IBiomeBlockReplacer> replacers = new ArrayList<>();
            for (IBiomeBlockReplacerProvider prov : providers) {
                replacers.add(prov.create(world, cubicBiome, conf));
            }

            biomeBlockReplacers.put(biome, replacers);
        }

    }

    //TODO: more efficent
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        CubePrimer primer = new CubePrimer();

        int heightarr[][] = new int[16][16];
        boolean surface = false;

        for(int x=0; x<16; x++) {
            for(int z=0; z<16; z++) {
                double Y = heights.estimateLocal((cubeX*16 + x)/SCALE, (cubeZ*16 + z)/SCALE);

                if(Coords.cubeToMinBlock(cubeY)<Y && Coords.cubeToMinBlock(cubeY)+16>Y) {
                    surface = true;
                    heightarr[x][z] = (int)Y;
                }

                for (int y = 0; y < 16 && y < Y - Coords.cubeToMinBlock(cubeY); y++) {
                    List<IBiomeBlockReplacer> reps = biomeBlockReplacers.get(biomes.getBiome(new BlockPos(cubeX*16 + x, 0, cubeZ*16 + z)));
                    IBlockState block = Blocks.STONE.getDefaultState();
                    for(IBiomeBlockReplacer rep : reps) {
                        block = rep.getReplacedBlock(block, cubeX*16 + x, cubeY*16 + y + 63, cubeZ*16 + z, 0, -1,0,Y - (cubeY*16 + y));
                    }

                    primer.setBlockState(x, y, z, block);
                }
            }
        }

        //spawn roads
        if(surface) {
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
                for (OpenStreetMaps.Edge e: edges) if(e.type == OpenStreetMaps.Type.ROAD || e.type == OpenStreetMaps.Type.MINOR) {
                    double start = e.slon;
                    double end = e.elon;

                    if(start > end) {
                        double tmp = start;
                        start = end;
                        end = tmp;
                    }

                    int sz = (int)Math.floor(SCALE*start) - cubeZ*16;
                    int ez = (int)Math.floor(SCALE*end) - cubeZ*16;

                    if(ez >= 16)ez = 16-1;

                    for(int z=sz>0?sz:0; z<=ez; z++) {
                        double realz = (z+cubeZ*16)/SCALE;
                        if(realz < start)
                            realz = start;

                        double nextz = realz + (1/SCALE);
                        if(nextz > end)
                            nextz = end;

                        int from = (int)Math.floor(SCALE*(e.slope*realz + e.offset)) - cubeX*16;
                        int to = (int)Math.floor(SCALE*(e.slope*nextz + e.offset)) - cubeX*16;

                        if(from > to) {
                            int tmp = from;
                            from = to;
                            to = tmp;
                        }

                        if(to >= 16)to = 16-1;

                        for(int x=from>0?from:0; x<=to; x++) {
                            int y = heightarr[x][z] - Coords.cubeToMinBlock(cubeY);

                            if(y >= 0 && y < 16)
                                primer.setBlockState(x, y, z, ( e.type == OpenStreetMaps.Type.ROAD ? Blocks.GRASS_PATH : Blocks.STONEBRICK).getDefaultState());
                        }
                    }
                }
            }
        }

        return primer;
    }


    //TODO: idek what populate does
    @Override
    public void populate(ICube cube) {
        /**
         * If event is not canceled we will use cube populators from registry.
         **/
        if (!MinecraftForge.EVENT_BUS.post(new CubePopulatorEvent(world, cube))) {
            Random rand = Coords.coordsSeedRandom(cube.getWorld().getSeed(), cube.getX(), cube.getY(), cube.getZ());

            if(isSurface(world, cube)) {
                roads.generateRoads(cube, cube.getX(), cube.getY(), cube.getZ(), cube.getWorld(), rand);
                
                for(ICubicPopulator pop: populators)
                	pop.generate(cube.getWorld(), rand, cube.getCoords(), cube.getBiome(Coords.getCubeCenter(cube)));
            }
        }
    }

    //TODO: so inefficent but it's the best i could think of, short of cachheing this state by coords
    //TODO: factor in if air right above solid cube
    private boolean isSurface(World world, ICube cube) {
        IBlockState defState = Blocks.AIR.getDefaultState();
        for(int x=0; x<16; x++)
            for(int z=0; z<16; z++) {
                if(world.getBlockState(new BlockPos(x + cube.getX()*16, 16 + cube.getY()*16, z + cube.getZ()*16)) == defState &&
                		cube.getBlockState(x, 0, z) != defState && !unnaturals.contains(cube.getBlockState(x, 0, z)))
                    return true;
            }
        return false;
    }

    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
        // eyes of ender are the new F3 for finding the origin :P
        return name.equals("Stronghold") ? new BlockPos(0, 0, 0) : null;
    }
}
