package io.github.terra121.dataset;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Building {
    public final IBlockState foundation = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.SILVER);
    public final IBlockState roof = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.SILVER);

    // Only affects walls
    public static enum BuildingMaterial {
        BRICK, // Standard bricks
        STONE_BRICK, // Standard stone bricks
        CONCRETE, // Plain silver concrete
        OSM_BRICK, // Whatever OSM material but default to brick
        OSM_STONE_BRICK, // Whatever OSM material but default to stone brick
        OSM_CONCRETE, // Whatever OSM material but default to silver concrete
        RANDOM_CONCRETE, // Random concrete colors (on a per building basis)
        RANDOM_PLUS, // Random concrete colors plus wood, stone, etc
        OSM_RANDOM_CONCRETE,  // Whatever OSM material but default to random concrete colors (on a per building basis)
        OSM_RANDOM_PLUS  // Whatever OSM material but default to random concrete colors plus wood, stone, etc
    }

    public Polygon[] outerPolygons;
    public Polygon[] innerPolygons;

    public boolean hasCalculatedHeights = false;
    public double heightOfLowestCorner;
    public double heightOfHighestCorner;

    public boolean hasMaterial = false;
    public IBlockState walls = Blocks.BRICK_BLOCK.getDefaultState();


    public OpenStreetMaps.Element element;

    public int minHeight = 0;
    public int levels = 2;
    public int levelHeight = 4;
    public int height = levels * levelHeight;


    public Building(OpenStreetMaps.Element element, Map<Long, OpenStreetMaps.Element> ways) {
        this.element = element;
        String tagBuilding = element.tags.get("building");
        if (tagBuilding == null) throw new IllegalArgumentException("Building objects cannot be created from non-buildings. (No building tag)");

        if (element.type == OpenStreetMaps.EType.way) {
            Polygon polygon = new Polygon(element.geometry);
            this.outerPolygons = new Polygon[] { polygon };

            if (element.tags.containsKey("building:levels")) {
                try {
                    levels = Integer.parseInt(element.tags.get("building:levels"));
                    height = levels * levelHeight;
                } catch (NumberFormatException ignored) {}
            }
            if (element.tags.containsKey("building:height")) {
                try {
                    height = Integer.parseInt(element.tags.get("building:height"));
                    levelHeight = height / levels;
                } catch (NumberFormatException ignored) {}
            } else {
                height = levels * levelHeight;
            }
            if (element.tags.containsKey("building:min_height")) {
                try {
                    minHeight = Integer.parseInt(element.tags.get("building:min_height"));
                } catch (NumberFormatException ignored) {}
            }

        } else if (element.type == OpenStreetMaps.EType.relation) {
            List<Polygon> outerPolygons = new ArrayList<>(element.members.length);
            List<Polygon> innerPolygons = new ArrayList<>(element.members.length);

            for (OpenStreetMaps.Member member : element.members) {
                if (member.type != OpenStreetMaps.EType.way)
                    continue;
                if (!ways.containsKey(member.ref))
                    continue;
                OpenStreetMaps.Element way = ways.get(member.ref);
                Polygon polygon = new Polygon(way.geometry);
                String role = member.role;
                if (role == null)
                    role = "outer"; // If there's no role, then it's probably an outer edge.
                if (role.equals("outer")) {
                    outerPolygons.add(polygon);
                } else if (role.equals("inner")) {
                    innerPolygons.add(polygon);
                }
            }

            this.outerPolygons = outerPolygons.toArray(new Polygon[0]);
            this.innerPolygons = innerPolygons.toArray(new Polygon[0]);

            if (element.tags.containsKey("building:levels")) {
                try {
                    levels = Integer.parseInt(element.tags.get("building:levels"));
                } catch (NumberFormatException ignored) {}
            }
            if (element.tags.containsKey("building:height")) {
                try {
                    height = Integer.parseInt(element.tags.get("building:height"));
                    levelHeight = height / levels;
                } catch (NumberFormatException ignored) {}
            } else {
                height = levels * levelHeight;
            }
            if (element.tags.containsKey("building:min_height")) {
                try {
                    minHeight = Integer.parseInt(element.tags.get("building:min_height"));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            throw new IllegalArgumentException("Building Object constructor requires either a way or relation element");
        }
    }

    public boolean contains(int x, int z) {
        boolean inside = false;
        for (Polygon p : outerPolygons)
            if (p.isInside(x, z))
                inside = true;

        if (this.innerPolygons != null)
            for (Polygon p : innerPolygons)
                if (p.isInside(x, z))
                    inside = false;
        return inside;
    }

    public void placeIntoChunk(World world, CubePrimer cubePrimer, CubePos chunkPosition, Heights heights, GeographicProjection projection, BuildingMaterial buildingMaterialSetting) {
        if (!hasMaterial)
            calculateMaterial(buildingMaterialSetting, world.rand);
        if (!hasCalculatedHeights)
            calculateHeights(heights, projection);
        int minX = minX();
        int minZ = minZ();
        int maxX = maxX();
        int maxZ = maxZ();
        int minY;
        int maxY;
        if (minHeight != 0) {
            minY = (int)heightOfLowestCorner + minHeight;
            maxY = minY + (height - minHeight);
        } else {
            minY = (int) heightOfLowestCorner;
            maxY = (int) Math.max(heightOfHighestCorner, minY + height);
        }
        if (maxY < chunkPosition.getMinBlockY()) return;
        if (minY > chunkPosition.getMaxBlockY()) return;

        // Foundation, roof, and clear area of building
        for (int x = Math.max(minX, chunkPosition.getMinBlockX()); x <= Math.min(maxX, chunkPosition.getMaxBlockX()); x++) {
            for (int z = Math.max(minZ, chunkPosition.getMinBlockZ()); z <= Math.min(maxZ, chunkPosition.getMaxBlockZ()); z++) {
                if (contains(x, z)) {
                    // Foundation
                    if (minY >= chunkPosition.getMinBlockY())
                        cubePrimer.setBlockState(x - chunkPosition.getMinBlockX(), minY - chunkPosition.getMinBlockY(), z - chunkPosition.getMinBlockZ(), foundation);
                    // Air
                    for (int y = Math.max(minY + 1, chunkPosition.getMinBlockY()); y <= Math.min(maxY+3, chunkPosition.getMaxBlockY()); y++)
                        if (cubePrimer.getBlockState(x - chunkPosition.getMinBlockX(), y - chunkPosition.getMinBlockY(), z - chunkPosition.getMinBlockZ()) != walls)
                            cubePrimer.setBlockState(x - chunkPosition.getMinBlockX(), y - chunkPosition.getMinBlockY(), z - chunkPosition.getMinBlockZ(), Blocks.AIR.getDefaultState());
                    // Roof
                    if ((maxY - 1) <= chunkPosition.getMaxBlockY() && (maxY - 1) >= chunkPosition.getMinBlockY())
                        cubePrimer.setBlockState(x - chunkPosition.getMinBlockX(), (maxY - 1) - chunkPosition.getMinBlockY(), z - chunkPosition.getMinBlockZ(), roof);
                }
            }
        }

        // Walls (done afterward so it overwrites the edge of the roof)
        for (int y = Math.max(minY, chunkPosition.getMinBlockY()); y <= Math.min(maxY, chunkPosition.getMaxBlockY()); y++) {
            for (Polygon p : outerPolygons) {
                OpenStreetMaps.Geometry last = p.vertices[0];
                for (int i = 1; i < p.vertices.length; i++) {
                    OpenStreetMaps.Geometry current = p.vertices[i];
                    placeLine(cubePrimer, (int)last.lon, y, (int)last.lat, (int)current.lon, y, (int)current.lat, walls, chunkPosition);
                    last = current;
                }
            }
            if (innerPolygons != null)
                for (Polygon p : innerPolygons) {
                    OpenStreetMaps.Geometry last = p.vertices[0];
                    for (int i = 1; i < p.vertices.length; i++) {
                        OpenStreetMaps.Geometry current = p.vertices[i];
                        placeLine(cubePrimer, (int)last.lon, y, (int)last.lat, (int)current.lon, y, (int)current.lat, walls, chunkPosition);
                        last = current;
                    }
                }
        }
    }

    private boolean placeLine(CubePrimer cubePrimer, int x0, int y0, int z0, int x1, int y1, int z1, IBlockState block, CubePos cube) {
        boolean placedAnything = false;
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
            ) {
                cubePrimer.setBlockState(x0 - cube.getMinBlockX(), y0 - cube.getMinBlockY(), z0 - cube.getMinBlockZ(), block);
                placedAnything = true;
            }
        }
        return placedAnything;
    }

    public Building projectFromGeo(GeographicProjection projection) {
        if (projection == null)
            throw new IllegalArgumentException("Projection cannot be null!");
        for (Polygon p : outerPolygons)
            p.projectFromGeo(projection);
        if (this.innerPolygons != null)
            for (Polygon p : innerPolygons)
                p.projectFromGeo(projection);
        return this;
    }

    private int _minX = Integer.MAX_VALUE;
    public int minX() {
        if (_minX != Integer.MAX_VALUE) return _minX;
        for (Polygon p : outerPolygons) {
            int minX = p.minX();
            if (minX < _minX)
                _minX = minX;
        }
        return _minX;
    }
    private int _minZ = Integer.MAX_VALUE;
    public int minZ() {
        if (_minZ != Integer.MAX_VALUE) return _minZ;
        for (Polygon p : outerPolygons) {
            int minZ = p.minZ();
            if (minZ < _minZ)
                _minZ = minZ;
        }
        return _minZ;
    }
    private int _maxX = Integer.MIN_VALUE;
    public int maxX() {
        if (_maxX != Integer.MIN_VALUE) return _maxX;
        for (Polygon p : outerPolygons) {
            int maxX = p.maxX();
            if (maxX > _maxX)
                _maxX = maxX;
        }
        return _maxX;
    }
    private int _maxZ = Integer.MIN_VALUE;
    public int maxZ() {
        if (_maxZ != Integer.MIN_VALUE) return _maxZ;
        for (Polygon p : outerPolygons) {
            int maxZ = p.maxZ();
            if (maxZ > _maxZ)
                _maxZ = maxZ;
        }
        return _maxZ;
    }

    public void calculateHeights(Heights heights, GeographicProjection projection) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (Polygon p : outerPolygons) {
            for (OpenStreetMaps.Geometry g : p.vertices) {
                double[] geo = projection.toGeo(g.lon, g.lat);
                double height = heights.estimateLocal(geo[0], geo[1]);
                if (height > max)
                    max = height;
                if (height < min)
                    min = height;
            }
        }
        this.heightOfLowestCorner = min;
        this.heightOfHighestCorner = max;

        this.hasCalculatedHeights = true;
    }

    public IBlockState materialFromOSM(String material) {
        switch (material) {
            case "cement_block":
            case "masonry":
                return Blocks.STONEBRICK.getDefaultState();
            case "plaster":
                return Blocks.HARDENED_CLAY.getDefaultState();
            case "brick":
                return Blocks.BRICK_BLOCK.getDefaultState();
            case "timber_framing":
            case "wood":
                return Blocks.PLANKS.getDefaultState();
            case "concrete":
                return Blocks.CONCRETE.getDefaultState();
            case "glass":
            case "mirror":
                return Blocks.GLASS.getDefaultState();
            case "stone":
                return Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE_SMOOTH);
            case "sand_cement_blocks":
            case "sandstone":
                return Blocks.SANDSTONE.getDefaultState();
            case "limestone":
                return Blocks.QUARTZ_BLOCK.getDefaultState();
            case "steel":
            case "metal":
            case "metal_plates":
                return Blocks.IRON_BLOCK.getDefaultState();
            case "mud":
            case "adobe":
            case "rammedearth":
                return Blocks.DIRT.getDefaultState();
            case "plastic":
            case "vinyl":
            case "tiles":
            default:
                return null;
        }
    }

    private static final ArrayList<IBlockState> randomPlusMaterials = new ArrayList<>();
    static {
        randomPlusMaterials.add(Blocks.STONEBRICK.getDefaultState());
        randomPlusMaterials.add(Blocks.BRICK_BLOCK.getDefaultState());
        randomPlusMaterials.add(Blocks.PLANKS.getDefaultState());
        randomPlusMaterials.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE_SMOOTH));
        randomPlusMaterials.add(Blocks.SANDSTONE.getDefaultState());
        randomPlusMaterials.add(Blocks.QUARTZ_BLOCK.getDefaultState());
        randomPlusMaterials.add(Blocks.HARDENED_CLAY.getDefaultState());
        for (EnumDyeColor color : EnumDyeColor.values()) {
            randomPlusMaterials.add(Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, color));
            randomPlusMaterials.add(Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, color));
        }
    }

    private void calculateMaterial(BuildingMaterial buildingMaterialSetting, Random rand) {
        switch (buildingMaterialSetting) {
            case BRICK:
                walls = Blocks.BRICK_BLOCK.getDefaultState();
                break;
            case STONE_BRICK:
                walls = Blocks.STONEBRICK.getDefaultState();
                break;
            case CONCRETE:
                walls = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.SILVER);
                break;
            case OSM_BRICK: {
                if (!element.tags.containsKey("building:material") || (walls = materialFromOSM(element.tags.get("building:material"))) == null) {
                    walls = Blocks.BRICK_BLOCK.getDefaultState();
                }
                break;
            }
            case OSM_STONE_BRICK: {
                if (!element.tags.containsKey("building:material") || (walls = materialFromOSM(element.tags.get("building:material"))) == null) {
                    walls = Blocks.STONEBRICK.getDefaultState();
                }
                break;
            }
            case OSM_CONCRETE: {
                if (!element.tags.containsKey("building:material") || (walls = materialFromOSM(element.tags.get("building:material"))) == null) {
                    walls = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.SILVER);
                }
                break;
            }
            case RANDOM_CONCRETE: {
                walls = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.values()[rand.nextInt(EnumDyeColor.values().length)]);
                break;
            }
            case RANDOM_PLUS: {
                walls = randomPlusMaterials.get(rand.nextInt(randomPlusMaterials.size()));
                break;
            }
            case OSM_RANDOM_CONCRETE: {
                if (!element.tags.containsKey("building:material") || (walls = materialFromOSM(element.tags.get("building:material"))) == null) {
                    walls = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.values()[rand.nextInt(EnumDyeColor.values().length)]);
                }
                break;
            }
            case OSM_RANDOM_PLUS: {
                if (!element.tags.containsKey("building:material") || (walls = materialFromOSM(element.tags.get("building:material"))) == null) {
                    walls = randomPlusMaterials.get(rand.nextInt(randomPlusMaterials.size()));
                }
                break;
            }
        }
        hasMaterial = true;
    }

    @Override
    public String toString() {
        if (!element.tags.containsKey("name"))
        return "Building{" +
                "heightOfLowestCorner=" + heightOfLowestCorner +
                ", heightOfHighestCorner=" + heightOfHighestCorner +
                '}';
        else
            return "Building{" +
                    "name = " + element.tags.get("name") +
                    ", heightOfLowestCorner=" + heightOfLowestCorner +
                    ", heightOfHighestCorner=" + heightOfHighestCorner +
                    '}';
    }
}
