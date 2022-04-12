package de.skyrising.litefabric.impl;

import de.skyrising.litefabric.impl.util.RemappingReferenceMapper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class PreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        long start = System.nanoTime();
        // Don't ask
        //noinspection ResultOfMethodCallIgnored
        RemappingReferenceMapper.class.getName();
        FabricLoader loader = FabricLoader.getInstance();
        Path gameDir = loader.getGameDir();
        Path modsDir = gameDir.resolve("mods");
        List<LitemodContainer> mods = new ArrayList<>();
        if (Files.exists(modsDir) && Files.isDirectory(modsDir)) {
            try {
                mods.addAll(prepareMods(modsDir));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        LiteFabric liteFabric = LiteFabric.getInstance();
        liteFabric.addMods(mods);
        liteFabric.preLaunch();
        if (LiteFabric.PROFILE_STARTUP) {
            LogManager.getFormatterLogger("LiteFabric|PreLaunch").info("preLaunch took %.3fms", (System.nanoTime() - start) / 1e6);
        }
    }

    private static List<LitemodContainer> prepareMods(Path modsDir) throws IOException {
        List<Path> jars = new ArrayList<>();
        Files.walkFileTree(modsDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();

                if (fileName.endsWith(".litemod") && !fileName.startsWith(".") && !Files.isHidden(file)) {
                    jars.add(file);
                }

                return FileVisitResult.CONTINUE;

            }
        });
        List<LitemodContainer> mods = new ArrayList<>();
        LitemodRemapper remapper = LiteFabric.getInstance().getRemapper();
        for (Path path : jars) {
            Optional<LitemodContainer> mod = LitemodContainer.load(path, remapper);
            mod.ifPresent(mods::add);
        }
        return mods;
    }
}
