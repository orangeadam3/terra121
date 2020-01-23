package io.github.terra121;

import java.util.Set;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.terra121.dataset.Heights;
import io.github.terra121.dataset.OpenStreetMaps;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import java.util.Random;

public class RoadGenerator {

    private static final double SCALE = 100000.0;
    private static final IBlockState ASPHALT = Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.GRAY);

    private OpenStreetMaps osm;
    private Heights heights;

    public RoadGenerator(OpenStreetMaps osm, Heights heights) {
        this.osm = osm;
        this.heights = heights;
    }

    public void generateRoads(ICube cube, int cubeX, int cubeY, int cubeZ, World world, Random rand) {
        Set<OpenStreetMaps.Edge> edges = osm.chunkStructures(cubeX, cubeZ);

        if(edges!=null) for(OpenStreetMaps.Edge e: edges) {
            if(e.type == OpenStreetMaps.Type.MAJOR || e.type == OpenStreetMaps.Type.HIGHWAY) {

                double r = 1.5*e.lanes/SCALE; //scale with lanes
                double x0 = 0;
                double b = r;
                if(Math.abs(e.slope)>=0.000001) {
                    x0 = r/Math.sqrt(1 + 1 / (e.slope * e.slope));
                    b = (e.slope < 0 ? -1 : 1) * x0 * (e.slope + 1.0 / e.slope);
                }

                double j = e.slon - (cubeZ*16)/SCALE;
                double k = e.elon - (cubeZ*16)/SCALE;
                double off = e.offset - (cubeX*16)/SCALE + e.slope*(cubeZ*16)/SCALE;

                if(j>k) {
                    double t = j;
                    j = k;
                    k = t;
                }

                if(j<=0) j=0;
                if(k>=16/SCALE) k=16/SCALE;

                int is = (int)Math.floor((j-r)*SCALE);
                int ie = (int)Math.floor((k+r)*SCALE);

                for(int z=is; z<=ie; z++) {
                    double X = z/SCALE;
                    double ul = bound(X, e.slope, j, k, r, x0, b, 1) + off; //TODO: save these repeated values
                    double ur = bound(X+1/SCALE, e.slope, j, k, r, x0, b, 1) + off;
                    double ll = bound(X, e.slope, j, k, r, x0, b, -1) + off;
                    double lr = bound(X+1/SCALE, e.slope, j, k, r, x0, b,-1) + off;

                    double from = Math.min(Math.min(ul,ur),Math.min(ll,lr));
                    double to = Math.max(Math.max(ul,ur),Math.max(ll,lr));

                    if(from==from) {
                        int ifrom = (int)Math.floor(from*SCALE);
                        int ito = (int)Math.floor(to*SCALE);

                        if(ifrom <= -1*16)
                            ifrom = 1 - 16;
                        if(ito >= 16*2)
                            ito = 16*2-1;

                        for(int x=ifrom; x<=ito; x++) {

                            //get the part of the center line i am tangent to (i hate high school algebra!!!)
                            double Z = x/SCALE;
                            double mainX = X;
                            if(Math.abs(e.slope)>=0.000001)
                                mainX = (Z + X/e.slope - off)/(e.slope + 1/e.slope);

                            if(mainX<j) mainX = j;
                            else if(mainX>k) mainX = k;

                            double mainZ = e.slope*mainX + off;

                            int y = (int)Math.floor(heights.estimateLocal(mainZ + cubeX*(16/SCALE), mainX + cubeZ*(16/SCALE)) - cubeY*16);

                            if (y >= 0 && y < 16) { //if not in this range, someone else will handle it
                                world.setBlockState(new BlockPos(x + cubeX * 16, y + cubeY * 16, z + cubeZ * 16), ASPHALT);

                                //clear the above blocks (to a point, we don't want to be here all day)
                                IBlockState defState = Blocks.AIR.getDefaultState();
                                for (int ay = y + 1; ay < 16 * 2 && world.getBlockState(new BlockPos(x + cubeX * 16, ay + cubeY * 16, z + cubeZ * 16)) != defState; ay++) {
                                    world.setBlockState(new BlockPos(x + cubeX * 16, ay + cubeY * 16, z + cubeZ * 16), defState);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int quickElev(ICube cube, int x, int z) {

        if(x<0 || x>=16 || z<0 || z>=16)
            return 16-1;

        int low = 0;
        int high = 16;

        IBlockState defState = Blocks.AIR.getDefaultState();

        while(low < high-1) {
            int y = low + (high - low) / 2;
            if(cube.getBlockState(new BlockPos(x, y, z))==defState)
                high = y;
            else low = y;
        }

        return low;
    }

    private static double bound(double x, double slope, double j, double k, double r, double x0, double b, double sign) {
        double slopesign = sign*(slope<0?-1:1);

        if(x < j - slopesign*x0) { //left circle
            return slope*j + sign*Math.sqrt(r*r-(x-j)*(x-j));
        }
        if(x > k - slopesign*x0) { //right circle
            return slope*k + sign*Math.sqrt(r*r-(x-k)*(x-k));
        }
        return slope*x + sign*b;
    }
}