package io.github.terra121;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import io.github.terra121.dataset.Climate;
import io.github.terra121.dataset.Soil;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

public class EarthBiomeProvider extends BiomeProvider {

    /*public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        int x = pos.getX();
        int z = pos.getZ();
        return Biomes.DESERT;
    }*/

    public Soil soil;
    public Climate climate;
    public GeographicProjection projection;

    /** The biome generator object. */
    private final Biome defaultBiome;

    public EarthBiomeProvider(Biome biomeIn, World world)
    {
        this(biomeIn);

        EarthGeneratorSettings cfg = new EarthGeneratorSettings(world.getWorldInfo().getGeneratorOptions());
    	projection = cfg.getProjection();
    }

    public EarthBiomeProvider(Biome biomeIn) {
        //load soil and climate data from assets
        this.defaultBiome = biomeIn;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("assets/terra121/data/suborder.img");
            soil = new Soil(is);
            is.close();

            is = getClass().getClassLoader().getResourceAsStream("assets/terra121/data/climate.dat");
            climate = new Climate(is);
            is.close();
        } catch(IOException ioe) {
            TerraMod.LOGGER.error("Failed to load biome data: " + ioe);
        }
    }

    /**
     * Returns the biome generator based on soil and climate (mostly soil)
     */
    public Biome getBiome(BlockPos pos)
    {
    	//null island
    	if(-80 < pos.getX() && pos.getX() < 80 && -80 < pos.getZ() && pos.getZ() < 80) {
    		if(-16 < pos.getX() && pos.getX() < 16 && -16 < pos.getZ() && pos.getZ() < 16)
    			return Biomes.FOREST;
    		return Biomes.MUSHROOM_ISLAND;
    	}

        return classify(projection.toGeo(pos.getX(), pos.getZ()));
    }

    /** Get explicit data on the environment (soil, tempature, precipitation) */
    public double[] getEnv(double lon, double lat) {
        Climate.ClimateData clim = climate.getPoint(lon, lat);

        return new double[] {soil.getPoint(lon, lat),
                            clim.temp, clim.precip};
    }

    public Biome classify(double[] projected) {

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
                return Biomes.ICE_MOUNTAINS;

            case 5: case 6: case 7: //Permafrost
                return Biomes.ICE_PLAINS;
            case 10:
                return Biomes.JUNGLE;
            case 11: case 12:
                return Biomes.PLAINS;

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
            case 41: case 42: case 43: case 44: case 45:
                return Biomes.PLAINS;

            case 50:
                return Biomes.COLD_TAIGA;
            case 51: //salt flats always desert
                return Biomes.DESERT;
            case 52: case 53: case 55: case 99: //hot and dry
                if(clim.temp<2)
                    return Biomes.COLD_TAIGA;
                if(clim.temp<5)
                    return Biomes.TAIGA; //TODO: Tundra in (1.15)
                if(clim.precip<5)
                    return Biomes.DESERT;
                return Biomes.MESA; //TODO: this soil can also be desert i.e. saudi Arabia (base on percip?)

            case 54: case 56:
                return Biomes.SAVANNA;

            case 60: case 61: case 62: case 63: case 64:
                if (clim.temp < 10)
                    return Biomes.TAIGA;
                return Biomes.FOREST;

            case 70: case 72: case 73: case 74: case 75: case 76: case 77:
                return Biomes.PLAINS;

            case 13: case 40: case 71: case 80: case 95: case 98:
                return Biomes.SWAMPLAND;

            case 81: case 83: case 84: case 86:
                return Biomes.FOREST;
            case 82: case 85:
                return Biomes.PLAINS;

            case 90: case 91: case 92: case 93: case 94:
                return Biomes.FOREST;
            case 96:
                return Biomes.SAVANNA;
            case 97:
                return Biomes.DESERT;
        }

        return Biomes.MUSHROOM_ISLAND;
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