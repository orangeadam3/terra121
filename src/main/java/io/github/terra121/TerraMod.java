package io.github.terra121;

import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.logging.log4j.Logger;

import io.github.terra121.dataset.Trees;
import net.minecraft.init.Biomes;
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
        //new Kms();
		new Trees();
		System.out.println(Biomes.BEACH);
        
        Field[] declaredFields = Biomes.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                System.out.println(field);
            }
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    }
}
