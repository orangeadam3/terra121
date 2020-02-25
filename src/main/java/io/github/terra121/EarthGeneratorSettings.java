package io.github.terra121;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import io.github.terra121.projection.GeographicProjection;
import io.github.terra121.projection.ScaleProjection;

public class EarthGeneratorSettings {
	
	//json template to be filled by Gson
	public static class JsonSettings {
		public String projection = "equirectangular";
		public GeographicProjection.Orentation orentation = GeographicProjection.Orentation.swapped;
		public Double scaleX = 100000.0;
		public Double scaleY = 100000.0;
		public Boolean smoothblend = false;
		public Boolean roads = true;
	}
	public JsonSettings settings;
	
	private Gson gson;
	
	public EarthGeneratorSettings(String generatorSettings) {
		
		System.out.println(generatorSettings);
		
		gson = new GsonBuilder().create();
		
		if(generatorSettings.length()==0) { //blank string means default
			settings = new JsonSettings();
		}
		else try {
			settings = gson.fromJson(generatorSettings, JsonSettings.class);
		} catch(JsonSyntaxException e) {
			TerraMod.LOGGER.error("Invalid Earth Generator Settings, using default settings");
			settings = new JsonSettings();
		}
	}
	
	public String toString() {
		return gson.toJson(settings, JsonSettings.class);
	}
	
	public GeographicProjection getProjection() {
		GeographicProjection p = GeographicProjection.orientProjection(
			GeographicProjection.projections.get(settings.projection),settings.orentation);
		
		if(settings.scaleX==null||settings.scaleY==null) {
			return new ScaleProjection(p, 100000, 100000); //TODO: better default
		}
		
		if(settings.scaleX==1&&settings.scaleY==1) System.exit(-1);
		
		return new ScaleProjection(p, settings.scaleX, settings.scaleY);
	}
	
	public GeographicProjection getNormalizedProjection() {
		return GeographicProjection.orientProjection(
			GeographicProjection.projections.get(settings.projection),GeographicProjection.Orentation.upright);
	}
}
