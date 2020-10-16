package io.github.terra121;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import io.github.opencubicchunks.cubicchunks.cubicgen.blue.endless.jankson.api.DeserializationException;
import io.github.opencubicchunks.cubicchunks.cubicgen.blue.endless.jankson.api.SyntaxError;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.preset.CustomGenSettingsSerialization;
import io.github.opencubicchunks.cubicchunks.cubicgen.preset.fixer.PresetLoadError;
import io.github.terra121.projection.GeographicProjection;
import io.github.terra121.projection.ScaleProjection;

public class EarthGeneratorSettings {

    public JsonSettings settings;
    private final Gson gson;

    public EarthGeneratorSettings(String generatorSettings) {

        System.out.println(generatorSettings);

        this.gson = new GsonBuilder().create();

        if (generatorSettings.length() == 0) { //blank string means default
            this.settings = new JsonSettings();
        } else {
            try {
                this.settings = this.gson.fromJson(generatorSettings, JsonSettings.class);
            } catch (JsonSyntaxException e) {
                TerraMod.LOGGER.error("Invalid Earth Generator Settings, using default settings");
                this.settings = new JsonSettings();
            }
        }
    }

    public String toString() {
        return this.gson.toJson(this.settings, JsonSettings.class);
    }

    public CustomGeneratorSettings getCustomCubic() {
        if (this.settings.customcubic.length() == 0) {
            CustomGeneratorSettings cfg = CustomGeneratorSettings.defaults();
            cfg.ravines = false;
            cfg.dungeonCount = 3; //there are way too many of these by default (in my humble opinion)

            //no surface lakes by default
            for (CustomGeneratorSettings.LakeConfig lake : cfg.lakes) {
                lake.surfaceProbability = new CustomGeneratorSettings.UserFunction();
            }

            return cfg;
        }

        return this.customCubicFromJson(this.settings.customcubic);
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
                GeographicProjection.projections.get(this.settings.projection), this.settings.orentation);

        if (this.settings.scaleX == null || this.settings.scaleY == null) {
            return new ScaleProjection(p, 100000, 100000); //TODO: better default
        }

        if (this.settings.scaleX == 1 && this.settings.scaleY == 1) {
            System.exit(-1);
        }

        return new ScaleProjection(p, this.settings.scaleX, this.settings.scaleY);
    }

    public GeographicProjection getNormalizedProjection() {
        return GeographicProjection.orientProjection(
                GeographicProjection.projections.get(this.settings.projection), GeographicProjection.Orientation.upright);
    }

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
}
