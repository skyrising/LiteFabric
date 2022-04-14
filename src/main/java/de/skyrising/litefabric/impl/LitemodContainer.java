package de.skyrising.litefabric.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.skyrising.litefabric.liteloader.LiteMod;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.impl.util.FileSystemUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.minecraft.client.resource.ResourceMetadataProvider;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ZipResourcePack;
import net.minecraft.util.Identifier;
import net.minecraft.util.MetadataSerializer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipError;

import static net.fabricmc.loader.impl.util.log.LogCategory.DISCOVERY;

public class LitemodContainer extends ZipResourcePack implements ResourcePack {
    public final ModMetadata meta;
    public final List<String> entryPoints;
    public final Set<String> configGuiCandidates = new TreeSet<>((a, b) -> {
        if (a == null) return 1;
        if (a.length() == b.length()) return a.compareTo(b);
        return a.length() - b.length();
    });
    public final Map<String, McmodInfo> mcmodInfos;
    @Nullable
    public McmodInfo mcmodInfo;
    private final FileSystemUtil.FileSystemDelegate fileSystem;
    final LitemodClassProvider classProvider;

    public LitemodContainer(Path path, ModMetadata meta, Map<String, McmodInfo> mcmodInfos, List<String> entryPoints, FileSystemUtil.FileSystemDelegate fileSystem, LitemodRemapper remapper) {
        super(path.toFile());
        this.meta = meta;
        this.mcmodInfos = mcmodInfos == null ? Collections.emptyMap() : Collections.unmodifiableMap(mcmodInfos);
        this.entryPoints = Collections.unmodifiableList(entryPoints);
        this.fileSystem = fileSystem;
        this.classProvider = new LitemodClassProvider(this, fileSystem.get(), remapper);
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
                // VoxelMap depends on itself being a ZipResourcePack with a non-null zipFile
                if ("voxelmap".equalsIgnoreCase(meta.name)) super.containsFile("");
                try {
                    this.meta.dynamicVersion = mod.getVersion();
                } catch (Throwable ignored) {}
                try {
                    this.meta.dynamicDisplayName = mod.getName();
                } catch (Throwable ignored) {}
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
        Path modJson, mcmodInfo, rootDir;
        try {
            jarFs = FileSystemUtil.getJarFileSystem(path, false);
            modJson = jarFs.get().getPath("litemod.json");
            mcmodInfo = jarFs.get().getPath("mcmod.info");
            rootDir = jarFs.get().getRootDirectories().iterator().next();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open mod JAR at " + path + "!");
        } catch (ZipError e) {
            throw new RuntimeException("Jar at " + path + " is corrupted, please redownload it!");
        }
        if (!Files.exists(modJson)) {
            Log.warn(DISCOVERY, "No litemod.json found in " + path + ", skipping");
            return Optional.empty();
        }
        ModMetadata meta;
        try {
            meta = new Gson().fromJson(Files.newBufferedReader(modJson), ModMetadata.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read litemod.json in " + path + "!");
        } catch (JsonParseException e) {
            Log.warn(DISCOVERY, "Could not load litemod.json in " + path + ": " + e.getMessage());
            return Optional.empty();
        }
        Map<String, McmodInfo> mcmodInfos = null;
        if (Files.exists(mcmodInfo)) {
            try {
                 mcmodInfos = McmodInfo.parse(Files.newBufferedReader(mcmodInfo, StandardCharsets.UTF_8));
            } catch (IOException e) {
                Log.warn(DISCOVERY, "Failed to read mcmod.info in " + path + ": " + e.getMessage());
            } catch (JsonParseException e) {
                Log.warn(DISCOVERY, "Could not load mcmod.info in " + path + ": " + e.getMessage());
            }
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
        return Optional.of(new LitemodContainer(path, meta, mcmodInfos, entryPoints, jarFs, remapper));
    }

    private Path getPath(Identifier id) {
        return fileSystem.get().getPath("assets", id.getNamespace(), id.getPath());
    }

    public Path getPath(String file) {
        return fileSystem.get().getPath(file);
    }

    @Override
    public InputStream open(Identifier id) throws IOException {
        return Files.newInputStream(getPath(id));
    }

    protected InputStream openFile(String file) throws IOException {
        return Files.newInputStream(getPath(file));
    }

    @Override
    public boolean contains(Identifier id) {
        return Files.exists(getPath(id));
    }

    @Override
    public Set<String> getNamespaces() {
        try {
            return Files.list(getPath("assets")).filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    @Nullable
    @Override
    public <T extends ResourceMetadataProvider> T parseMetadata(MetadataSerializer parser, String section) throws IOException {
        try {
            return parseJson(parser, this.openFile("pack.mcmeta"), section);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    static <T extends ResourceMetadataProvider> T parseJson(MetadataSerializer parser, InputStream inputStream, String section) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            JsonObject jsonObject = (new JsonParser()).parse(bufferedReader).getAsJsonObject();
            return parser.fromJson(section, jsonObject);
        } catch (RuntimeException var9) {
            throw new JsonParseException(var9);
        } finally {
            IOUtils.closeQuietly(bufferedReader);
        }
    }

    @Override
    public BufferedImage getIcon() throws IOException {
        return TextureUtil.create(this.openFile("pack.png"));
    }

    @Override
    public String getName() {
        return "Litemod(" + meta.name + ")";
    }

    public void dumpResources() throws IOException {
        for (Path baseSrc : fileSystem.get().getRootDirectories()) {
            Path baseDest = Paths.get(".litefabric.out/resource", meta.name);
            Files.walkFileTree(baseSrc, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getNameCount() == 0 || file.toString().endsWith(".class")) return FileVisitResult.CONTINUE;
                    Path dest = baseDest.resolve(baseSrc.relativize(file).toString());
                    Files.createDirectories(dest.getParent());
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
