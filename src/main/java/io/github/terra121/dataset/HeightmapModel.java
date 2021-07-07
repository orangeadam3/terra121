package io.github.terra121.dataset;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;

import java.util.HashMap;
import java.util.Map;

public class HeightmapModel {
    public boolean surface;
    public double[][] heightmap;

    private HeightmapModel() {}

    public HeightmapModel(boolean surface, double[][] heightmap) {
        this.heightmap = heightmap;
        this.surface = surface;
    }

    private static Map<CubePos, HeightmapModel> cachedHeightmaps = new HashMap<>();

    public static HeightmapModel getModel(int chunkX, int chunkY, int chunkZ) {
        return getModel(new CubePos(chunkX, chunkY, chunkZ));
    }

    public static HeightmapModel getModel(CubePos pos) {
        if(!cachedHeightmaps.containsKey(pos))
            return null;

        return cachedHeightmaps.get(pos);
    }

    public static void add(CubePos pos, HeightmapModel model) {
        cachedHeightmaps.put(pos, model);
    }
}
