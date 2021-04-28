package io.github.terra121.dataset;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.terra121.TerraMod;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;

import static io.github.terra121.populator.VectorPathGenerator.bound;

/**
 * dataset.Pathway holds most of the nessisary methods and classes to
 * generate pathways using 3 dimentional vectors (Vec3d). Generating pathways with
 * special features (e.g. tunnels, bridges) requires use of these
 * methods and classes.
 * I decided to create a new generator because the changes I'd have to make to RoadGenerator would be so large that:
 * a, it probably wouldn't work anymore and b, because it would be similar if not less work to make an experimental generator.
 * VectorPath is also intended to be used as a more experimental generation type, so adding features like
 * more specific surface block types or road signs (etc.) can be done without touching RoadGenerator.
 * The best way to convert the data used in generating pathways block by block
 * as in populator.RoadGenerator is by using Pathway.VectorPathFromEdge().
 * There are a lot of "space saving" classes and methods, but I mainly use them to make sure the
 * more surface code is easier to read and understand.<br>
 * If you don't understand something, don't hesitate to reach out to me (the same goes for all VectorPath generation stuff):
 * taeko-chan on Github
 * taeko#4924 on Discord
 */
public class Pathway {

    // surfaces
    private static final IBlockState ASPHALT = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.GRAY);
    private static final IBlockState PATH = Blocks.GRASS_PATH.getDefaultState();
    private static final IBlockState WATER_SOURCE = Blocks.WATER.getDefaultState();
    private static final IBlockState WATER_BEACH = Blocks.DIRT.getDefaultState();
    private static final IBlockState DIRT = Blocks.DIRT.getDefaultState();
    private static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    private static final IBlockState WOOD = Blocks.PLANKS.getDefaultState();
    private static final IBlockState CONCRETE = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.SILVER);
    private static final IBlockState COBBLE = Blocks.COBBLESTONE.getDefaultState();
    // roof and walls for buildings by AidanLovelace
    public static final IBlockState ROOF = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.SILVER);
    public static final IBlockState WALLS = Blocks.BRICK_BLOCK.getDefaultState();

    // CLASS METHODS

    /**
     * Calculates width of a road based on number of lanes and OSM type. Returns the width as double.
     */
    private static double pathWidth(int l, OpenStreetMaps.Type c) {
        double width = 2;
        switch (c) {
            case MINOR:
                width = l * 2;
                break;
            case SIDE:
                width = l * 3 + 1;
                break;
            case INTERCHANGE:
            case MAIN:
                width = 3 * l + l;
                break;
            case FREEWAY:
            case LIMITEDACCESS:
                width = 4 * l + l + 6;
                break;
            default:
                break;
        }
        // there are really no roads over 256 m width. it's probably a mistake.
        // i even researched this, and the widest road (that 50 lane one in China) is 123 meters, just under
        // the limit for a byte. so then...
        if (width > 256) {
            width = 2;
        }
        return width / 2;
    }

    private static BiFunction<Double, BlockPos, IBlockState> getMaterial(OpenStreetMaps.Surface s) {

        switch (s) {
            case ASPHALT:
                return (dis, bpos) -> ASPHALT;
            case DIRT:
                return (dis, bpos) -> DIRT;
            case WOOD:
                return (dis, bpos) -> WOOD;
            case COBBLE:
                return (dis, bpos) -> COBBLE;
            case GRAVEL:
                return (dis, bpos) -> GRAVEL;
            case CONCRETE:
                return (dis, bpos) -> CONCRETE;
            case GRASS_PATH:
                return (dis, bpos) -> PATH;
            default:
                throw new IllegalStateException("Unexpected value: " + s);
        }
    }

    /**
     * Get length of way
     * */
    private static double distanceAlong(List<Double> latG, List<Double> lonG, GeographicProjection p) {

        double dist = 0;

        if (latG.size() == lonG.size()) {
            List<Double> lat = new ArrayList<>();
            List<Double> lon = new ArrayList<>();

            for (int i = 0; i < latG.size(); i++) {

                double[] n = p.fromGeo(lonG.get(i), latG.get(i));

                lat.add(n[1]);
                lon.add(n[0]);

            }

            // calculate length of way
            for (int i = 0; i <= lat.size() - 1; i++) {

                if (i == lat.size() - 1) {

                    break;

                } else {

                    double xdif = lat.get(i + 1) - lat.get(i);
                    double zdif = lon.get(i + 1) - lon.get(i);

                    // distance(lat.get(i), lon.get(i), lat.get(i + 1), lon.get(i + 1));

                    double d = Math.sqrt((xdif * xdif) + (zdif * zdif));
                    dist += d;

                }
            }
        }
        return dist;
    }

    private static int relativeHeight(double x1, double z1, double x2, double z2, List<Double> pointsX,
                                     List<Double> pointsZ, double y1, double y2, GeographicProjection projection, double x, double z) {

        // height within cube (0-16)
        double exactY = absoluteHeight(x1,z1,x2,z2,pointsX,pointsZ,y1,y2,projection,x,z);

        return (int) (exactY - Math.floor(exactY / 16) * 16);

    }

    private static int absoluteHeight(double x1, double z1, double x2, double z2, List<Double> pointsX,
                                     List<Double> pointsZ, double y1, double y2, GeographicProjection projection, double x, double z) {

        // get distance from (x,y,z)1 to (x,y,z)2
        double xdif = x2 - x1;
        double zdif = z2 - z1;
        double ydif = y2 - y1;

        double xzLinDist = Math.sqrt((xdif * xdif) + (zdif * zdif)); // smallest length
        double xzActualDist = distanceAlong(pointsZ, pointsX, projection); // actual length
        double distortion = xzActualDist / xzLinDist; // meh attempt at correcting for distortion

        double cxdif = x - x1;
        double czdif = z - z1;

        double distorted = Math.sqrt((cxdif * cxdif) + (czdif * czdif)); // shortest distance between current point and begin
        double nondistorted = distorted * distortion; // correct distortion

        double exactY = y1 + ((nondistorted / xzActualDist) * ydif);

        return (int) exactY;

    }

    /**
     * Place simple one block wide path. Intended for buildings, as the material will be brick.
     * */
    private static void doBuildings(CubePos cubePos, OpenStreetMaps.Edge e, GeographicProjection projection, Heights heights, World world) {

        int cubeX = cubePos.getX();
        int cubeY = cubePos.getY();
        int cubeZ = cubePos.getZ();

        double start = e.slon;
        double end = e.elon;

        if (start > end) {
            double tmp = start;
            start = end;
            end = tmp;
        }

        int sx = (int) Math.floor(start) - cubeX * 16;
        int ex = (int) Math.floor(end) - cubeX * 16;

        if (ex >= 16) ex = 16 - 1;

        for (int x = Math.max(sx, 0); x <= ex; x++) {
            double realx = (x + cubeX * 16);
            if (realx < start)
                realx = start;

            double nextx = realx + 1;
            if (nextx > end)
                nextx = end;

            int from = (int) Math.floor((e.slope * realx + e.offset)) - cubeZ * 16;
            int to = (int) Math.floor((e.slope * nextx + e.offset)) - cubeZ * 16;

            if (from > to) {
                int tmp = from;
                from = to;
                to = tmp;
            }

            if (to >= 16) to = 16 - 1;

            for (int z = Math.max(from, 0); z <= to; z++) {

                double[] geo = projection.toGeo(x + cubeX * (16), z + cubeZ * (16));
                int y = (int) Math.floor(heights.estimateLocal(geo[0], geo[1])) - cubeY * 16;

                if (y >= 0 && y < 16) {

                    BlockPos b = new BlockPos(x, y, z);
                    world.setBlockState(b, Blocks.BRICK_BLOCK.getDefaultState());

                }
            }
        }
    }

    /**
     * Converts HashSet of OpenStreetMaps.Edge to 10 VectorPathGroups,
     * each one of which containing all path blocks within a cube with a certain surface material.
     */
    public static List<VectorPathGroup> chunkStructuresAsVectors(Set<OpenStreetMaps.Edge> edges, World world, int cubeX, int cubeY, int cubeZ,
                                                                 Heights heights, GeographicProjection projection) {

        BiFunction<Double, BlockPos, IBlockState> state;

        VectorPath3D v;

        List<VectorPath3D> asphalt = new ArrayList<>();
        List<VectorPath3D> path = new ArrayList<>();
        List<VectorPath3D> dirt = new ArrayList<>();
        List<VectorPath3D> wood = new ArrayList<>();
        List<VectorPath3D> cobble = new ArrayList<>();
        List<VectorPath3D> gravel = new ArrayList<>();
        List<VectorPath3D> concrete = new ArrayList<>();
        List<VectorPath3D> waterway = new ArrayList<>();
        List<VectorPath3D> evp = new ArrayList<>();
        List<VectorPathGroup> allPaths = new ArrayList<>();
        double[] superSecretCoords = projection.fromGeo(45.9766701, 7.6569730);

        for (OpenStreetMaps.Edge e : edges) {

            boolean tunnel = false;

            if (e.type != OpenStreetMaps.Type.BUILDING) {

                if (e.attribute == OpenStreetMaps.Attributes.ISTUNNEL || e.attribute == OpenStreetMaps.Attributes.ISBRIDGE) tunnel = true;

                int r;

                // process edges into vec3d (copied code mostly from RoadGenerator :P)
                switch (e.type) {
                    case STREAM:
                        state = ((dis, bpos) -> riverState(world, dis, bpos));
                        r = 1;
                        break;
                    case RIVER:
                        state = ((dis, bpos) -> riverState(world, dis, bpos));
                        r = 5;
                        break;
                    case PATH:
                        state = getMaterial(e.surf);
                        r = 1;
                        break;
                    default:
                        state = getMaterial(e.surf);
                        r = (int) Pathway.pathWidth(e.lanes, e.type);
                        break;
                }

                double x0 = 0;
                double b = r;
                if (Math.abs(e.slope) >= 0.000001) {
                    x0 = r / Math.sqrt(1 + 1 / (e.slope * e.slope));
                    b = (e.slope < 0 ? -1 : 1) * x0 * (e.slope + 1.0 / e.slope);
                }

                double j = e.slon - (cubeX * 16);
                double k = e.elon - (cubeX * 16);

                if (j > k) {
                    double t = j;
                    j = k;
                    k = t;
                }

                double ij = j - r;
                double ik = k + r;

                if (j <= 0) {
                    j = 0;
                    //ij=0;
                }
                if (k >= 16) {
                    k = 16;
                    //ik = 16;
                }

                double off = e.offset - (cubeZ * 16) + e.slope * (cubeX * 16);

                int is = (int) Math.floor(ij);
                int ie = (int) Math.floor(ik);

                List<Double> allX = new ArrayList<>();
                List<Double> allY = new ArrayList<>();
                List<Double> allZ = new ArrayList<>();

                for (int x = is; x <= ie; x++) {

                    double ul = bound(x, e.slope, j, k, r, x0, b, 1) + off; //TODO: save these repeated values
                    double ur = bound((double) x + 1, e.slope, j, k, r, x0, b, 1) + off;
                    double ll = bound(x, e.slope, j, k, r, x0, b, -1) + off;
                    double lr = bound((double) x + 1, e.slope, j, k, r, x0, b, -1) + off;

                    double from = Math.min(Math.min(ul, ur), Math.min(ll, lr));
                    double to = Math.max(Math.max(ul, ur), Math.max(ll, lr));

                    if (from == from) {

                        int ifrom = (int) Math.floor(from);
                        int ito = (int) Math.floor(to);

                        if (ifrom <= -16)
                            ifrom = -15;
                        if (ito >= 32)
                            ito = 31;

                        for (int z = ifrom; z <= ito; z++) {
                            //get the part of the center line i am tangent to (i hate high school algebra!!!)
                            double mainX = x;
                            if (Math.abs(e.slope) >= 0.000001)
                                mainX = ((double) z + (double) x / e.slope - off) / (e.slope + 1 / e.slope);

                            double mainZ = e.slope * mainX + off;

                            //get distance to closest point
                            double distance = mainX - (double) x;
                            distance *= distance;
                            double t = mainZ - (double) z;
                            distance += t * t;
                            distance = Math.sqrt(distance);

                            double[] geo = projection.toGeo(mainX + cubeX * (16), mainZ + cubeZ * (16));

                            int y = (int) Math.floor(heights.estimateLocal(geo[0], geo[1]) - cubeY * 16);
                            int clearBlock = 32;

                            Integer heightCheck = null;

                            // override y if tunnel or bridge
                            if (tunnel) {

                                // only clear up to 6 blocks above in tunnels (bridges too i guess, but when would that come up?)
                                clearBlock = 6;

                                try {
                                    // ik this is hard to read
                                    double[] start = projection.fromGeo(e.wp.lon.get(0), e.wp.lat.get(0));
                                    double[] end = projection.fromGeo(e.wp.lon.get(e.wp.lon.size()-1), e.wp.lat.get(e.wp.lat.size()-1));

                                    double sy = heights.estimateLocal(e.wp.lon.get(0), e.wp.lat.get(0));
                                    double ey = heights.estimateLocal(e.wp.lon.get(e.wp.lon.size()-1), e.wp.lat.get(e.wp.lat.size()-1));

                                    heightCheck = absoluteHeight(start[0], start[1], end[0], end[1], e.wp.lat, e.wp.lon, sy, ey, projection,
                                            mainX + cubeX * 16, mainZ + cubeZ * 16);

                                    y = relativeHeight(start[0], start[1], end[0], end[1], e.wp.lon, e.wp.lat, sy, ey, projection, mainX + cubeX * 16, mainZ + cubeZ * 16);

                                } catch (Exception ignored) { } // data fault, probably
                            }

                            // if not in this range, someone else will handle it
                            if (y >= 0 && y < 16) {

                                allX.add((double) (x + cubeX * 16));
                                allZ.add((double) (z + cubeZ * 16));
                                int actualHeight = y + cubeY * 16;

                                // do correction if height is much lower or higher than unprocessed mathematical result
                                boolean heightIsTooLow = heightCheck != null && actualHeight - heightCheck < -3,
                                        heightIsTooHigh = heightCheck != null && actualHeight - heightCheck > 3;

                                // prevent large jumps in height
                                if (heightIsTooLow) actualHeight += heightCheck - actualHeight;
                                if (heightIsTooHigh) actualHeight -= actualHeight - heightCheck;

                                allY.add((double) actualHeight);

                                BlockPos pathPos = new BlockPos(x + cubeX * 16, actualHeight, z + cubeZ * 16);
                                IBlockState bstate = state.apply(distance, pathPos);

                                if (bstate != null) {

                                    try {
                                        v = VectorPathFromValues(allX, allY, allZ, bstate, e.attribute, null);

                                        if (e.type == OpenStreetMaps.Type.RIVER || e.type == OpenStreetMaps.Type.STREAM) {

                                            waterway.add(v);

                                        } else {

                                            // seriously if anyone has a better idea for this please let me know
                                            switch (e.surf) {
                                                case ASPHALT:
                                                    asphalt.add(v);
                                                    break;
                                                case CONCRETE:
                                                    concrete.add(v);
                                                    break;
                                                case COBBLE:
                                                    cobble.add(v);
                                                    break;
                                                case WOOD:
                                                    wood.add(v);
                                                    break;
                                                case GRAVEL:
                                                    gravel.add(v);
                                                    break;
                                                case DIRT:
                                                    dirt.add(v);
                                                    break;
                                                case GRASS_PATH:
                                                    path.add(v);
                                                    break;
                                                case IGNORE:
                                                default:
                                                    evp.add(v);
                                            }
                                        }

                                    } catch (IOException ex) {
                                        TerraMod.LOGGER.error("An IOException has occured while trying to call VectorPathFromValues(). " +
                                                "Are all coordinate lists equal in size? When making a GitHub issue please include the following: ");
                                        ex.printStackTrace();
                                    }

                                    IBlockState defState = Blocks.AIR.getDefaultState();

                                    int ay = actualHeight + 1;
                                    while (ay < actualHeight + clearBlock &&
                                            world.getBlockState(new BlockPos(x + cubeX * 16, ay, z + cubeZ * 16)) != defState &&
                                            // don't overwrite buildings
                                            world.getBlockState(new BlockPos(x + cubeX * 16, ay, z + cubeZ * 16)) != WALLS &&
                                            world.getBlockState(new BlockPos(x + cubeX * 16, ay, z + cubeZ * 16)) != ROOF
                                    ) {
                                        world.setBlockState(new BlockPos(x + cubeX * 16, ay, z + cubeZ * 16), defState);
                                        ay++;
                                    }
                                    // doesn't work quite yet lol
                                    if (world.getBlockState(new BlockPos(superSecretCoords[1], 4355, superSecretCoords[0])).getBlock().getDefaultState()
                                            != Blocks.STANDING_SIGN.getDefaultState()) {
                                        Pathway.superSecretMethod(world, (int)superSecretCoords[1], 4355, (int)superSecretCoords[0]);
                                    }
                                }
                            }
                        }
                    }
                }
            } else doBuildings(new CubePos(cubeX, cubeY, cubeZ), e, projection, heights, world);
        }

        // todo this is horrible however it keeps them from bleeding into eachother
        allPaths.add(new VectorPathGroup(evp));
        allPaths.add(new VectorPathGroup(waterway));
        allPaths.add(new VectorPathGroup(path));
        allPaths.add(new VectorPathGroup(cobble));
        allPaths.add(new VectorPathGroup(gravel));
        allPaths.add(new VectorPathGroup(wood));
        allPaths.add(new VectorPathGroup(dirt));
        allPaths.add(new VectorPathGroup(asphalt));
        allPaths.add(new VectorPathGroup(concrete));

        return allPaths;

    }

    /**
     * Converts x, y and z doubles into VectorPath objects and passes through other values belonging
     * to said x, y and z values and returns a VectorPath.
     */
    public static VectorPath3D VectorPathFromValues(List<Double> x, List<Double> y, List<Double> z,
                                                    IBlockState material, OpenStreetMaps.Attributes attribute, OpenStreetMaps.Edge e) throws IOException {

        if (x == null) return new VectorPath3D(null, null, null, e);
        Vec3d vector;
        List<Vec3d> vectors = new ArrayList<>();

        if (x.size() == y.size() && y.size() == z.size()) {

            for (int i = 0; i < x.size(); i++) {
                vector = new Vec3d(x.get(i), y.get(i), z.get(i));
                vectors.add(vector);
            }

        } else {

            TerraMod.LOGGER.error("While attempting to create a VectorPath, the provided x, y and z lists were not equal. The VectorPath will not generate." +
                    " When making a GitHub issue, please include the following: ");
            throw new IOException();

        }

        return new VectorPath3D(vectors, material, attribute, e);

    }

    /***
     * Required for processing rivers
     */
    private static IBlockState riverState(World world, double dis, BlockPos pos) {
        IBlockState prev = world.getBlockState(pos);
        if (dis > 2) {
            if (!prev.getBlock().equals(Blocks.AIR))
                return null;
            IBlockState under = world.getBlockState(pos.down());
            if (under.getBlock() instanceof BlockLiquid)
                return null;
            return WATER_BEACH;
        } else return WATER_SOURCE;
    }

    public static void superSecretMethod(World world, int x, int y, int z) {

        Block secret = Blocks.STANDING_SIGN;

        BlockPos b = new BlockPos(x,y,z);
        if (world.getBlockState(b).getBlock() != Blocks.AIR) {

            world.setBlockState(b, secret.getDefaultState());
            TileEntity t = world.getTileEntity(b);

            if (t instanceof TileEntitySign) {
                TileEntitySign s = (TileEntitySign) t;
                s.signText[0] = new TextComponentString("liebe grüsse");
                s.signText[1] = new TextComponentString("aus zürich");
                s.signText[2] = new TextComponentString("- taeko-chan");
                s.signText[3] = new TextComponentString("der reusstal ist in uri");
            }
        }
    }

    // CLASS SUBCLASSES

    /**
     * Class used to group multiple VectorPaths together.
     * Mainly intended for use in CubeStructuresAsVectors(), which returns 3 of this type
     * and allows for them to be generated seperatly as rivers, buildings and roads.<br>
     * <strong>IMPORTANT:</strong><br>
     * For generation, check path type in this order:<br>
     * RIVER, BUILDING, PATH<br>
     * Only after these three have been checked, should a "default:" or "else" be used
     * to generate roads. <strong>Roads will most likely not always have type ROAD!</strong>
     */
    public static class VectorPathGroup {

        public List<VectorPath3D> paths;

        public VectorPathGroup(List<VectorPath3D> paths) {
            this.paths = paths;
        }

    }

    /**
     * An object that holds a List of vectors (Vec3d) of block locations, as well as their respective IBlockState, OSM relations (String) and Attributes.
     * <br> While an OpenStreetMaps.Edge is required to instantiate, for most situations it should be assigned as null.
     */
    public static class VectorPath3D {

        public List<Vec3d> path = new ArrayList<>();
        public IBlockState material;
        public OpenStreetMaps.Attributes attribute;
        public OpenStreetMaps.Edge edge;

        public VectorPath3D(List<Vec3d> blockLocs, IBlockState material, OpenStreetMaps.Attributes attribute, OpenStreetMaps.Edge e) {

            if (blockLocs != null) {
                this.path.addAll(blockLocs);
            }

            this.material = material;
            this.attribute = attribute;
            this.edge = e;

        }

    }

    public static class LatLon {

        List<Double> lat;
        List<Double> lon;

        public LatLon(List<Double> lat, List<Double> lon) {

            this.lat = lat;
            this.lon = lon;

        }
    }

    public static void main(String[] args) {

        // Double[] start = {46.6477470,8.5908610};
        // Double[] fin = {46.6477470,8.5908610};

        //System.out.println(getTunnelY());

    }

}
