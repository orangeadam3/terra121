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
        return w.provider.getClass() == WorldProviderSurface.class; // a more general way to check if it's overworld
    }

    //TODO: Custom Settings
    public boolean isCustomizable() {
        return false;
    }
}
