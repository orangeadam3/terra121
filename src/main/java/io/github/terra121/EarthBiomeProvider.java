package io.github.terra121;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;

import io.github.terra121.dataset.Climate;
import io.github.terra121.dataset.Soil;
import io.github.terra121.projection.GeographicProjection;
import io.github.terra121.projection.InvertedGeographic;
import io.github.terra121.projection.MinecraftGeographic;
import net.minecraft.util.math.BlockPos;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.Biome;

import java.io.IOException;
import java.io.InputStream;

public class EarthBiomeProvider extends BiomeProvider {

    /*public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        int x = pos.getX();
        int z = pos.getZ();
        System.out.println(Biomes.DESERT);
        return Biomes.DESERT;
    }*/

    private Soil soil;
    private Climate climate;
    private GeographicProjection projection;

    /** The biome generator object. */
    private final Biome defaultBiome;

    public EarthBiomeProvider(Biome biomeIn)
    {
    	projection = new InvertedGeographic();
        this.defaultBiome = biomeIn;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("assets/terra121/data/suborder.img");
            soil = new Soil(is);
            is.close();
        } catch(IOException ioe) {
            System.err.println("Failed to load soil " + ioe);
        }

        try {
        	InputStream is = getClass().getClassLoader().getResourceAsStream("assets/terra121/data/climate.dat");
            climate = new Climate(is);
            is.close();
        } catch(IOException ioe) {
            System.err.println("Failed to load climate " + ioe);
        }
    }

    /**
     * Returns the biome generator
     */
    public Biome getBiome(BlockPos pos)
    {
    	if(-1600 < pos.getX() && pos.getX() < 1600 && -1600 < pos.getZ() && pos.getZ() < 1600) {
    		if(-16 < pos.getX() && pos.getX() < 16 && -16 < pos.getZ() && pos.getZ() < 16)
    			return Biomes.FOREST;
    		return Biomes.MUSHROOM_ISLAND;
    	}
    	
    	double[] projected = projection.toGeo(pos.getX() / 100000.0, pos.getZ() / 100000.0);
    	
        Climate.ClimateData clim = climate.getPoint(projected[0], projected[1]);
        byte stype = soil.getPoint(projected[0], projected[1]);
        switch(stype) {
            case 0: //Ocean
                if(clim.temp < -5)
                    return Biomes.FROZEN_OCEAN;
                return Biomes.DEEP_OCEAN;
            case 1: //Shifting Sand
                return Biomes.DESERT;
            case 2: //Rock
                return Biomes.DESERT; //cant find it (rock mountians)
            case 3: //Ice
                return Biomes.FROZEN_OCEAN;

            case 5: case 6: case 7: //Permafrost
                return Biomes.ICE_PLAINS;
            case 10:
                return Biomes.JUNGLE;
            case 11: case 12:
                return Biomes.PLAINS;
            case 13:
                return Biomes.SWAMPLAND;

            case 15:
                if(clim.temp<5)
                    return Biomes.COLD_TAIGA;
                else if(clim.temp>15)
                    return Biomes.SWAMPLAND;
                return Biomes.FOREST;

            case 16: case 17: case 18: case 19:
                if(clim.temp<15) {
                    if (clim.temp < 0)
                        return Biomes.COLD_TAIGA;
                    return Biomes.SWAMPLAND;
                }
                if(clim.temp > 20)
                    return Biomes.SWAMPLAND;
                return Biomes.FOREST;

            case 29: case 30: case 31: case 32: case 33:
                return Biomes.SAVANNA;
            case 34:
                return Biomes.JUNGLE;

            case 95:
                return Biomes.SWAMPLAND;
            case 96:
                return Biomes.SAVANNA;
            case 97:
                return Biomes.DESERT;
            case 98:
                return Biomes.SWAMPLAND;
            case 99: //hot and dry, grand canyon country
                return Biomes.MESA; //TODO: this soil can also be desert i.e. saudi Arabia (base on percip?)
        }

        return defaultBiome;
    }

    /**
     * Returns an array of biomes for the location input.
     */
    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height)
    {
        if (biomes == null || biomes.length < width * height)
        {
            biomes = new Biome[width * height];
        }

        Arrays.fill(biomes, 0, width * height, this.defaultBiome);
        return biomes;
    }

    /**
     * Gets biomes to use for the blocks and loads the other data like temperature and humidity onto the
     * WorldChunkManager.
     */
    public Biome[] getBiomes(@Nullable Biome[] oldBiomeList, int x, int z, int width, int depth)
    {
        if (oldBiomeList == null || oldBiomeList.length < width * depth)
        {
            oldBiomeList = new Biome[width * depth];
        }

        for(int r=0; r<width; r++) {
            for(int c=0; c<depth; c++) {
                oldBiomeList[r*depth + c] = getBiome(new BlockPos(x+r,0,z+c));
            }
        }
        return oldBiomeList;
    }

    /**
     * Gets a list of biomes for the specified blocks.
     */
    public Biome[] getBiomes(@Nullable Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag)
    {
        return this.getBiomes(listToReuse, x, z, width, length);
    }

    @Nullable
    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random)
    {
        return null;
    }

    /**
     * checks given Chunk's Biomes against List of allowed ones
     */
    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed)
    {
        return true;
    }

    public boolean func_190944_c()
    {
        return true;
    }

    public Biome func_190943_d()
    {
        return this.defaultBiome;
    }
}