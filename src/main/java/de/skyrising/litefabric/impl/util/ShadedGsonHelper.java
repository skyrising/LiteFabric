package de.skyrising.litefabric.impl.util;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

public class ShadedGsonHelper {
    public static Pair<String, Object> parseMixinConfig(InputStream stream, Class<?> mixinConfigClass) {
        // Use the normal Gson so we can use JsonObject.has/remove, etc.
        JsonObject obj = new com.google.gson.Gson().fromJson(new InputStreamReader(stream), JsonObject.class);
        String refMap = null;
        if (obj.has("refmap")) {
            refMap = obj.getAsJsonPrimitive("refmap").getAsString();
            obj.remove("refmap");
        }
        // Use Mixin's Gson so it recognizes its own @SerializedName annotations correctly
        Object config = new org.spongepowered.include.com.google.gson.Gson().fromJson(new StringReader(obj.toString()), mixinConfigClass);
        return Pair.of(refMap, config);
    }
}
