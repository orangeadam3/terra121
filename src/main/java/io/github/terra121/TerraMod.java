package io.github.terra121;

import org.apache.logging.log4j.Logger;

import io.github.terra121.control.TerraTeleport;
import io.github.terra121.provider.EarthWorldProvider;
import io.github.terra121.provider.GenerationEventDenier;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = TerraMod.MODID, name = TerraMod.NAME, version = TerraMod.VERSION, dependencies = "required-after:cubicchunks; required-after:cubicgen", acceptableRemoteVersions="*")
public class TerraMod
{
    public static final String MODID = "terra121";
    public static final String NAME = "Terra 1 to 1";
    public static final String VERSION = "0.1";
    public static final String USERAGENT = TerraMod.MODID+"/"+TerraMod.VERSION;
    
    public static final boolean CUSTOM_PROVIDER = false; //could potentially interfere with other mods and is relatively untested, leaving off for now

    public static Logger LOGGER;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        LOGGER = event.getModLog();
        EarthWorldType.create();
        
        if(CUSTOM_PROVIDER) {
	        setupProvider();
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	MinecraftForge.TERRAIN_GEN_BUS.register(GenerationEventDenier.class);
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {    	
    	
    }
    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new TerraTeleport());
    }
    
    //set custom provider
    private static void setupProvider() {
		DimensionType type = DimensionType.register("earth", "_earth", 0, EarthWorldProvider.class, true);
        DimensionManager.init();
        DimensionManager.unregisterDimension(0);
        DimensionManager.registerDimension(0, type);
	}
}
