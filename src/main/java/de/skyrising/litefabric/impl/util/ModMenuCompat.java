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
    private static final LitemodIconHandler ICON_HANDLER = new LitemodIconHandler();

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
        private final LitemodContainer mod;
        private final String id;
        private final String name;
        private final String summary;
        private final String description;
        private final String version;
        private final Set<String> authors;
        private final String website;
        private final String logo;

        public LiteModMenuEntry(LitemodContainer mod) {
            this.mod = mod;
            this.id = mod.meta.name;
            String name = mod.meta.displayName;
            if (name == null) name = id;
            if (name.startsWith("root project '") && name.length() > 14) name = name.substring(14, name.length() - 1);
            this.name = name;
            String description = mod.meta.description;
            if (description == null) description = "";
            String version = mod.meta.version;
            if (version == null) version = mod.meta.mcversion + "-" + mod.meta.revision;
            this.authors = Arrays.stream(Objects.toString(mod.meta.author, "").split(",")).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
            String website = null;
            String logo = null;
            if (mod.mcmodInfo != null) {
                if (description.isEmpty() && mod.mcmodInfo.description != null) description = mod.mcmodInfo.description;
                if (mod.mcmodInfo.authors != null) authors.addAll(mod.mcmodInfo.authors);
                website = mod.mcmodInfo.url;
                logo = mod.mcmodInfo.logoFile;
            }
            this.description = description;
            this.summary = description.split("\n")[0];
            if (version.startsWith("v")) version = version.substring(1);
            this.version = version;
            this.website = website;
            this.logo = logo;
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
            if (logo != null && !logo.isEmpty()) {
                return ModMenuCompat.ICON_HANDLER.createIcon(mod, logo);
            }
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
            return new ArrayList<>(authors);
        }

        @Override
        public @NotNull List<String> getContributors() {
            return Collections.emptyList();
        }

        @Override
        public @NotNull Set<Badge> getBadges() {
            Set<Badge> badges = new HashSet<>();
            badges.add(Badge.LITELOADER);
            if (description.toLowerCase(Locale.ROOT).contains("client")) badges.add(Badge.CLIENT);
            if (id.toLowerCase(Locale.ROOT).endsWith("lib")) badges.add(Badge.LIBRARY);
            return badges;
        }

        @Override
        public @Nullable String getWebsite() {
            return website;
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
