package de.skyrising.litefabric.impl;

import java.util.List;

public class ModMetadata {
    public final String name;
    public final String displayName;
    public final String description;
    public final String version;
    public final String author;
    public final String mcversion;
    public final String revision;
    public final List<String> mixinConfigs;
    public String dynamicVersion;
    public String dynamicDisplayName;

    public ModMetadata(String name, String displayName, String description, String version, String author, String mcversion, String revision, List<String> mixinConfigs) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.version = version;
        this.author = author;
        this.mcversion = mcversion;
        this.revision = revision;
        this.mixinConfigs = mixinConfigs;
    }
}
