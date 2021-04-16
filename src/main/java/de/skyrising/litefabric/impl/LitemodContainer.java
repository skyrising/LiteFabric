package de.skyrising.litefabric.impl;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import de.skyrising.litefabric.liteloader.LiteMod;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.util.FileSystemUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipError;

public class LitemodContainer {
    public final ModMetadata meta;
    public final List<String> entryPoints;
    private final FileSystemUtil.FileSystemDelegate fileSystem;
    final LitemodClassLoader classLoader;

    public LitemodContainer(ModMetadata meta, List<String> entryPoints, FileSystemUtil.FileSystemDelegate fileSystem, LitemodRemapper remapper, CodeSource codeSource, ClassLoader parent) {
        this.meta = meta;
        this.entryPoints = Collections.unmodifiableList(entryPoints);
        this.fileSystem = fileSystem;
        this.classLoader = new LitemodClassLoader(parent, fileSystem.get(), remapper, codeSource);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public LiteMod init(File configPath) {
        for (String className : entryPoints) {
            try {
                Class<? extends LiteMod> modClass = (Class<? extends LiteMod>) classLoader.loadClass(className);
                System.out.println(modClass + ": " + modClass.getClassLoader());
                for (Class<?> itf : modClass.getInterfaces()) {
                    System.out.println(itf + ": " + itf.getClassLoader());
                }
                LiteMod mod = modClass.newInstance();
                mod.init(configPath);
                return mod;
            } catch (Throwable t) {
                throw new RuntimeException("Failed to initialize LiteMod " + meta.name, t);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return meta.name + "@" + meta.version;
    }

    public static Optional<LitemodContainer> load(Path path, LitemodRemapper remapper) {
        FabricLoader loader = (FabricLoader) net.fabricmc.loader.api.FabricLoader.getInstance();
        FileSystemUtil.FileSystemDelegate jarFs;
        Path modJson, rootDir;
        try {
            jarFs = FileSystemUtil.getJarFileSystem(path, false);
            modJson = jarFs.get().getPath("litemod.json");
            rootDir = jarFs.get().getRootDirectories().iterator().next();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open mod JAR at " + path + "!");
        } catch (ZipError e) {
            throw new RuntimeException("Jar at " + path + " is corrupted, please redownload it!");
        }
        if (!Files.exists(modJson)) {
            loader.getLogger().warn("No litemod.json found in " + path + ", skipping");
            return Optional.empty();
        }
        ModMetadata meta;
        try {
            meta = new Gson().fromJson(Files.newBufferedReader(modJson), ModMetadata.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read litemod.json in " + path + "!");
        } catch (JsonParseException e) {
            loader.getLogger().warn("Could not load litemod.json in " + path + ": " + e.getMessage());
            return Optional.empty();
        }
        CodeSource codeSource;
        try {
            codeSource = new CodeSource(path.toUri().toURL(), (Certificate[]) null);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to construct CodeSource", e);
        }
        List<String> entryPoints = new ArrayList<>();
        try {
            Files.walk(rootDir).filter(p -> {
                if (p.getNameCount() == 0) return false;
                String fileName = p.getFileName().toString();
                return fileName.startsWith("LiteMod") && fileName.endsWith(".class");
            }).forEach(p -> {
                Path relative = rootDir.relativize(p);
                String fullStr = relative.toString();
                String nameStr = fullStr.substring(0, fullStr.length() - 6).replace('/', '.');
                entryPoints.add(nameStr);
            });
        } catch (IOException e) {
            throw new RuntimeException("Could not search for LiteMod entry point", e);
        }
        return Optional.of(new LitemodContainer(meta, entryPoints, jarFs, remapper, codeSource, LiteFabric.getInstance().combinedClassLoader));
    }
}
