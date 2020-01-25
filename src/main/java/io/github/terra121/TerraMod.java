package io.github.terra121;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.net.URL;
import java.net.URLClassLoader;

import org.apache.logging.log4j.Logger;

//import io.github.Kms;

@Mod(modid = TerraMod.MODID, name = TerraMod.NAME, version = TerraMod.VERSION, dependencies = "required-after:cubicchunks; required-after:cubicgen")
public class TerraMod
{
    public static final String MODID = "terra121";
    public static final String NAME = "Terra 1 to 1";
    public static final String VERSION = "0.1";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        EarthWorldType.create();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    }
}
