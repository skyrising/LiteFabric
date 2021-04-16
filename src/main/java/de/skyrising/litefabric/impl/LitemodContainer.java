package de.skyrising.litefabric.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.skyrising.litefabric.liteloader.LiteMod;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.util.FileSystemUtil;
import net.minecraft.class_6055;
import net.minecraft.class_6057;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipError;

public class LitemodContainer implements ResourcePack {
    public final ModMetadata meta;
    public final List<String> entryPoints;
    private final FileSystemUtil.FileSystemDelegate fileSystem;
    final LitemodClassProvider classProvider;

    public LitemodContainer(ModMetadata meta, List<String> entryPoints, FileSystemUtil.FileSystemDelegate fileSystem, LitemodRemapper remapper) {
        this.meta = meta;
        this.entryPoints = Collections.unmodifiableList(entryPoints);
        this.fileSystem = fileSystem;
        this.classProvider = new LitemodClassProvider(fileSystem.get(), remapper);
    }

    LitemodClassProvider getClassProvider() {
        return classProvider;
    }

    public LiteMod init(File configPath) {
        for (String className : entryPoints) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends LiteMod> modClass = (Class<? extends LiteMod>) FabricLauncherBase.getClass(className);
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
        return Optional.of(new LitemodContainer(meta, entryPoints, jarFs, remapper));
    }

    private Path getPath(Identifier id) {
        return fileSystem.get().getPath("assets", id.getNamespace(), id.getPath());
    }

    private Path getPath(String file) {
        return fileSystem.get().getPath(file);
    }

    @Override
    public InputStream open(Identifier id) throws IOException {
        return Files.newInputStream(getPath(id));
    }

    private InputStream openFile(String file) throws IOException {
        return Files.newInputStream(getPath(file));
    }

    @Override
    public boolean contains(Identifier id) {
        return Files.exists(getPath(id));
    }

    @Override
    public Set<String> method_31465() {
        try {
            return Files.list(getPath("assets")).filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    @Nullable
    @Override
    public <T extends class_6055> T method_31461(class_6057 parser, String section) throws IOException {
        try {
            return parseJson(parser, this.openFile("pack.mcmeta"), section);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    static <T extends class_6055> T parseJson(class_6057 parser, InputStream inputStream, String section) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            JsonObject jsonObject = (new JsonParser()).parse(bufferedReader).getAsJsonObject();
            return parser.method_31530(section, jsonObject);
        } catch (RuntimeException var9) {
            throw new JsonParseException(var9);
        } finally {
            IOUtils.closeQuietly(bufferedReader);
        }
    }

    @Override
    public BufferedImage method_31460() throws IOException {
        return TextureUtil.method_31382(this.openFile("pack.png"));
    }

    @Override
    public String getName() {
        return "Litemod(" + meta.name + ")";
    }
}
