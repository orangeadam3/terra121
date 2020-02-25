package io.github.terra121;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.terra121.control.EarthGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.init.Biomes;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EarthWorldType extends WorldType implements ICubicWorldType  {
    public EarthWorldType () { super("EarthCubic"); }

    public static EarthWorldType create() { return new EarthWorldType(); }

    public ICubeGenerator createCubeGenerator(World world) {
        return new EarthTerrainProcessor(world);
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        return new EarthBiomeProvider(Biomes.FOREST, world);
    }

    @Override public IntRange calculateGenerationHeightRange(WorldServer world) {
        return new IntRange(-12000, 9000);
    }

    @Override public boolean hasCubicGeneratorForWorld(World w) {
        return w.provider instanceof WorldProviderSurface; // an even more general way to check if it's overworld (need custom providers)
    }

    public boolean isCustomizable() {
        return true;
    }
    
    public float getCloudHeight()
    {
        return 5000;
    }
    
    public double voidFadeMagnitude() {
    	return 0;
    }
    
    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld) {
    	mc.displayGuiScreen(new EarthGui(guiCreateWorld, mc));
    }
}
