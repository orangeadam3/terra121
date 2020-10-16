package io.github.terra121.control;

import io.github.terra121.EarthBiomeProvider;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;

class BiomeMap {
    final EarthBiomeProvider biomes;

    public final HashMap<Biome, Integer> map;

    public BiomeMap() {
        //I don't like this but BiomeMap was always gonna be slow anyways
        this.biomes = new EarthBiomeProvider(Biomes.MUSHROOM_ISLAND);
        this.map = new HashMap<>();

        //Full credit to Amidst for these colors
        this.map.put(Biomes.OCEAN, 0xFF000070);
        this.map.put(Biomes.PLAINS, 0xFF8DB360);
        this.map.put(Biomes.DESERT, 0xFFFA9418);
        this.map.put(Biomes.FOREST, 0xFF056621);
        this.map.put(Biomes.TAIGA, 0xFF0B6659);
        this.map.put(Biomes.SWAMPLAND, 0xFF07F9B2);
        this.map.put(Biomes.FROZEN_OCEAN, 0xFF9090A0);
        this.map.put(Biomes.ICE_PLAINS, 0xFFFFFFFF);
        this.map.put(Biomes.ICE_MOUNTAINS, 0xFFA0A0A0);
        this.map.put(Biomes.JUNGLE, 0xFF537B09);
        this.map.put(Biomes.DEEP_OCEAN, 0xFF000030);
        this.map.put(Biomes.COLD_TAIGA, 0xFF31554A);
        this.map.put(Biomes.SAVANNA, 0xFFBDB25F);
        this.map.put(Biomes.MESA, 0xFFD94515);
    }

    public int getColor(double[] coords) {
        Integer out = this.map.get(this.biomes.classify(coords));
        if (out == null) {
            return 0;
        }
        return out;
    }
}