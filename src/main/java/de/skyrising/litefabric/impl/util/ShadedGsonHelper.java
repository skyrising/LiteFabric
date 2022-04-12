package de.skyrising.litefabric.impl.util;

import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

public class ShadedGsonHelper {
    public static Object parseMixinConfig(InputStream stream, String configFile, Class<?> mixinConfigClass) {
        // Use the normal Gson, so we can use JsonObject.has/remove, etc.
        JsonObject obj = new com.google.gson.Gson().fromJson(new InputStreamReader(stream), JsonObject.class);
        obj.addProperty("refmapWrapper", "$" + configFile.replace('.', '_') + "$LiteFabricRefMapper");
        // Use Mixin's Gson, so it recognizes its own @SerializedName annotations correctly
        return new org.spongepowered.include.com.google.gson.Gson().fromJson(new StringReader(obj.toString()), mixinConfigClass);
    }
}
