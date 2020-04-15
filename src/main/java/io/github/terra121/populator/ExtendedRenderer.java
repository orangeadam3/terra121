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
 * Preload roads out to set distance. This is a quick implementation of tunnel gen but it should work for most tunnels.
 * I do however plan to replace this with something more elegant/resource conscious.
 * I know it doesn't actually <i>render</i> anything, but I couldn't think of a better name.
 */
public class ExtendedRenderer extends Thread {

    public EarthGeneratorSettings cfg;
    int extendedGen;
    int chunkCount;
    CubePos local;
    OpenStreetMaps osm;
    GeographicProjection projection;
    public static List<CubePos> chunks = new ArrayList<>();

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

    static class Renderer implements Runnable {

        int quad;
        static OpenStreetMaps osm;
        CubePos localCube;
        int start;
        int dist;

        public Renderer(OpenStreetMaps osm, CubePos localCube, int start, int dist, int quad) {
            Renderer.osm = osm;
            this.localCube = localCube;
            this.start = start;
            this.dist = dist;
            this.quad = quad;
        }

        /**
         * Pre-load roads into memory. Returns all roads within the given limit.
         * This can obviously impact performance. The worlds longest road tunnel is no longer than
         * about 25 kilometers, so no more than 25000/16=1562 is necessary (could also be rounded to next 2 exponent, which is 2048).<br>
         * The theoretical upper limit is a bit less than 1.1M Kilometers, but that's unrealistic.
         * I use multithreading to speed it up and for maximum CPU utilization.
         */
        @Override
        public void run() {

            ExtendedRenderManager.covered = new ArrayList<>();

            Set<Pathway.ChunkWithStructures> osmStrc = new HashSet<>();

            // google is a weird place. i wanted to find the greatest height difference between two ends of a tunnel in the world,
            // but for some reason "greatest height difference of married couple" came up :P
            Vec3d local = new Vec3d(localCube.getX(), localCube.getY(), localCube.getZ());

            int x = (int) local.x;
            int z = (int) local.z;
            int y = (int) local.y;

            // offset begin of processing
            switch (quad) {
                case 1:
                    x -= dist;
                    z -= dist;
                case 2:
                    x += dist;
                    z -= dist;
                case 3:
                    x -= dist;
                    z += dist;
                case 4:
                    x += dist;
                    z += dist;
            }

            int sqr = start; // the dimension of the spiral square
            int i = 1;
            while (i <= dist) {
                int e = 0;
                while (e <= sqr) {
                    // counterclockwise rotation, south -> east -> north -> west
                    z--;
                    Pathway.ChunkWithStructures st = new Pathway.ChunkWithStructures(new CubePos(x, y, z), osm.chunkStructures(x, z));
                    if (st.structures != null) osmStrc.add(st);
                    chunks.add(new CubePos(x, y, z));
                    e++;
                }
                e = 1;
                while (e <= sqr) {
                    x++;
                    Pathway.ChunkWithStructures st = new Pathway.ChunkWithStructures(new CubePos(x, y, z), osm.chunkStructures(x, z));
                    if (st.structures != null) osmStrc.add(st);
                    chunks.add(new CubePos(x, y, z));
                    e++;
                }
                e = 1;
                while (e <= sqr) {
                    z++;
                    Pathway.ChunkWithStructures st = new Pathway.ChunkWithStructures(new CubePos(x, y, z), osm.chunkStructures(x, z));
                    if (st.structures != null) osmStrc.add(st);
                    chunks.add(new CubePos(x, y, z));
                    e++;
                }
                e = 1;
                while (e <= sqr) {
                    x--;
                    Pathway.ChunkWithStructures st = new Pathway.ChunkWithStructures(new CubePos(x, y, z), osm.chunkStructures(x, z));
                    if (st.structures != null) osmStrc.add(st);
                    chunks.add(new CubePos(x, y, z));
                    e++;
                }
                sqr -= 2;
                i++;
            }

            for (Pathway.ChunkWithStructures c : osmStrc) {
                for (OpenStreetMaps.Edge e : c.structures) {
                    if (e.type != OpenStreetMaps.Type.RIVER && e.type != OpenStreetMaps.Type.BUILDING && e.type != OpenStreetMaps.Type.STREAM) {
                        osmStructures.add(c);
                    }
                }
            }

        }
    }

    public static Set<Pathway.ChunkWithStructures> osmStructures = new HashSet<>();

    public void run() {
        int dist = chunkCount / 4;

        // using 4 threads is arbitrary
        for (int i = 1; i <= 4; i++) {

            Renderer e = new Renderer(osm, local, 0, dist, i);
            Thread thread = new Thread(e);
            thread.start();
            TerraMod.LOGGER.info("Started thread {} of {} (thread id {})", i, 4, thread.getId());

        }

        ExtendedRenderManager.covered = chunks;

    }
}

