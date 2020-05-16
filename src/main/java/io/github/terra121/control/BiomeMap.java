package io.github.terra121.control;

import io.github.terra121.EarthBiomeProvider;

import java.util.HashMap;
import net.minecraft.world.biome.Biome;
import net.minecraft.init.Biomes;

class BiomeMap {
    EarthBiomeProvider biomes;

    public HashMap<Biome, Integer> map;

    public BiomeMap() {
        //I don't like this but BiomeMap was always gonna be slow anyways
        biomes = new EarthBiomeProvider(Biomes.MUSHROOM_ISLAND);
        map = new HashMap<Biome, Integer>();

        //Full credit to Amidst for these colors
        map.put(Biomes.OCEAN, 0xFF000070);
        map.put(Biomes.PLAINS, 0xFF8DB360);
        map.put(Biomes.DESERT, 0xFFFA9418);
        map.put(Biomes.FOREST, 0xFF056621);
        map.put(Biomes.TAIGA, 0xFF0B6659);
        map.put(Biomes.SWAMPLAND, 0xFF07F9B2);
        map.put(Biomes.FROZEN_OCEAN, 0xFF9090A0);
        map.put(Biomes.ICE_PLAINS, 0xFFFFFFFF);
        map.put(Biomes.ICE_MOUNTAINS, 0xFFA0A0A0);
        map.put(Biomes.JUNGLE, 0xFF537B09);
        map.put(Biomes.DEEP_OCEAN, 0xFF000030);
        map.put(Biomes.COLD_TAIGA, 0xFF31554A);
        map.put(Biomes.SAVANNA, 0xFFBDB25F);
        map.put(Biomes.MESA, 0xFFD94515);
    }

    public int getColor(double[] coords) {
        Integer out = map.get(biomes.classify(coords));
        if(out==null)return 0;
        return out;
    }
}