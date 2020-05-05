package io.github.terra121;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import io.github.opencubicchunks.cubicchunks.cubicgen.blue.endless.jankson.api.DeserializationException;
import io.github.opencubicchunks.cubicchunks.cubicgen.blue.endless.jankson.api.SyntaxError;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.preset.CustomGenSettingsSerialization;
import io.github.opencubicchunks.cubicchunks.cubicgen.preset.fixer.CustomGeneratorSettingsFixer;
import io.github.opencubicchunks.cubicchunks.cubicgen.preset.fixer.PresetLoadError;
import io.github.terra121.projection.GeographicProjection;
import io.github.terra121.projection.ScaleProjection;

public class EarthGeneratorSettings {
	
	//json template to be filled by Gson
	public static class JsonSettings {
		public String projection = "equirectangular";
		public GeographicProjection.Orientation orentation = GeographicProjection.Orientation.swapped;
		public Double scaleX = 100000.0;
		public Double scaleY = 100000.0;
		public Boolean smoothblend = true;
		public Boolean roads = true;
		public String customcubic = "";
		public Boolean dynamicbaseheight = true;
		public Boolean osmwater = false;
		public Boolean buildings = false;
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
	
	public CustomGeneratorSettings getCustomCubic() {
		if(settings.customcubic.length()==0) {
			CustomGeneratorSettings cfg = CustomGeneratorSettings.defaults();
	        cfg.ravines = false;
	        cfg.dungeonCount = 3; //there are way too many of these by default (in my humble opinion)
	        
	        //no surface lakes by default
	        for(CustomGeneratorSettings.LakeConfig lake: cfg.lakes)
	        	lake.surfaceProbability = new CustomGeneratorSettings.UserFunction();
	        
	        return cfg;
		}
		
		return customCubicFromJson(settings.customcubic);
	}
	
	//Crappy attempt to coerce custom cubic settings
	private CustomGeneratorSettings customCubicFromJson(String jsonString) {
		try {
            return CustomGenSettingsSerialization.jankson().fromJsonCarefully(jsonString, CustomGeneratorSettings.class);
        } catch (PresetLoadError | DeserializationException err) {
            throw new RuntimeException(err);
        } catch (SyntaxError err) {
            String message = err.getMessage() + "\n" + err.getLineMessage();
            throw new RuntimeException(message, err);
        }
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
			GeographicProjection.projections.get(settings.projection),GeographicProjection.Orientation.upright);
	}
}
