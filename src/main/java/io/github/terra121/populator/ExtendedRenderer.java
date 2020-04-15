package io.github.terra121.populator;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.terra121.EarthGeneratorSettings;
import io.github.terra121.TerraMod;
import io.github.terra121.dataset.OpenStreetMaps;
import io.github.terra121.dataset.Pathway;
import io.github.terra121.projection.GeographicProjection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 *  This class contains the methods and subclasses necessary to make tunnels possible.
 *  In essence, it allows structures to be generated and stored in memory until their respective chunks are loaded
 *  into the game. Think of it as preloading the structures.
 *
 * */
public class ExtendedRenderer extends Thread {


    public EarthGeneratorSettings cfg;
    int extendedGen;
    int chunkCount;
    CubePos local;
    OpenStreetMaps osm;
    GeographicProjection projection;

    public ExtendedRenderer(World world, GeographicProjection projection, CubePos local) {

        this.cfg = new EarthGeneratorSettings(world.getWorldInfo().getGeneratorOptions());
        this.chunkCount = cfg.settings.extendedRender;
        if (chunkCount > 2048) {
            chunkCount = 2048;
            TerraMod.LOGGER.warn("Extended Chunk Draw was more than 2048, setting to 2048.");
        }
        this.extendedGen = chunkCount * chunkCount;
        this.local = local;
        this.projection = projection;
        this.osm = new OpenStreetMaps(projection, true, false, false);
    }

    static class renderFar implements Runnable {

        static OpenStreetMaps osm;
        CubePos localCube;
        int start;
        int stop;

        public renderFar(OpenStreetMaps osm, CubePos localCube, int start, int stop) {
            renderFar.osm = osm;
            this.localCube = localCube;
            this.start = start;
            this.stop = stop;
        }

        /**
         * Pre-load roads into memory. Returns all roads within the given limit.
         * This can obviously impact performance. The worlds longest road tunnel is no longer than
         * about 25 kilometers, so no more than 25000/16=1562 is necessary (could also be rounded to next 2 exponent, which is 2048).<br>
         * The theoretical upper limit of chunkCount is a bit less than 1.1M Kilometers, but that's unrealistic.
         * */

        @Override
        public void run() {

            Set<Pathway.ChunkWithStructures> osmStrc = new HashSet<>();

            // google is a weird place. i wanted to find the greatest height difference between two ends of a tunnel in the world,
            // but for some reason "greatest height difference of married couple" came up :P
            Vec3d local = new Vec3d(localCube.getX(), localCube.getY(), localCube.getZ());

            int x = (int) local.x;
            int z = (int) local.z;
            int y = (int) local.y;

            int sqr = start; // the dimension of the spiral square
            int i = 1;
            while (i <= stop) {
                int e = 1;
                while (e <= sqr) {
                    // counterclockwise rotation, south -> east -> north -> west
                    z--;
                    Pathway.ChunkWithStructures st = new Pathway.ChunkWithStructures(new CubePos(x, y, z), osm.chunkStructures(x, z));
                    if (st.structures!=null) osmStrc.add(st);
                    e++;
                }
                e = 1;
                while (e <= sqr) {
                    x++;
                    Pathway.ChunkWithStructures st = new Pathway.ChunkWithStructures(new CubePos(x, y, z), osm.chunkStructures(x, z));
                    if (st.structures!=null) osmStrc.add(st);
                    e++;
                }
                e = 1;
                while (e <= sqr) {
                    z++;
                    Pathway.ChunkWithStructures st = new Pathway.ChunkWithStructures(new CubePos(x, y, z), osm.chunkStructures(x, z));
                    if (st.structures!=null) osmStrc.add(st);
                    e++;
                }
                e = 1;
                while (e <= sqr) {
                    x--;
                    Pathway.ChunkWithStructures st = new Pathway.ChunkWithStructures(new CubePos(x, y, z), osm.chunkStructures(x, z));
                    if (st.structures!=null) osmStrc.add(st);
                    e++;
                }
                sqr += 2;
                i++;
            }

            for (Pathway.ChunkWithStructures c : osmStrc) {
                for (OpenStreetMaps.Edge e : c.structures) {
                    if (e.type != OpenStreetMaps.Type.RIVER && e.type != OpenStreetMaps.Type.BUILDING && e.type != OpenStreetMaps.Type.STREAM) {
                        osmStructures.add(c);
                    }
                }
            }

            List<Pathway.ChunkWithStructures> osmStrcL = new ArrayList<>(osmStrc);
            TerraMod.LOGGER.info("Pre-generation of chunks (counterclockwise rotation) {} to {} finished successfully.",
                    osmStrcL.get(0).chunk, osmStrcL.get(osmStrcL.size() - 1).chunk);

        }
    }

    public static Set<Pathway.ChunkWithStructures> osmStructures = new HashSet<>();

    public void run() {

        // this is arbitrary
        int threadCount = 4;
        if (extendedGen > 512 && extendedGen < 1024) {
            threadCount = 8;
        } else if (extendedGen > 1024) {
            threadCount = 16;
        }

        renderFar e = new renderFar(osm, local, 0, extendedGen);
        Thread thread = new Thread(e);
        thread.start();

        /*int blocksPerThread = extendedGen / threadCount;
        int dis = extendedGen / blocksPerThread;
        int stspDis = chunkCount / threadCount;
        int lastStep = stspDis;
        for (int i = 0; i <= threadCount; i++) {

            ExtendedRenderer e = new ExtendedRenderer(osm, local, lastStep + 1, dis);
            Thread thread = new Thread(e);
            lastStep += stspDis;
            thread.start();

        }*/
    }
}

