package io.github.terra121.provider;

import io.github.opencubicchunks.cubicchunks.cubicgen.flat.FlatCubicWorldType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;

public class FlatCubicWorkaround extends FlatCubicWorldType {
	public FlatCubicWorkaround() {
		super(); //we just need to override a single function
	}
	
	// an even more general way to check if it's overworld (need custom surface providers)
	@Override public boolean hasCubicGeneratorForWorld(World w) {
    	System.out.println(w.provider.getClass().getName());
        return w.provider instanceof WorldProviderSurface;
    }
	
}
