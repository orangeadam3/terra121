package io.github.terra121.dataset;

import java.util.*;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.github.terra121.projection.GeographicProjection;
import io.github.terra121.projection.InvertedGeographic;
import io.github.terra121.TerraMod;

public class OpenStreetMaps {

    private static final double BLOCK_SIZE = 1/100000.0;
    private static final double CHUNK_SIZE = 16*BLOCK_SIZE;
    public static final double TILE_SIZE = 1/60.0;//250*(360.0/40075000.0);
    private static final double NOTHING = 0.1*BLOCK_SIZE;
    
    private static final String OVERPASS_INSTANCE = "https://overpass-api.de";//"https://overpass.kumi.systems";
    private static final String URL_PREFACE = OVERPASS_INSTANCE+"/api/interpreter?data=[out:json];way(";
    private static final String URL_A = ")[!\"building\"];out%20geom(";
    private static final String URL_B = ")%20tags%20qt;(._<;);out%20body%20qt;is_in(";
    private static final String URL_SUFFIX = ");area._[\"natural\"=\"water\"];out%20ids;";

    private HashMap<Coord, Set<Edge>> chunks;
    public LinkedHashMap<Coord, Region> regions;
    public Water water;

    private int numcache = 1000000;

    private ArrayList<Edge> allEdges;

    private Gson gson;
    
    private GeographicProjection projection;
    
    public static enum Type {
        IGNORE, MINOR, ROAD, MAJOR, HIGHWAY, RAIL //TODO, rail
    }

    Type wayType;
    byte wayLanes;

    public OpenStreetMaps (GeographicProjection proj) {
    	gson = new GsonBuilder().create();
        chunks = new LinkedHashMap<Coord, Set<Edge>>();
        allEdges = new ArrayList<Edge>();
        regions = new LinkedHashMap<Coord, Region>();
        projection = proj;
        try {
			water = new Water(this, 160);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public Coord getRegion(double lon, double lat) {
    	return new Coord((int)Math.floor(lon/TILE_SIZE), (int)Math.floor(lat/TILE_SIZE));
    }
    
    public Set<Edge> chunkStructures(int x, int z) {
        Coord coord = new Coord(x,z);
        
        if(regionCache(projection.toGeo(x*CHUNK_SIZE, z*CHUNK_SIZE))==null)
        	return null;
        
        if(regionCache(projection.toGeo((x+1)*CHUNK_SIZE, z*CHUNK_SIZE))==null)
        	return null;
        
        if(regionCache(projection.toGeo((x+1)*CHUNK_SIZE, (z+1)*CHUNK_SIZE))==null)
        	return null;
        
        if(regionCache(projection.toGeo(x*CHUNK_SIZE, (z+1)*CHUNK_SIZE))==null)
        	return null;
        
        return chunks.get(coord);
    }
    
    public Region regionCache(double[] corner) {
    	Coord coord = getRegion(corner[0], corner[1]);
		Region region;
    	
    	if((region = regions.get(coord)) == null) {
    		region = new Region(coord, water);
            int i;
            for (i = 0; i < 5 && !regiondownload(region); i++);
            regions.put(coord, region);
            if(regions.size() > numcache) {
            	//TODO: delete
                /*Iterator<Region> it = regions.entrySet().iterator();
                Region delete = it.next();
                it.remove();
                removeRegion(delete);*/
            }

            if(i==5) {
                TerraMod.LOGGER.error("OSM region" + region.coord.x + " " + region.coord.y + " failed to download several times, no structures will spawn");
                return null;
            }
        }
    	return region;
    }

    public boolean regiondownload (Region region) {
        double X = region.coord.x*TILE_SIZE;
        double Y = region.coord.y*TILE_SIZE;

        try {
        	String bottomleft = Y + "," + X;
        	String bbox = bottomleft + "," + (Y + TILE_SIZE) + "," + (X + TILE_SIZE);
        	
            String urltext = URL_PREFACE + bbox + URL_A + bbox + URL_B + bottomleft + URL_SUFFIX;
            TerraMod.LOGGER.info(urltext);

            URL url = new URL(urltext);
            InputStream is = url.openStream();

            doGson(is, region);
            
            is.close();

        } catch(Exception e) {
            TerraMod.LOGGER.error("Osm region download failed, no osm features will spawn, "+e);
            e.printStackTrace();
            return false;
        }

        double[] ll = projection.fromGeo(X, Y);
        double[] lr = projection.fromGeo(X + TILE_SIZE, Y);
        double[] ur = projection.fromGeo(X + TILE_SIZE, Y + TILE_SIZE);
        double[] ul = projection.fromGeo(X, Y + TILE_SIZE);
        
        //estimate bounds of region in terms of chunks
        int lowX = (int)Math.floor(Math.min(Math.min(ll[0], ul[0]), Math.min(lr[0], ur[0]))/CHUNK_SIZE);
        int highX = (int)Math.ceil(Math.max(Math.max(ll[0], ul[0]), Math.max(lr[0], ur[0]))/CHUNK_SIZE);
        int lowZ = (int)Math.floor(Math.min(Math.min(ll[1], ul[1]), Math.min(lr[1], ur[1]))/CHUNK_SIZE);
        int highZ = (int)Math.ceil(Math.max(Math.max(ll[1], ul[1]), Math.max(lr[1], ur[1]))/CHUNK_SIZE);
        
        for(Edge e: allEdges)
            relevantChunks(lowX, lowZ, highX, highZ, e);
        allEdges.clear();

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
        
    	for(Element elem: data.elements) {
    		if(elem.type==EType.way) {
    			allWays.put(elem.id, elem);
    			
    			if(elem.tags==null) {
    				unusedWays.add(elem);
    				continue;
    			}
    			
    			String naturalv = elem.tags.get("natural");
    			String highway = elem.tags.get("highway");
    			
    			if(naturalv != null && naturalv.equals("coastline"))
    				waterway(elem, -1, region, null);
    			
    			else if(highway!=null) {
    				Type type = Type.ROAD;

                    if(highway.equals("motorway") || highway.equals("trunk"))
                        type = Type.HIGHWAY;
                    else if(highway.equals("tertiary") || highway.equals("residential") || highway.equals("primary") || highway.equals("secondary") || highway.equals("raceway") || highway.equals("motorway_link") || highway.equals("trunk_link"))
                        type = Type.MAJOR;
                    else if(
                    		highway.equals("primary_link") || highway.equals("secondary_link") || highway.equals("living_street") || highway.equals("bus_guideway") || highway.equals("service") || highway.equals("unclassified"))
                        type = Type.MINOR;
    				
                    //get lane number (default is 2)
                    String slanes = elem.tags.get("lanes");
                    byte lanes = 2;
                    if(slanes != null) {
                    	try {
                    		lanes = Byte.parseByte(slanes);
                    	} catch(NumberFormatException e) { } //default to 2, if bad format
                    }
                    
                    //prevent super high # of lanes to prevent ridiculous results (prly a mistake if its this high anyways)
                    if(lanes>8)
                    	lanes = 8;
                    
                    //upgrade road type if many lanes (and the road was important enough to include a lanes tag)
                    if(lanes>2 && type==Type.MINOR)
                    	type = Type.MAJOR;
                    
                    double[] lastProj = null;
                    for(Geometry geom: elem.geometry) {
                    	if(geom==null) lastProj = null;
                    	else {
	                    	double[] proj = projection.fromGeo(geom.lon, geom.lat);
	                    	
	                    	if(lastProj!=null) { //register as a road edge
	                    		allEdges.add(new Edge(lastProj[0], lastProj[1], proj[0], proj[1], type, lanes, region));
	                    	}
	                    	
	                    	lastProj = proj;
                    	}
                    }
    			}
    			else unusedWays.add(elem);
    		}
    		else if(elem.type == EType.relation && elem.members!=null && elem.tags!=null) {
    			String naturalv = elem.tags.get("natural");
	    		String waterv = elem.tags.get("water");
	    			
	    		if(waterv!=null || (naturalv!=null && elem.tags.get("natural").equals("water"))) {
	    			for(Member member: elem.members) {
	    				if(member.type == EType.way) {
	    					Element way = allWays.get(member.ref);
	    					if(way != null) {
		    					waterway(way, elem.id+3600000000L, region, null);
		    					unusedWays.remove(way);
	    					}
	    				}
	    			}
	    		}
    		}
    		else if(elem.type == EType.area) {
    			ground.add(elem.id);
    		}
    	}
    	
    	for(Element way: unusedWays) {
    		if(way.tags!=null) {
    			String naturalv = way.tags.get("natural");
    			String waterv = way.tags.get("water");
    			
    			if(waterv != null || (naturalv!=null && naturalv.equals("water")))
    				waterway(way, way.id+2400000000L, region, null);
			}
    	}
    	
    	region.renderWater(ground);
    }
    
    Geometry waterway(Element way, long id, Region region, Geometry last) {
    	if(way.geometry!=null)
    	for(Geometry geom: way.geometry) {
        	if(geom!=null && last != null) {
            	region.addWaterEdge(last.lon, last.lat, geom.lon, geom.lat, id);
        	}
        	last = geom;
        }
    	
    	return last;
    }

    private void relevantChunks(int lowX, int lowZ, int highX, int highZ, Edge edge) {
        Coord start = new Coord((int)Math.floor(edge.slon/CHUNK_SIZE), (int)Math.floor(edge.slat/CHUNK_SIZE));
        Coord end = new Coord((int)Math.floor(edge.elon/CHUNK_SIZE), (int)Math.floor(edge.elat/CHUNK_SIZE));

        double startx = edge.slon;
        double endx = edge.elon;

        if(startx > endx) {
            Coord tmp = start;
            start = end;
            end = tmp;
            startx = endx;
            endx = edge.slon;
        }
        
        highX = Math.min(highX, end.x+1);
        for(int x=Math.max(lowX, start.x); x<highX; x++) {
            double X = x*CHUNK_SIZE;
            int from = (int)Math.floor((edge.slope*Math.max(X, startx) + edge.offset)/CHUNK_SIZE);
            int to = (int)Math.floor((edge.slope*Math.min(X+CHUNK_SIZE, endx) + edge.offset)/CHUNK_SIZE);

            if(from > to) {
                int tmp = from;
                from = to;
                to = tmp;
            }

            for(int y=Math.max(from, lowZ); y<=to && y<highZ; y++) {
                assoiateWithChunk(new Coord(x,y), edge);
            }
        }
    }

    private void assoiateWithChunk(Coord c, Edge edge) {
        Set<Edge> list = chunks.get(c);
        if(list == null) {
            list = new HashSet<Edge>();
            chunks.put(c, list);
        }
        list.add(edge);
    }

    //TODO: this algorithm is untested and may have some memory leak issues and also strait up copies code from earlier
    private void removeRegion(Region delete) {
    	
    	double X = delete.coord.x*TILE_SIZE;
        double Y = delete.coord.y*TILE_SIZE;
    	
    	double[] ll = projection.fromGeo(X, Y);
        double[] lr = projection.fromGeo(X + TILE_SIZE, Y);
        double[] ur = projection.fromGeo(X + TILE_SIZE, Y + TILE_SIZE);
        double[] ul = projection.fromGeo(X, Y + TILE_SIZE);
        
        //estimate bounds of region in terms of chunks
        int lowX = (int)Math.floor(Math.min(Math.min(ll[0], ul[0]), Math.min(lr[0], ur[0]))/CHUNK_SIZE);
        int highX = (int)Math.ceil(Math.max(Math.max(ll[0], ul[0]), Math.max(lr[0], ur[0]))/CHUNK_SIZE);
        int lowZ = (int)Math.floor(Math.min(Math.min(ll[1], ul[1]), Math.min(lr[1], ur[1]))/CHUNK_SIZE);
        int highZ = (int)Math.ceil(Math.max(Math.max(ll[1], ul[1]), Math.max(lr[1], ur[1]))/CHUNK_SIZE);
        
        for(int x=lowX; x<highX; x++) {
            for(int z=lowZ; z<highZ; z++) {
            	Set<Edge> edges = chunks.get(new Coord(x,z));
            	if(edges!=null) {
            		Iterator<Edge> it = edges.iterator();
            		while(it.hasNext())
            			if(it.next().region.equals(delete))
            				it.remove();
            		
            		if(edges.size() <= 0)
            			chunks.remove(new Coord(x,z));
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

        public double slope;
        public double offset;

        public byte lanes;
        
        Region region;

        private double squareLength() {
            double dlat = elat-slat;
            double dlon = elon-slon;
            return dlat*dlat + dlon*dlon;
        }

        private Edge(double slon, double slat, double elon, double elat, Type type, byte lanes, Region region) {
            //slope must not be infinity, slight inaccuracy shouldn't even be noticible unless you go looking for it
            double dif = elon-slon;
            if(-NOTHING <= dif && dif <= NOTHING) {
                if(dif<0) {
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
            this.lanes = lanes;
            this.region = region;

            slope = (elat-slat)/(elon-slon);
            offset = slat - slope*slon;
        }

        public int hashCode() {
            return (int)((slon * 79399) + (slat * 100000) + (elat * 13467) + (elon * 103466));
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
    }
    
    public static class Geometry {
    	double lat;
    	double lon;
    }
    
    public static class Element {
    	EType type;
    	long id;
    	Map<String,String> tags;
    	long[] nodes;
    	Member[] members;
    	Geometry[] geometry;
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