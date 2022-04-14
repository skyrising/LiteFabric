package de.skyrising.litefabric.impl;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McmodInfo {
    @SerializedName("modid")
    public final String id;
    public final String name;
    public final String description;
    public final String version;
    @SerializedName("mcversion")
    public final String mcVersion;
    public final String url;
    public final String updateUrl;
    @SerializedName("authorList")
    public final List<String> authors;
    public final String credits;
    public final String logoFile;

    public McmodInfo(String id, String name, String description, String version, String mcVersion, String url, String updateUrl, List<String> authors, String credits, String logoFile) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.mcVersion = mcVersion;
        this.url = url;
        this.updateUrl = updateUrl;
        this.authors = authors;
        this.credits = credits;
        this.logoFile = logoFile;
    }

    public static Map<String, McmodInfo> parse(Reader reader) {
        Gson gson = new Gson();
        List<McmodInfo> infos = gson.fromJson(reader, new TypeToken<List<McmodInfo>>(){}.getType());
        Map<String, McmodInfo> map = new HashMap<>();
        for (McmodInfo info : infos) {
            map.put(info.id, info);
        }
        return map;
    }
}
