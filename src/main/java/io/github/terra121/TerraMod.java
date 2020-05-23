package io.github.terra121;

import org.apache.logging.log4j.Logger;

import io.github.terra121.control.TerraTeleport;
import io.github.terra121.control.TerraCommand;
import io.github.terra121.provider.EarthWorldProvider;
import io.github.terra121.provider.GenerationEventDenier;
import io.github.terra121.provider.WaterDenier;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import io.github.terra121.letsencryptcraft.ILetsEncryptMod;
import io.github.terra121.letsencryptcraft.LetsEncryptAdder;

@Mod(modid = TerraMod.MODID, name = TerraMod.NAME, version = TerraMod.VERSION, dependencies = "required-after:cubicchunks; required-after:cubicgen", acceptableRemoteVersions="*")
public class TerraMod implements ILetsEncryptMod
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
    	MinecraftForge.EVENT_BUS.register(WaterDenier.class);
        MinecraftForge.EVENT_BUS.register(TerraConfig.class);
	PermissionAPI.registerNode("terra121.commands.tpll", DefaultPermissionLevel.OP, "Allows a player to do /tpll");
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if(!Loader.isModLoaded("letsencryptcraft"))
            LetsEncryptAdder.doStuff(this);
    }
    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new TerraTeleport());
        event.registerServerCommand(new TerraCommand());
    }
    
    //set custom provider
    private static void setupProvider() {
		DimensionType type = DimensionType.register("earth", "_earth", 0, EarthWorldProvider.class, true);
        DimensionManager.init();
        DimensionManager.unregisterDimension(0);
        DimensionManager.registerDimension(0, type);
	}

	//stuff to implement ILetsEncryptMod
    public void info(String log) {
        LOGGER.info(log);
    }

    public void error(String log) {
        LOGGER.error(log);
    }

    public void error(String log, Throwable t) {
        LOGGER.error(log, t);
    }
}
