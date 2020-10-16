package io.github.terra121.provider;

import io.github.terra121.EarthWorldType;
import io.github.terra121.populator.SnowPopulator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldProviderSurface;

public class EarthWorldProvider extends WorldProviderSurface {

    protected boolean isEarth;

    @Override
    public void init() {
        super.init();
        this.isEarth = this.world.getWorldInfo().getTerrainType() instanceof EarthWorldType;
    }

    @Override
    public boolean canSnowAt(BlockPos pos, boolean checkLight) {
        if (!this.isEarth) {
            return super.canSnowAt(pos, checkLight);
        }

        return SnowPopulator.canSnow(pos, this.world, false);
    }
}
