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
    public static final IBlockState ROOF = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.SILVER);
    public static final IBlockState WALLS = Blocks.BRICK_BLOCK.getDefaultState();

    private OpenStreetMaps osm;
    private Heights heights;
    private GeographicProjection projection;
    private Building.BuildingMaterial buildingMaterialSetting;

    public BuildingGenerator(OpenStreetMaps osm, Heights heights, GeographicProjection projection, Building.BuildingMaterial buildingMaterialSetting) {
        this.osm = osm;
        this.heights = heights;
        this.projection = projection;
        this.buildingMaterialSetting = buildingMaterialSetting;
    }

    @Override
    public void generate(World world, CubePrimer cubePrimer, CubePos chunkPosition) {
        Set<Building> buildings = osm.chunkBuildings(chunkPosition.getX(), chunkPosition.getZ());
        if (buildings != null)
        for (Building building : buildings)
            building.placeIntoChunk(world, cubePrimer, chunkPosition, heights, projection, buildingMaterialSetting);
    }


}