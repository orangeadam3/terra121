package io.github.terra121.populator;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.structure.ICubicStructureGenerator;
import io.github.terra121.dataset.Building;
import io.github.terra121.dataset.Heights;
import io.github.terra121.dataset.OpenStreetMaps;
import io.github.terra121.dataset.Polygon;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Random;
import java.util.Set;

public class BuildingGenerator implements ICubicStructureGenerator {
    public static final IBlockState FOUNDATION = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.SILVER);
    public static final IBlockState ROOF = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.BLACK);
    public static final IBlockState WALLS = Blocks.BRICK_BLOCK.getDefaultState();

    private OpenStreetMaps osm;
    private Heights heights;
    private GeographicProjection projection;

    public BuildingGenerator(OpenStreetMaps osm, Heights heights, GeographicProjection projection) {
        this.osm = osm;
        this.heights = heights;
        this.projection = projection;
    }

    @Override
    public void generate(World world, CubePrimer cubePrimer, CubePos chunkPosition) {
        Set<Building> buildings = osm.chunkBuildings(chunkPosition.getX(), chunkPosition.getZ());
        if (buildings != null)
        for (Building building : buildings) {
            if (!building.hasCalculatedHeights)
                building.calculateHeights(heights, projection);
            int minX = building.minX();
            int minZ = building.minZ();
            int maxX = building.maxX();
            int maxZ = building.maxZ();
            int minY;
            int maxY;
            if (building.minHeight != 0) {
                minY = (int)building.heightOfLowestCorner + building.minHeight;
                maxY = minY + (building.height - building.minHeight);
            } else {
                minY = (int) building.heightOfLowestCorner;
                maxY = (int) Math.max(building.heightOfHighestCorner, minY + building.height);
            }
            if (maxY < chunkPosition.getMinBlockY()) continue;
            if (minY > chunkPosition.getMaxBlockY()) continue;

            // Foundation, roof, and clear area of building
            for (int x = Math.max(minX, chunkPosition.getMinBlockX()); x <= Math.min(maxX, chunkPosition.getMaxBlockX()); x++) {
                for (int z = Math.max(minZ, chunkPosition.getMinBlockZ()); z <= Math.min(maxZ, chunkPosition.getMaxBlockZ()); z++) {
                    if (building.contains(x, z)) {
                        // Foundation
                        if (minY >= chunkPosition.getMinBlockY())
                            cubePrimer.setBlockState(x - chunkPosition.getMinBlockX(), minY - chunkPosition.getMinBlockY(), z - chunkPosition.getMinBlockZ(), FOUNDATION);
                        // Air
                        for (int y = Math.max(minY + 1, chunkPosition.getMinBlockY()); y <= Math.min(maxY+3, chunkPosition.getMaxBlockY()); y++)
                            if (cubePrimer.getBlockState(x - chunkPosition.getMinBlockX(), y - chunkPosition.getMinBlockY(), z - chunkPosition.getMinBlockZ()) != WALLS)
                                cubePrimer.setBlockState(x - chunkPosition.getMinBlockX(), y - chunkPosition.getMinBlockY(), z - chunkPosition.getMinBlockZ(), Blocks.AIR.getDefaultState());
                            // Roof
                        if (maxY-1 <= chunkPosition.getMaxBlockY())
                            cubePrimer.setBlockState(x - chunkPosition.getMinBlockX(), maxY-1 - chunkPosition.getMinBlockY(), z - chunkPosition.getMinBlockZ(), ROOF);
                    }
                }
            }

            // Walls (done afterward so it overwrites the edge of the roof)
            for (int y = Math.max(minY, chunkPosition.getMinBlockY()); y <= Math.min(maxY, chunkPosition.getMaxBlockY()); y++) {
                for (Polygon p : building.outerPolygons) {
                    OpenStreetMaps.Geometry last = p.vertices[0];
                    for (int i = 1; i < p.vertices.length; i++) {
                        OpenStreetMaps.Geometry current = p.vertices[i];
                        placeLine(cubePrimer, (int)last.lon, y, (int)last.lat, (int)current.lon, y, (int)current.lat, WALLS, chunkPosition);
                        last = current;
                    }
                }
                if (building.innerPolygons != null)
                for (Polygon p : building.innerPolygons) {
                    OpenStreetMaps.Geometry last = p.vertices[0];
                    for (int i = 1; i < p.vertices.length; i++) {
                        OpenStreetMaps.Geometry current = p.vertices[i];
                        placeLine(cubePrimer, (int)last.lon, y, (int)last.lat, (int)current.lon, y, (int)current.lat, WALLS, chunkPosition);
                        last = current;
                    }
                }
            }
        }
    }

    private void placeLine(CubePrimer cubePrimer, int x0, int y0, int z0, int x1, int y1, int z1, IBlockState block, CubePos cube) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);
        int stepX = x0 < x1 ? 1 : -1;
        int stepY = y0 < y1 ? 1 : -1;
        int stepZ = z0 < z1 ? 1 : -1;
        double hypotenuse = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2));
        double tMaxX = hypotenuse*0.5 / dx;
        double tMaxY = hypotenuse*0.5 / dy;
        double tMaxZ = hypotenuse*0.5 / dz;
        double tDeltaX = hypotenuse / dx;
        double tDeltaY = hypotenuse / dy;
        double tDeltaZ = hypotenuse / dz;
        while (x0 != x1 || y0 != y1 || z0 != z1){
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x0 = x0 + stepX;
                    tMaxX = tMaxX + tDeltaX;
                }
                else if (tMaxX > tMaxZ){
                    z0 = z0 + stepZ;
                    tMaxZ = tMaxZ + tDeltaZ;
                }
                else{
                    x0 = x0 + stepX;
                    tMaxX = tMaxX + tDeltaX;
                    z0 = z0 + stepZ;
                    tMaxZ = tMaxZ + tDeltaZ;
                }
            }
            else if (tMaxX > tMaxY){
                if (tMaxY < tMaxZ) {
                    y0 = y0 + stepY;
                    tMaxY = tMaxY + tDeltaY;
                }
                else if (tMaxY > tMaxZ){
                    z0 = z0 + stepZ;
                    tMaxZ = tMaxZ + tDeltaZ;
                }
                else{
                    y0 = y0 + stepY;
                    tMaxY = tMaxY + tDeltaY;
                    z0 = z0 + stepZ;
                    tMaxZ = tMaxZ + tDeltaZ;

                }
            }
            else{
                if (tMaxY < tMaxZ) {
                    y0 = y0 + stepY;
                    tMaxY = tMaxY + tDeltaY;
                    x0 = x0 + stepX;
                    tMaxX = tMaxX + tDeltaX;
                }
                else if (tMaxY > tMaxZ){
                    z0 = z0 + stepZ;
                    tMaxZ = tMaxZ + tDeltaZ;
                }
                else{
                    x0 = x0 + stepX;
                    tMaxX = tMaxX + tDeltaX;
                    y0 = y0 + stepY;
                    tMaxY = tMaxY + tDeltaY;
                    z0 = z0 + stepZ;
                    tMaxZ = tMaxZ + tDeltaZ;

                }
            }
            if (
                    x0 >= cube.getMinBlockX() &&
                    x0 <= cube.getMaxBlockX() &&
                    y0 >= cube.getMinBlockY() &&
                    y0 <= cube.getMaxBlockY() &&
                    z0 >= cube.getMinBlockZ() &&
                    z0 <= cube.getMaxBlockZ()
            )
                cubePrimer.setBlockState(x0 - cube.getMinBlockX(), y0 - cube.getMinBlockY(), z0 - cube.getMinBlockZ(), block);
        }
    }
}