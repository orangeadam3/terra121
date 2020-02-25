package io.github.terra121.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.terra121.TerraMod;
import io.github.terra121.projection.GeographicProjection;

public class OpenStreetMaps {

    private static final double CHUNK_SIZE = 16;
    private static final double TILE_SIZE = 1/60.0;//250*(360.0/40075000.0);
    private static final double NOTHING = 0.01;
    
    private static final String OVERPASS_INSTANCE = "overpass-api.de";
    private static final String URL_PREFACE = "https://"+OVERPASS_INSTANCE+"/api/interpreter?data=[out:json];way(";
    private static final String URL_SUFFIX = ")[%22highway%22];(._;%3E;);out%20body;";

    private HashMap<Coord, Set<Edge>> chunks;
    private LinkedHashSet<Coord> regions;

    private int numcache = 1000000;

    private Map<Long, Element> allNodes;
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
    	allNodes =  new HashMap<Long, Element>();
        chunks = new LinkedHashMap<Coord, Set<Edge>>();
        allEdges = new ArrayList<Edge>();
        regions = new LinkedHashSet<Coord>();
        projection = proj;
    }

    private Coord getRegion(double lon, double lat) {
    	return new Coord((int)Math.floor(lon/TILE_SIZE), (int)Math.floor(lat/TILE_SIZE));
    }
    
    public Set<Edge> chunkStructures(int x, int z) {
        Coord coord = new Coord(x,z);
        
        if(!regionCache(projection.toGeo(x*CHUNK_SIZE, z*CHUNK_SIZE)))
        	return null;
        
        if(!regionCache(projection.toGeo((x+1)*CHUNK_SIZE, z*CHUNK_SIZE)))
        	return null;
        
        if(!regionCache(projection.toGeo((x+1)*CHUNK_SIZE, (z+1)*CHUNK_SIZE)))
        	return null;
        
        if(!regionCache(projection.toGeo(x*CHUNK_SIZE, (z+1)*CHUNK_SIZE)))
        	return null;
        
        return chunks.get(coord);
    }
    
    private boolean regionCache(double[] corner) {
    	Coord region = getRegion(corner[0], corner[1]);
    	
    	if(!regions.contains(region)) {
            int i;
            for (i = 0; i < 5 && !regiondownload(region); i++);
            regions.add(region);
            if(regions.size() > numcache) {
                Iterator<Coord> it = regions.iterator();
                Coord delete = it.next();
                it.remove();
                removeRegion(delete);
            }

            if(i==5) {
                TerraMod.LOGGER.error("OSM region" + region.x + " " + region.y + " failed to download several times, no structures will spawn");
                return false;
            }
        }
    	return true;
    }

    public boolean regiondownload (Coord mchunk) {
        double X = mchunk.x*TILE_SIZE;
        double Y = mchunk.y*TILE_SIZE;

        try {
            String urltext = URL_PREFACE + Y + "," + X + "," + (Y + TILE_SIZE) + "," + (X + TILE_SIZE) + URL_SUFFIX;
            TerraMod.LOGGER.info(urltext);

            URL url = new URL(urltext);
            InputStream is = url.openStream();

            doGson(is, mchunk);
            
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
    
    private void doGson(InputStream is, Coord region) throws IOException {
    	
    	StringWriter writer = new StringWriter();
    	IOUtils.copy(is, writer, StandardCharsets.UTF_8);
    	String str = writer.toString();
    	
    	Data data = gson.fromJson(str.toString(), Data.class);
    	
    	for(Element elem: data.elements) {
    		if(elem.type==EType.node) {
    			allNodes.put(elem.id, elem);
    		}
    		else if(elem.type==EType.way && elem.tags!=null) {
    			
    			String highway = elem.tags.get("highway");
    			
    			if(highway!=null) {
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
                    
                    //prevent super high # of lanes to prevent ridiculous results (prly an error if its this high anyways)
                    if(lanes>8)
                    	lanes = 8;
                    
                    //upgrade road type if many lanes (and the road was important enough to include a lanes tag)
                    if(lanes>2 && type==Type.MINOR)
                    	type = Type.MAJOR;
                    
	    			Element lastNode = null;
	    			double[] lastProj = null;
	    			for(long id: elem.nodes) {
	    				Element node = allNodes.get(id);
	    				double[] proj = projection.fromGeo(node.lon, node.lat);
	    				if(lastNode!=null) {
	    					allEdges.add(new Edge(lastProj[0], lastProj[1], proj[0], proj[1], type, lanes, region));
	    				}
	    				lastProj = proj;
	    				lastNode = node;
	    			}
    			}
    		}
    	}
    	
    	allNodes.clear();
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
    private void removeRegion(Coord mchunk) {
    	
    	double X = mchunk.x*TILE_SIZE;
        double Y = mchunk.y*TILE_SIZE;
    	
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
            			if(it.next().region.equals(mchunk))
            				it.remove();
            		
            		if(edges.size() <= 0)
            			chunks.remove(new Coord(x,z));
            	}
            }
        }
    }

    //integer coordinate class
    private static class Coord {
        private int x;
        private int y;

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
        
        Coord region;

        private double squareLength() {
            double dlat = elat-slat;
            double dlon = elon-slon;
            return dlat*dlat + dlon*dlon;
        }

        private Edge(double slon, double slat, double elon, double elat, Type type, byte lanes, Coord region) {
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
    	invalid, node, way, relation
    }
    
    public static class Element {
    	EType type;
    	long id;
    	Map<String,String> tags;
    	long[] nodes;
    	double lat;
    	double lon;
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