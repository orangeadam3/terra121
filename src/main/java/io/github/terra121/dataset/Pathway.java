package io.github.terra121.dataset;

import io.github.terra121.TerraMod;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.Range;

import java.io.IOException;
import java.util.*;
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
 * If you don't understand something, don't hessitate to reach out to me (the same goes for all VectorPath generation stuff):
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

    // CLASS METHODS

    /**
     * Calculates width of a road based on number of lanes and OSM type. Returns the width as double.
     */
    public static double calculateRoadWidth(int l, OpenStreetMaps.Type c) {
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

    public static BiFunction<Double, BlockPos, IBlockState> getMaterial(OpenStreetMaps.Surface s) {

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
     * Given two points, calculate the slope between the two points.
     */
    public static double getIncline(Vec3d fin, Vec3d init) {

        double dP12 = fin.dotProduct(init);
        double X = fin.x - init.x * fin.x - init.x;
        double Z = fin.z - init.z * fin.z - init.z;
        double pytP1 = Math.sqrt((X*X + Math.pow(fin.y - init.y, 2) + Z*Z));
        double pytP2 = Math.sqrt((X*X + Z*Z));

        double cosC = dP12 / (pytP1 * pytP2);
        double C = Math.acos(cosC);

        return Math.tan(C);

    }

    public static int getTunnelY(Double[] fin, Double[] init, double x, double z, Heights heights) {
        int height = 0;
        // get all points (2d) along tunnel
        double Dz = fin[0] - init[0];
        double Dx = fin[1] - init[1];
        double sXZ = Dz/Dx;
        List<Double> xpoints = new ArrayList<>();
        List<Double> zpoints = new ArrayList<>();
        Range<Double> xrange = Range.between(x+1, x-1);
        Range<Double> zrange = Range.between(z-1, z+1);

        double cx=x;
        double cz=z;
        boolean end = false;
        while (!end) {
            for (double on = sXZ; on <= 1; on+=sXZ) {
                cx+=1;
                xpoints.add(cx);
            }
            cz+=1;
            zpoints.add(cz);
            if (xrange.contains(cx) && zrange.contains(cz)) end = true;
        }
        if (xpoints.size()==zpoints.size()) {
            double dist = Math.abs(xpoints.get(0) - x);
            int index = 0;
            for (int c = 1; c < xpoints.size(); c++) {
                double cdist = Math.abs(xpoints.get(c)-x);
                if (cdist < dist) {
                    index = c;
                    dist = cdist;
                }
            }
            // closest x + z
            double xmatch = xpoints.get(index);
            double zmatch = zpoints.get(index);

            double sY = heights.estimateLocal(init[0], init[1]);
            double eY = heights.estimateLocal(fin[0], fin[1]);

            double Dy = eY - sY;
            double sXY = Dy/Dx;

            List<Double> ypoints = new ArrayList<>();
            Range<Double> yrange = Range.between(eY-1, eY+1);

            double cx2 = x;
            double cy = sY;
            boolean endy = false;
            while (!endy) {
                for (double ony = sXY; ony <= 1; ony+=sXY) {
                    cy+=1;
                    ypoints.add(cy);
                }
                cx2+=1;
                if (yrange.contains(cy)) endy = true;
            }

            TerraMod.LOGGER.info("height {}", height);
            height = (int) Math.floor(ypoints.get(index));

        }
        return height;
    }

    /**
     * This method uses the <i>exact</i> same code as in RoadGenerator. However, instead of
     * generating the blocks directly, it stores them in VectorPath objects, which allows sections of roads
     * to be grouped together very easily. This means generation of special features, such as tunnels
     * and bridges, is made possible by calculating the incline directly before generation instead of on a
     * block by block basis. Use this method to generate VectorPaths using the same args as RoadGenerator.placeEdge() ( except
     * the edges must be in a set and are iterated over within the method instead of in generate() ).<br>
     * Returns List of VectorPathGroup. Do not use for buildings.
     */
    public static List<VectorPathGroup> chunkStructuresAsVectors(Set<OpenStreetMaps.Edge> edges, World world, int cubeX, int cubeY, int cubeZ,
                                                                 Heights heights, GeographicProjection projection) {
        BiFunction<Double, BlockPos, IBlockState> state;
        VectorPath v;
        List<Double> allX = new ArrayList<>();
        List<Double> allY = new ArrayList<>();
        List<Double> allZ = new ArrayList<>();
        List<VectorPath> air = new ArrayList<>();
        List<VectorPathGroup> allPaths = new ArrayList<>();

        for (OpenStreetMaps.Edge e : edges) {

            boolean tunnel = false;
            List<VectorPath> evp = new ArrayList<>();

            if (e.type != OpenStreetMaps.Type.BUILDING) {

                if (e.attribute == OpenStreetMaps.Attributes.ISTUNNEL || e.attribute == OpenStreetMaps.Attributes.ISBRIDGE) tunnel = true;
                /*if (e.attribute == OpenStreetMaps.Attributes.ISTUNNEL || e.attribute == OpenStreetMaps.Attributes.ISBRIDGE) {

                    if (e.wp != null) {

                        Double[] start = {e.wp[0], e.wp[1]};
                        Double[] end = {e.wp[2], e.wp[3]};

                        // todo use estimateLocal instead of getElevation
                        e.slope = getIncline(new Vec3d(end[0], heights.estimateLocal(end[0], end[1]), end[1]),
                                new Vec3d(start[0], heights.estimateLocal(start[0], start[1]), start[1]));
                        tunnel = true;

                        TerraMod.LOGGER.info("elevation: {}", heights.estimateLocal(end[0], end[1]));
                        TerraMod.LOGGER.info("elevation e: {}",heights.estimateLocal(start[0], start[1]));
                        TerraMod.LOGGER.info("e.slope: {}", e.slope);

                    } else {
                        TerraMod.LOGGER.warn("Tunnel or bridge didn't have whole path data! (This could be an OSM data fault or a bug)");
                    }
                }*/

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
                        r = (int) Pathway.calculateRoadWidth(e.lanes, e.type);
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
                            int y;
                            if (!tunnel) y = (int) Math.floor(heights.estimateLocal(geo[0], geo[1]) - cubeY * 16);
                            else {
                                // lon lat
                                Double[] start = {e.wp[0], e.wp[1]};
                                Double[] end = {e.wp[2], e.wp[3]};
                                y = getTunnelY(end, start, x, z, heights);
                                TerraMod.LOGGER.info("height: {}", y);
                            }

                            // if not in this range, someone else will handle it
                            if (y >= 0 && y < 16) {

                                allX.add((double) (x + cubeX * 16));
                                allY.add((double) (y + cubeY * 16));
                                allZ.add((double) (z + cubeZ * 16));

                                BlockPos pathPos = new BlockPos(x + cubeX * 16, y + cubeY * 16, z + cubeZ * 16);
                                IBlockState bstate = state.apply(distance, pathPos);

                                if (bstate != null) {

                                    try {

                                        v = VectorPathFromValues(allX, allY, allZ, bstate, e.attribute, null);
                                        evp.add(v);

                                    } catch (IOException ex) {
                                        TerraMod.LOGGER.error("An IOException has occured while trying to call VectorPathFromValues(). Are all coordinate lists equal in size?");
                                        ex.printStackTrace();
                                    }

                                    // clear the above blocks (to a point, we don't want to be here all day)
                                    List<Double> abX = new ArrayList<>();
                                    List<Double> abY = new ArrayList<>();
                                    List<Double> abZ = new ArrayList<>();
                                    IBlockState defState = Blocks.AIR.getDefaultState();
                                    for (int ay = y + 1;
                                         ay < 32 && world.getBlockState(new BlockPos(x + cubeX * 16, ay + cubeY * 16, z + cubeZ * 16)) != defState; ay++) {
                                        abX.add((double) x + cubeX * 16);
                                        abY.add((double) ay + cubeY * 16);
                                        abZ.add((double) z + cubeZ * 16);
                                    }
                                    try {
                                        air.add(VectorPathFromValues(abX, abY, abZ, defState, OpenStreetMaps.Attributes.NONE, null));
                                    } catch (IOException ex) {
                                        TerraMod.LOGGER.error("An IOException has occured while trying to call VectorPathFromValues(). Are all coordinate lists equal in size?");
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }

                allPaths.add(new VectorPathGroup(evp));

            }
        }

        allPaths.add(new VectorPathGroup(air));

        return allPaths;

    }

    /**
     * Converts x, y and z doubles into VectorPath objects and passes through other values belonging
     * to said x, y and z values and returns a VectorPath.
     */
    public static VectorPath VectorPathFromValues(List<Double> x, List<Double> y, List<Double> z,
                                                  IBlockState material, OpenStreetMaps.Attributes attribute, OpenStreetMaps.Edge e) throws IOException {

        if (x == null) return new VectorPath(null, null, null, e);
        Vec3d vector;
        List<Vec3d> vectors = new ArrayList<>();

        if (x.size() == y.size() && y.size() == z.size()) {

            for (int i = 0; i < x.size(); i++) {
                vector = new Vec3d(x.get(i), y.get(i), z.get(i));
                vectors.add(vector);
            }

        } else {

            TerraMod.LOGGER.error("While attempting to create a VectorPath, the provided x, y and z lists were not equal. The VectorPath will not generate.");
            throw new IOException();

        }

        return new VectorPath(vectors, material, attribute, e);

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

        public List<VectorPath> paths;

        public VectorPathGroup(List<VectorPath> paths) {
            this.paths = paths;
        }

    }

    /**
     * An object that holds a List of vectors (Vec3d) of block locations, as well as their respective IBlockState, OSM relations (String) and Attributes.
     * <br> While an OpenStreetMaps.Edge is required to instantiate, for most situations it should be assigned as null.
     */
    public static class VectorPath {

        public List<Vec3d> path = new ArrayList<>();
        public IBlockState material;
        public OpenStreetMaps.Attributes attribute;
        public OpenStreetMaps.Edge edge;

        public VectorPath(List<Vec3d> blockLocs, IBlockState material, OpenStreetMaps.Attributes attribute, OpenStreetMaps.Edge e) {

            if (blockLocs != null) {
                this.path.addAll(blockLocs);
            }

            this.material = material;
            this.attribute = attribute;
            this.edge = e;
        }

    }

    public static void main(String[] args) {

        Double[] start = {46.6477470,8.5908610};
        Double[] fin = {46.6477470,8.5908610};

        //System.out.println(getTunnelY());

    }

}
