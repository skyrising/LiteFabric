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

        public LiteModMenuEntry(LitemodContainer mod) {
            this.mod = mod;
        }

        @Override
        public @NotNull String getId() {
            return mod.meta.name;
        }

        @Override
        public @NotNull String getName() {
            String name = mod.meta.dynamicDisplayName;
            if (name == null) name = mod.meta.displayName;
            if (name == null) name = getId();
            if (name.startsWith("root project '") && name.length() > 14) name = name.substring(14, name.length() - 1);
            return name;
        }

        @Override
        public @NotNull NativeImageBackedTexture getIcon(ModIconHandler iconHandler, int i) {
            String logo = mod.mcmodInfo != null ? mod.mcmodInfo.logoFile : null;
            if (logo != null && !logo.isEmpty()) {
                return ModMenuCompat.ICON_HANDLER.createIcon(mod, logo);
            }
            return iconHandler.createIcon(FabricLoader.getInstance().getModContainer(ModMenu.MOD_ID).orElseThrow(() -> new RuntimeException("Cannot get ModContainer for Fabric mod with id " + ModMenu.MOD_ID)), "assets/" + ModMenu.MOD_ID + "/unknown_icon.png");
        }

        @Override
        public @NotNull String getSummary() {
            return getDescription().split("\n")[0];
        }

        @Override
        public @NotNull String getDescription() {
            String description = mod.meta.description;
            if (description == null) description = "";
            if (mod.mcmodInfo != null && description.isEmpty() && mod.mcmodInfo.description != null) {
                description = mod.mcmodInfo.description;
            }
            return description;
        }

        @Override
        public @NotNull String getVersion() {
            String version = mod.meta.version;
            if (version == null) version = mod.meta.dynamicVersion;
            if (version == null) version = mod.meta.mcversion + "-" + mod.meta.revision;
            if (version.startsWith("v")) version = version.substring(1);
            return version;
        }

        private Set<String> getAuthorsSet() {
            return Arrays.stream(Objects.toString(mod.meta.author, "").split(",")).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public @NotNull List<String> getAuthors() {
            return new ArrayList<>(getAuthorsSet());
        }

        @Override
        public @NotNull List<String> getContributors() {
            Set<String> authors = getAuthorsSet();
            if (mod.mcmodInfo != null && mod.mcmodInfo.authors != null) authors.addAll(mod.mcmodInfo.authors);
            return new ArrayList<>(authors);
        }

        @Override
        public @NotNull Set<Badge> getBadges() {
            Set<Badge> badges = new HashSet<>();
            badges.add(Badge.LITELOADER);
            if (getDescription().toLowerCase(Locale.ROOT).contains("client")) badges.add(Badge.CLIENT);
            if (getId().toLowerCase(Locale.ROOT).endsWith("lib")) badges.add(Badge.LIBRARY);
            return badges;
        }

        @Override
        public @Nullable String getWebsite() {
            return mod.mcmodInfo != null ? mod.mcmodInfo.url : null;
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
