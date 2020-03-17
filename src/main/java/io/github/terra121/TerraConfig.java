package io.github.terra121;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.*;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.ConfigManager;

@Config(modid = TerraMod.MODID)
public class TerraConfig {
	@Name("overpass_interpreter")
	@Comment({"overpass interpreter for road and water OpenStreetMap data",
			  "Make sure you follow the instances guidelines",
			  "Url must be able to take interpreter input by adding a \'?\'",
			  "e.x. \"https://.../api/interpreter\""})
	public static String serverOverpass = "https://overpass.kumi.systems/api/interpreter"; //"https://overpass-api.de/api/interpreter"
	
	@Name("rest_tree_services")
	@Comment({"An ArcGis REST API instance with tree cover support",
		  	  "Should allow all tree data sources used (just TreeCover2000 right now)",
			  "End with a \"/\" e.x. \"https://.../arcgis/rest/services/\""})
	public static String serverTree = "https://gis-treecover.wri.org/arcgis/rest/services/";
	
	@Name("terrarium_instance")
	@Comment({"A Mapzen Terrain Tile terrrarium instance allowing x/y.png querrys",
			  "End with a \"/\" e.x. https://.../terrarium/"})
	public static String serverTerrain = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/";
	
	@Name("cache_size")
	@Comment({"Amount of tiles to keep in memory at once",
			  "This applies to both Tree data and Height data",
			  "Evrey tile takes exactly 262,144 bytes of memory (plus some support structures)",
			  "The memory requirement for the tiles will be about cacheSize/2 MB",
			  "Warning: This number should be at least 4*playerCount to prevent massive slowdowns and internet useage, lower at your own risk"})
	@RangeInt(min = 1)
	public static int cacheSize = 100;
	
	@Name("osm_cache_size")
	@Comment({"Number of osm regions to keep data about at a time",
			  "(these tiles are roughly 1,850 meters/blocks in length but this varies based of position and projection) (they are exaclty 1 arcminute across)",
			  "Warning: The amount of memory taken by theses tiles fluxuates based on region and is not well studied, raise at your own risk",
			  "Warning: This number should be at least 9*playerCount to prevent massive slowdowns and internet useage, lower at your own risk"})
	@RangeInt(min = 1)
	public static int osmCacheSize = 1000;
	
	@Name("three_water")
	@Comment({"require 3 water in order to form a new source instead of the vanilla 2",
			"This will make generated streams more stable but will disrupt vanilla water mechanics like 2x2 infinite water sources",
			"Highly expiremental, use at your own risk"})
	public static boolean threeWater = false;

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if(TerraMod.MODID.equals(event.getModID()))
			ConfigManager.sync(TerraMod.MODID, Config.Type.INSTANCE);
	}
}
