package io.github.terra121;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import net.minecraft.init.Biomes;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.BiomeProviderSingle;

public class EarthWorldType extends WorldType implements ICubicWorldType  {
    public EarthWorldType () { super("EarthCubic"); }

    public static EarthWorldType create() { return new EarthWorldType(); }

    public ICubeGenerator createCubeGenerator(World world) {
    	System.out.println(world.provider.isNether());
        return new EarthTerrainProcessor(world);
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        return new EarthBiomeProvider(Biomes.FOREST);
    }

    @Override public IntRange calculateGenerationHeightRange(WorldServer world) {
        return new IntRange(0, 256); // TODO: Flat generation height range
    }

    @Override public boolean hasCubicGeneratorForWorld(World w) {
    	System.out.println(w.provider.getClass().getName());
        return w.provider instanceof WorldProviderSurface; // an even more general way to check if it's overworld (need custom providers)
    }

    //TODO: Custom Settings
    public boolean isCustomizable() {
        return false;
    }
    
    public float getCloudHeight()
    {
        return 5000;
    }
    
    public double voidFadeMagnitude() {
    	return 0;
    }
}
