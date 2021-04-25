package de.skyrising.litefabric.impl.util;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.ModIconHandler;
import de.skyrising.litefabric.impl.LiteFabric;
import de.skyrising.litefabric.impl.LitemodContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ModMenuCompat implements ModMenuApi {
    public ModMenuCompat() {
        for (LitemodContainer mod : LiteFabric.getInstance().getMods()) {
            LiteModMenuEntry entry = new LiteModMenuEntry(mod);
            String id = entry.getId();
            ModMenu.MODS.put(id, entry);
            ModMenu.ROOT_MODS.put(id, entry);
        }
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new HashMap<>();
        LiteFabric liteFabric = LiteFabric.getInstance();
        for (LitemodContainer mod : liteFabric.getMods()) {
            String id = mod.meta.name;
            factories.put(id, parent -> liteFabric.getConfigScreenForMod(id, parent));
        }
        return factories;
    }

    static class LiteModMenuEntry implements Mod {
        private final String id;
        private final String name;
        private final String summary;
        private final String description;
        private final String version;
        private final List<String> authors;

        public LiteModMenuEntry(LitemodContainer mod) {
            this.id = mod.meta.name;
            String name = mod.meta.displayName;
            if (name == null) name = id;
            this.name = name;
            String description = mod.meta.description;
            if (description == null) description = "";
            this.description = description;
            this.summary = description.split("\n")[0];
            String version = mod.meta.version;
            if (version.startsWith("v")) version = version.substring(1);
            this.version = version;
            this.authors = Arrays.stream(Objects.toString(mod.meta.author, "").split(",")).map(String::trim).collect(Collectors.toList());
        }

        @Override
        public @NotNull String getId() {
            return id;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public @NotNull NativeImageBackedTexture getIcon(ModIconHandler iconHandler, int i) {
            return iconHandler.createIcon(FabricLoader.getInstance().getModContainer(ModMenu.MOD_ID).orElseThrow(() -> new RuntimeException("Cannot get ModContainer for Fabric mod with id " + ModMenu.MOD_ID)), "assets/" + ModMenu.MOD_ID + "/unknown_icon.png");
        }

        @Override
        public @NotNull String getSummary() {
            return summary;
        }

        @Override
        public @NotNull String getDescription() {
            return description;
        }

        @Override
        public @NotNull String getVersion() {
            return version;
        }

        @Override
        public @NotNull List<String> getAuthors() {
            return authors;
        }

        @Override
        public @NotNull List<String> getContributors() {
            return Collections.emptyList();
        }

        @Override
        public @NotNull Set<Badge> getBadges() {
            return Collections.singleton(Badge.LITELOADER);
        }

        @Override
        public @Nullable String getWebsite() {
            return null;
        }

        @Override
        public @Nullable String getIssueTracker() {
            return null;
        }

        @Override
        public @Nullable String getSource() {
            return null;
        }

        @Override
        public @Nullable String getParent() {
            return null;
        }

        @Override
        public @NotNull Set<String> getLicense() {
            return Collections.emptySet();
        }

        @Override
        public @NotNull Map<String, String> getLinks() {
            return Collections.emptyMap();
        }

        @Override
        public boolean isReal() {
            return true;
        }
    }
}
