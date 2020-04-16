package io.github.terra121.populator;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.terra121.TerraMod;
import io.github.terra121.dataset.Heights;
import io.github.terra121.dataset.OpenStreetMaps;
import io.github.terra121.dataset.Pathway;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import io.github.terra121.dataset.Pathway.VectorPath;
import java.util.*;

public class VectorPathGenerator implements ICubicPopulator {

    OpenStreetMaps osm;
    Heights heights;
    GeographicProjection projection;

    public VectorPathGenerator(Heights heights, GeographicProjection proj, OpenStreetMaps osm) {
        this.heights = heights;
        projection = proj;
        this.osm = osm;
    }

    @Override
    public void generate(World world, Random random, CubePos cubePos, Biome biome) {

        int cubeX = cubePos.getX();
        int cubeY = cubePos.getY();
        int cubeZ = cubePos.getZ();

        TerraMod.LOGGER.info("generate");
        Set<OpenStreetMaps.Edge> edges = osm.chunkStructures(cubeX, cubeY);

        if (edges != null) {

            TerraMod.LOGGER.info("size: {}", edges.size());
            List<Pathway.VectorPathGroup> paths = Pathway.chunkStructuresAsVectors(edges, world, cubeX, cubeY, cubeZ, heights, projection, false);
            List<Pathway.VectorPathGroup> sPaths = new ArrayList<>();
            List<Pathway.VectorPathGroup> secondProcessPaths;
            List<OpenStreetMaps.Edge> edgeCache = new ArrayList<>();

            if (!paths.isEmpty()) {

                // iterate over VectorPathGroups
                for (Pathway.VectorPathGroup vpg : paths) {

                    List<VectorPath> currentVp = vpg.paths;

                    for (int e = 0; e <= currentVp.size() - 1; e++) {

                        VectorPath current = currentVp.get(e);

                        if (current.edge != null) {

                            edgeCache.add(current.edge);

                        } else if (!current.path.isEmpty()) {

                            if (!edgeCache.isEmpty()) {

                                Set<OpenStreetMaps.Edge> tunnels = new HashSet<>(edgeCache);
                                secondProcessPaths = Pathway.chunkStructuresAsVectors(tunnels, world, cubeX, cubeY, cubeZ, heights, projection, true);
                                sPaths.addAll(secondProcessPaths);

                            }
                        }
                    }
                }

                sPaths.addAll(paths);

                for (Pathway.VectorPathGroup g : sPaths) {
                    placeVectorPaths(g.paths, world);
                }

            }
        }
    }

    public void placeVectorPaths(List<VectorPath> paths, World world) {
        for (VectorPath p : paths) {
            for (Vec3d path : p.path) {
                BlockPos l = new BlockPos(path.x, path.y, path.z);
                if (world.getBlockState(l).getBlock().getDefaultState() != p.material) {
                    world.setBlockState(l, p.material);
                }
            }
        }
    }

    public static double bound(double x, double slope, double j, double k, double r, double x0, double b, double sign) {
        double slopeSign = sign * (slope < 0 ? -1 : 1);

        if (x < j - slopeSign * x0) { //left circle
            return slope * j + sign * Math.sqrt(r * r - (x - j) * (x - j));
        }
        if (x > k - slopeSign * x0) { //right circle
            return slope * k + sign * Math.sqrt(r * r - (x - k) * (x - k));
        }
        return slope * x + sign * b;
    }

}
