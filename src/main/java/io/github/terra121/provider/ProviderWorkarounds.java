package io.github.terra121.provider;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.DimensionManager;

public class ProviderWorkarounds {
	
	public static void setupProvider() {
		DimensionType type = DimensionType.register("earth", "_earth", 0, EarthWorldProvider.class, true);
        DimensionManager.init();
        DimensionManager.unregisterDimension(0);
        DimensionManager.registerDimension(0, type);
	}
	
	public static void replaceOthers() {
		for(int x=0; x<WorldType.WORLD_TYPES.length; x++) {
			WorldType type = WorldType.WORLD_TYPES[x];
			if(type != null && type.getName().equals("FlatCubic")) {
				WorldType.WORLD_TYPES[x] = null;
			}
		}
		
		new FlatCubicWorkaround();
		//new CustomCubicWorkaround(); //not working atm, won't do anything
	}
}
