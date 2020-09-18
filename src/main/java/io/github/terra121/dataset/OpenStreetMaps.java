package io.github.terra121.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.terra121.TerraConfig;
import io.github.terra121.TerraMod;
import io.github.terra121.projection.GeographicProjection;

public class OpenStreetMaps {

    private static final double CHUNK_SIZE = 16;
    public static final double TILE_SIZE = 1 / 60.0;//250*(360.0/40075000.0);
    private static final double NOTHING = 0.01;

    private static final String OVERPASS_INSTANCE = "https://overpass-api.de";//"https://overpass.kumi.systems";
    private static final String URL_PREFACE = TerraConfig.serverOverpass + "/api/interpreter?data=[out:json];way(";
    private String URL_A = ")";
    private static final String URL_B = "%20tags%20qt;(._<;);out%20body%20qt;";
    private static final String URL_C = "is_in(";
    private String URL_SUFFIX = ");area._[~\"natural|waterway\"~\"water|riverbank\"];out%20ids;";

    private HashMap<Coord, Set<Edge>> chunks;
    private HashMap<Coord, Set<Building>> chunksBuildings;
    public LinkedHashMap<Coord, Region> regions;
    public Water water;

    private int numcache = TerraConfig.osmCacheSize;

    private ArrayList<Edge> allEdges;
    private Set<Building> allBuildings;

    private Gson gson;

    private GeographicProjection projection;

    public static enum Type {
        IGNORE, ROAD, MINOR, SIDE, MAIN, INTERCHANGE, LIMITEDACCESS, FREEWAY, STREAM, RIVER, BUILDING, RAIL
        // ranges from minor to freeway for roads, use road if not known
    }

    public static enum BuildingGenerationType {
        NONE, OUTLINES, SHELLS
    }

    public static enum Attributes {
        ISBRIDGE, ISTUNNEL, NONE
    }

    public static class noneBoolAttributes {
        public static String layer;
    }

    Type wayType;
    byte wayLanes;

    boolean doRoad;
    boolean doWater;
    BuildingGenerationType buildingGenerationType;

    public OpenStreetMaps(GeographicProjection proj, boolean doRoad, boolean doWater, BuildingGenerationType buildingGenerationType) {
        gson = new GsonBuilder().create();
        chunks = new LinkedHashMap<>();
        chunksBuildings = new LinkedHashMap<>();
        allEdges = new ArrayList<Edge>();
        allBuildings = new HashSet<>();
        regions = new LinkedHashMap<Coord, Region>();
        projection = proj;
        try {
            water = new Water(this, 256);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.doRoad = doRoad;
        this.doWater = doWater;
        this.buildingGenerationType = buildingGenerationType;

        if (buildingGenerationType == BuildingGenerationType.NONE) URL_A += "[!\"building\"]";
        if (!doRoad) URL_A += "[!\"highway\"]";
        if (!doWater) URL_A += "[!\"water\"][!\"natural\"][!\"waterway\"]";
        URL_A += ";out%20geom";
    }

    public Coord getRegion(double lon, double lat) {
        return new Coord((int) Math.floor(lon / TILE_SIZE), (int) Math.floor(lat / TILE_SIZE));
    }

    public Set<Edge> chunkStructures(int x, int z) {
        Coord coord = new Coord(x, z);

        if (regionCache(projection.toGeo(x * CHUNK_SIZE, z * CHUNK_SIZE)) == null)
            return null;

        if (regionCache(projection.toGeo((x + 1) * CHUNK_SIZE, z * CHUNK_SIZE)) == null)
            return null;

        if (regionCache(projection.toGeo((x + 1) * CHUNK_SIZE, (z + 1) * CHUNK_SIZE)) == null)
            return null;

        if (regionCache(projection.toGeo(x * CHUNK_SIZE, (z + 1) * CHUNK_SIZE)) == null)
            return null;

        return chunks.get(coord);
    }

    public Set<Building> chunkBuildings(int x, int z) {
        Coord coord = new Coord(x, z);

        if (regionCache(projection.toGeo(x * CHUNK_SIZE, z * CHUNK_SIZE)) == null)
            return null;

        if (regionCache(projection.toGeo((x + 1) * CHUNK_SIZE, z * CHUNK_SIZE)) == null)
            return null;

        if (regionCache(projection.toGeo((x + 1) * CHUNK_SIZE, (z + 1) * CHUNK_SIZE)) == null)
            return null;

        if (regionCache(projection.toGeo(x * CHUNK_SIZE, (z + 1) * CHUNK_SIZE)) == null)
            return null;

        return chunksBuildings.get(coord);
    }

    public Region regionCache(double[] corner) {

        //bound check
        if(!(corner[0]>=-180 && corner[0]<=180 && corner[1]>=-80 && corner[1]<=80))
            return null;

        Coord coord = getRegion(corner[0], corner[1]);
        Region region;

        if ((region = regions.get(coord)) == null) {
            region = new Region(coord, water);
            int i;
            for (i = 0; i < 5 && !regiondownload(region); i++) ;
            regions.put(coord, region);
            if (regions.size() > numcache) {
                //TODO: delete beter
                Iterator<Region> it = regions.values().iterator();
                Region delete = it.next();
                it.remove();
                removeRegion(delete);
            }

            if (i == 5) {
                region.failedDownload = true;
                TerraMod.LOGGER.error("OSM region" + region.coord.x + " " + region.coord.y + " failed to download several times, no structures will spawn");
                return null;
            }
        } else if (region.failedDownload) return null; //don't return dummy regions
        return region;
    }

    public boolean regiondownload(Region region) {
        double X = region.coord.x * TILE_SIZE;
        double Y = region.coord.y * TILE_SIZE;

        //limit extreme (a.k.a. way too clustered on some projections) requests and out of bounds requests
        if (Y > 80 || Y < -80 || X < -180 || X > 180 - TILE_SIZE) {
            region.failedDownload = true;
            return false;
        }

        try {
            String bottomleft = Y + "," + X;
            String bbox = bottomleft + "," + (Y + TILE_SIZE) + "," + (X + TILE_SIZE);

            String urltext = URL_PREFACE + bbox + URL_A /*+ bbox */+ URL_B;
            if (doWater) urltext += URL_C + bottomleft + URL_SUFFIX;

            TerraMod.LOGGER.info(urltext);

            //kumi systems request a meaningful user-agent
            URL url = new URL(urltext);
            URLConnection c = url.openConnection();
            c.addRequestProperty("User-Agent", TerraMod.USERAGENT);
            InputStream is = c.getInputStream();

            doGson(is, region);

            is.close();

        } catch (Exception e) {
            TerraMod.LOGGER.error("Osm region download failed, no osm features will spawn, " + e);
            e.printStackTrace();
            return false;
        }

        double[] ll = projection.fromGeo(X, Y);
        double[] lr = projection.fromGeo(X + TILE_SIZE, Y);
        double[] ur = projection.fromGeo(X + TILE_SIZE, Y + TILE_SIZE);
        double[] ul = projection.fromGeo(X, Y + TILE_SIZE);

        //estimate bounds of region in terms of chunks
        int lowX = (int) Math.floor(Math.min(Math.min(ll[0], ul[0]), Math.min(lr[0], ur[0])) / CHUNK_SIZE);
        int highX = (int) Math.ceil(Math.max(Math.max(ll[0], ul[0]), Math.max(lr[0], ur[0])) / CHUNK_SIZE);
        int lowZ = (int) Math.floor(Math.min(Math.min(ll[1], ul[1]), Math.min(lr[1], ur[1])) / CHUNK_SIZE);
        int highZ = (int) Math.ceil(Math.max(Math.max(ll[1], ul[1]), Math.max(lr[1], ur[1])) / CHUNK_SIZE);

        for (Edge e : allEdges)
            relevantChunks(lowX, lowZ, highX, highZ, e);
        allEdges.clear();

        for (Building b : allBuildings)
            relevantChunks(b);
        allBuildings.clear();

        return true;
    }

    private void doGson(InputStream is, Region region) throws IOException {

        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, StandardCharsets.UTF_8);
        String str = writer.toString();

        Data data = gson.fromJson(str.toString(), Data.class);

        Map<Long, Element> allWays = new HashMap<Long, Element>();
        Set<Element> unusedWays = new HashSet<Element>();
        Set<Long> ground = new HashSet<Long>();

        for (Element elem : data.elements) {
            Attributes attributes = Attributes.NONE;
            if (elem.type == EType.way) {
                allWays.put(elem.id, elem);

                if (elem.tags == null) {
                    unusedWays.add(elem);
                    continue;
                }

                String naturalv = null, highway = null, waterway = null, building = null, istunnel = null, isbridge = null;

                if (doWater) {
                    naturalv = elem.tags.get("natural");
                    waterway = elem.tags.get("waterway");
                }

                if (doRoad) {
                    highway = elem.tags.get("highway");
                    istunnel = elem.tags.get("tunnel");
                    // to be implemented
                    isbridge = elem.tags.get("bridge");
                }

                if (buildingGenerationType != BuildingGenerationType.NONE) {
                    building = elem.tags.get("building");
                }

                if (naturalv != null && naturalv.equals("coastline")) {
                    waterway(elem, -1, region, null);
                } else if (building != null && buildingGenerationType == BuildingGenerationType.SHELLS) {
                    allBuildings.add(new Building(elem, allWays).projectFromGeo(projection));
                } else if (highway != null || (waterway != null && (waterway.equals("river") ||
                        waterway.equals("canal") || waterway.equals("stream"))) || building != null) { //TODO: fewer equals

                    Type type = Type.ROAD;

                    if (waterway != null) {
                        type = Type.STREAM;
                        if (waterway.equals("river") || waterway.equals("canal"))
                            type = Type.RIVER;

                    }

                    if (building != null) type = Type.BUILDING;

                    if (istunnel != null && istunnel.equals("yes")) {

                        attributes = Attributes.ISTUNNEL;

                    } else if (isbridge != null && isbridge.equals("yes")) {

                        attributes = Attributes.ISBRIDGE;

                    } else {

                        // totally skip classification if it's a tunnel or bridge. this should make it more efficient.
                        if (highway != null && attributes == Attributes.NONE) {
                            switch (highway) {
                                case "motorway":
                                    type = Type.FREEWAY;
                                    break;
                                case "trunk":
                                    type = Type.LIMITEDACCESS;
                                    break;
                                case "motorway_link":
                                case "trunk_link":
                                    type = Type.INTERCHANGE;
                                    break;
                                case "secondary":
                                    type = Type.SIDE;
                                    break;
                                case "primary":
                                case "raceway":
                                    type = Type.MAIN;
                                    break;
                                case "tertiary":
                                case "residential":
                                    type = Type.MINOR;
                                    break;
                                default:
                                    if (highway.equals("primary_link") ||
                                            highway.equals("secondary_link") ||
                                            highway.equals("living_street") ||
                                            highway.equals("bus_guideway") ||
                                            highway.equals("service") ||
                                            highway.equals("unclassified"))
                                        type = Type.SIDE;
                                    break;
                            }
                        }
                    }
                    //get lane number (default is 2)
                    String slanes = elem.tags.get("lanes");
                    String slayer = elem.tags.get("layers");
                    byte lanes = 2;
                    byte layer = 1;

                    if (slayer != null) {

                        try {

                            layer = Byte.parseByte(slayer);

                        } catch (NumberFormatException e) {

                            // default to layer 1 if bad format

                        }

                    }

                    if (slanes != null) {

                        try {

                            lanes = Byte.parseByte(slanes);

                        } catch (NumberFormatException e) {

                        } //default to 2, if bad format
                    }

                    //prevent super high # of lanes to prevent ridiculous results (prly a mistake if its this high anyways)
                    if (lanes > 8)
                        lanes = 8;

                    // an interchange that doesn't have any lane tag should be defaulted to 2 lanes
                    if (lanes < 2 && type == Type.INTERCHANGE) {
                        lanes = 2;
                    }

                    // upgrade road type if many lanes (and the road was important enough to include a lanes tag)
                    if (lanes > 2 && type == Type.MINOR)
                        type = Type.MAIN;

                    addWay(elem, type, lanes, region, attributes, layer);
                } else unusedWays.add(elem);
            } else if (elem.type == EType.relation && elem.members != null && elem.tags != null) {

                if(doWater) {
                    String naturalv = elem.tags.get("natural");
                    String waterv = elem.tags.get("water");
                    String wway = elem.tags.get("waterway");

                    if (waterv != null || (naturalv != null && naturalv.equals("water")) || (wway != null && wway.equals("riverbank"))) {
                        for (Member member : elem.members) {
                            if (member.type == EType.way) {
                                Element way = allWays.get(member.ref);
                                if (way != null) {
                                    waterway(way, elem.id + 3600000000L, region, null);
                                    unusedWays.remove(way);
                                }
                            }
                        }
                        continue;
                    }
                }
                if(buildingGenerationType == BuildingGenerationType.OUTLINES && elem.tags.get("building") != null) {
                    for (Member member : elem.members) {
                        if (member.type == EType.way) {
                            Element way = allWays.get(member.ref);
                            if (way != null) {
                                addWay(way, Type.BUILDING, (byte) 1, region, Attributes.NONE, (byte) 0);
                                unusedWays.remove(way);
                            }
                        }
                    }
                }
                if(buildingGenerationType == BuildingGenerationType.SHELLS && elem.tags.get("building") != null) {
                    allBuildings.add(new Building(elem, allWays).projectFromGeo(projection));
                }

            } else if (elem.type == EType.area) {
                ground.add(elem.id);
            }
        }

        if (doWater) {

            for (Element way : unusedWays) {
                if (way.tags != null) {
                    String naturalv = way.tags.get("natural");
                    String waterv = way.tags.get("water");
                    String wway = way.tags.get("waterway");

                    if (waterv != null || (naturalv != null && naturalv.equals("water")) || (wway != null && wway.equals("riverbank")))
                        waterway(way, way.id + 2400000000L, region, null);
                }
            }

            if (water.grounding.state(region.coord.x, region.coord.y) == 0) {
                ground.add(-1L);
            }

            region.renderWater(ground);
        }
    }

    void addWay(Element elem, Type type, byte lanes, Region region, Attributes attributes, byte layer) {
        double[] lastProj = null;
        if(elem.geometry != null)
        for (Geometry geom : elem.geometry) {
            if (geom == null) lastProj = null;
            else {
                double[] proj = projection.fromGeo(geom.lon, geom.lat);

                if (lastProj != null) { //register as a road edge
                    allEdges.add(new Edge(lastProj[0], lastProj[1], proj[0], proj[1], type, lanes, region, attributes, layer));
                }

                lastProj = proj;
            }
        }
    }

    Geometry waterway(Element way, long id, Region region, Geometry last) {
        if (way.geometry != null)
            for (Geometry geom : way.geometry) {
                if (geom != null && last != null) {
                    region.addWaterEdge(last.lon, last.lat, geom.lon, geom.lat, id);
                }
                last = geom;
            }

        return last;
    }

    private void relevantChunks(int lowX, int lowZ, int highX, int highZ, Edge edge) {
        Coord start = new Coord((int) Math.floor(edge.slon / CHUNK_SIZE), (int) Math.floor(edge.slat / CHUNK_SIZE));
        Coord end = new Coord((int) Math.floor(edge.elon / CHUNK_SIZE), (int) Math.floor(edge.elat / CHUNK_SIZE));

        double startx = edge.slon;
        double endx = edge.elon;

        if (startx > endx) {
            Coord tmp = start;
            start = end;
            end = tmp;
            startx = endx;
            endx = edge.slon;
        }

        highX = Math.min(highX, end.x + 1);
        for (int x = Math.max(lowX, start.x); x < highX; x++) {
            double X = x * CHUNK_SIZE;
            int from = (int) Math.floor((edge.slope * Math.max(X, startx) + edge.offset) / CHUNK_SIZE);
            int to = (int) Math.floor((edge.slope * Math.min(X + CHUNK_SIZE, endx) + edge.offset) / CHUNK_SIZE);

            if (from > to) {
                int tmp = from;
                from = to;
                to = tmp;
            }

            for (int y = Math.max(from, lowZ); y <= to && y < highZ; y++) {
                assoiateWithChunk(new Coord(x, y), edge);
            }
        }
    }

    private void relevantChunks(Building building) {
        int lowX = (int)Math.floor(building.minX() / CHUNK_SIZE);
        int lowZ = (int)Math.floor(building.minZ() / CHUNK_SIZE);
        int highX = (int)Math.ceil(building.maxX() / CHUNK_SIZE);
        int highZ = (int)Math.ceil(building.maxZ() / CHUNK_SIZE);
//        System.out.println("relevantChunks(" + building + ") -> l: " + lowX + ", " + lowZ + "; h: " + highX + ", " + highZ);
        // There's probably an error if this one building is relevant to 100+ chunks.
//        if ((highX - lowX) * (highZ - lowZ) > 100) return;
        for (int x = lowX; x < highX; x++) {
            for (int z = lowZ; z < highZ; z++) {
                assoiateWithChunk(new Coord(x, z), building);
            }
        }
    }

    private void assoiateWithChunk(Coord c, Building building) {
        Set<Building> list = chunksBuildings.computeIfAbsent(c, k -> new HashSet<Building>());
        list.add(building);
    }

    private void assoiateWithChunk(Coord c, Edge edge) {
        Set<Edge> list = chunks.computeIfAbsent(c, k -> new HashSet<Edge>());
        list.add(edge);
    }

    //TODO: this algorithm is untested and may have some memory leak issues and also strait up copies code from earlier
    private void removeRegion(Region delete) {
        double X = delete.coord.x * TILE_SIZE;
        double Y = delete.coord.y * TILE_SIZE;

        double[] ll = projection.fromGeo(X, Y);
        double[] lr = projection.fromGeo(X + TILE_SIZE, Y);
        double[] ur = projection.fromGeo(X + TILE_SIZE, Y + TILE_SIZE);
        double[] ul = projection.fromGeo(X, Y + TILE_SIZE);

        //estimate bounds of region in terms of chunks
        int lowX = (int) Math.floor(Math.min(Math.min(ll[0], ul[0]), Math.min(lr[0], ur[0])) / CHUNK_SIZE);
        int highX = (int) Math.ceil(Math.max(Math.max(ll[0], ul[0]), Math.max(lr[0], ur[0])) / CHUNK_SIZE);
        int lowZ = (int) Math.floor(Math.min(Math.min(ll[1], ul[1]), Math.min(lr[1], ur[1])) / CHUNK_SIZE);
        int highZ = (int) Math.ceil(Math.max(Math.max(ll[1], ul[1]), Math.max(lr[1], ur[1])) / CHUNK_SIZE);

        for (int x = lowX; x < highX; x++) {
            for (int z = lowZ; z < highZ; z++) {
                Set<Edge> edges = chunks.get(new Coord(x, z));
                if (edges != null) {
                    Iterator<Edge> it = edges.iterator();
                    while (it.hasNext())
                        if (it.next().region.equals(delete))
                            it.remove();

                    if (edges.size() <= 0)
                        chunks.remove(new Coord(x, z));
                }
            }
        }
    }

    //integer coordinate class
    public static class Coord {
        public int x;
        public int y;

        private Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int hashCode() {
            return (x * 79399) + (y * 100000);
        }

        public boolean equals(Object o) {
            Coord c = (Coord) o;
            return c.x == x && c.y == y;
        }

        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    public static class Edge {
        public Type type;
        public double slat;
        public double slon;
        public double elat;
        public double elon;
        public Attributes attribute;
        public byte layer_number;
        public double slope;
        public double offset;

        public byte lanes;

        Region region;

        private double squareLength() {
            double dlat = elat - slat;
            double dlon = elon - slon;
            return dlat * dlat + dlon * dlon;
        }

        private Edge(double slon, double slat, double elon, double elat, Type type, byte lanes, Region region, Attributes att, byte ly) {
            //slope must not be infinity, slight inaccuracy shouldn't even be noticible unless you go looking for it
            double dif = elon - slon;
            if (-NOTHING <= dif && dif <= NOTHING) {
                if (dif < 0) {
                    elon -= NOTHING;
                } else {
                    elon += NOTHING;
                }
            }

            this.slat = slat;
            this.slon = slon;
            this.elat = elat;
            this.elon = elon;
            this.type = type;
            this.attribute = att;
            this.lanes = lanes;
            this.region = region;
            this.layer_number = ly;

            slope = (elat - slat) / (elon - slon);
            offset = slat - slope * slon;
        }

        public int hashCode() {
            return (int) ((slon * 79399) + (slat * 100000) + (elat * 13467) + (elon * 103466));
        }

        public boolean equals(Object o) {
            Edge e = (Edge) o;
            return e.slat == slat && e.slon == slon && e.elat == elat && e.elon == e.elon;
        }

        public String toString() {
            return "(" + slat + ", " + slon + "," + elat + "," + elon + ")";
        }
    }

    public static enum EType {
        invalid, node, way, relation, area
    }

    public static class Member {
        EType type;
        long ref;
        String role;

        Element element;
    }

    public static class Geometry {
        public double lat;
        public double lon;

        public Geometry(){}

        public Geometry(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static class Element {
        public EType type;
        public long id;
        public Map<String, String> tags;
        public long[] nodes;
        public Member[] members;
        public Geometry[] geometry;

        @Override
        public String toString() {
            return type + "{" +
                    "id=" + id +
                    ", tags=" + tags +
                    ", geometry=" + Arrays.toString(geometry) +
                    '}';
        }
    }

    public static class Data {
        float version;
        String generator;
        Map<String, String> osm3s;
        List<Element> elements;
    }

    public static void main(String[] args) {
    }
}