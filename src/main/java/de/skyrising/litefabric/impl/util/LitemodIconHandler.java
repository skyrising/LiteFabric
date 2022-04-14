package de.skyrising.litefabric.impl.util;

import de.skyrising.litefabric.impl.LitemodContainer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureUtil;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LitemodIconHandler {
    private static final Logger LOGGER = LogManager.getLogger("LiteFabric|ModIconHandler");

    private final Map<Path, NativeImageBackedTexture> modIconCache = new HashMap<>();

    public NativeImageBackedTexture createIcon(LitemodContainer iconSource, String iconPath) {
        try {
            Path path = iconSource.getPath(iconPath);
            NativeImageBackedTexture cachedIcon = getCachedModIcon(path);
            if (cachedIcon != null) {
                return cachedIcon;
            }
            cachedIcon = getCachedModIcon(path);
            if (cachedIcon != null) {
                return cachedIcon;
            }
            try (InputStream inputStream = Files.newInputStream(path)) {
                BufferedImage image = TextureUtil.create(inputStream);
                Validate.validState(image.getHeight() == image.getWidth(), "Must be square icon");
                NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
                cacheModIcon(path, tex);
                return tex;
            }

        } catch (Throwable t) {
            if (!iconPath.equals("assets/" + iconSource.getName() + "/icon.png")) {
                LOGGER.error("Invalid mod icon for icon source {}: {}", iconSource.getName(), iconPath, t);
            }
            return null;
        }
    }

    NativeImageBackedTexture getCachedModIcon(Path path) {
        return modIconCache.get(path);
    }

    void cacheModIcon(Path path, NativeImageBackedTexture tex) {
        modIconCache.put(path, tex);
    }
}
