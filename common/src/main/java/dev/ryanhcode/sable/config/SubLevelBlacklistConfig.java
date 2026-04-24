package dev.ryanhcode.sable.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.platform.SableLoaderPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class SubLevelBlacklistConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "sable/sublevel_blacklisted.json";

    private static volatile Set<ResourceLocation> blacklisted = Set.of();

    public static void load() {
        final Path path = SableLoaderPlatform.INSTANCE.getConfigDir().resolve(CONFIG_FILE);

        if (!Files.exists(path)) {
            writeDefault(path);
        }

        blacklisted = readFromFile(path);
        Sable.LOGGER.info("Loaded {} sub-level blacklisted block(s) from config", blacklisted.size());
    }

    public static boolean isBlacklisted(final BlockState state) {
        return blacklisted.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static Set<ResourceLocation> getBlacklisted() {
        return blacklisted;
    }

    public static Path getConfigPath() {
        return SableLoaderPlatform.INSTANCE.getConfigDir().resolve(CONFIG_FILE);
    }

    private static void writeDefault(final Path path) {
        try {
            Files.createDirectories(path.getParent());

            final JsonObject root = new JsonObject();
            root.addProperty("description",
                    "Blocks listed here cannot be placed on or assembled into sub-levels. " +
                    "Add block IDs (e.g. \"modid:block_name\") to prevent crashes from incompatible mods.");

            final JsonArray blocks = new JsonArray();
            blocks.add("minecraft:lapis_block");
            root.add("blacklisted_blocks", blocks);

            Files.writeString(path, GSON.toJson(root));
            Sable.LOGGER.info("Generated default sub-level blacklist config at {}", path);
        } catch (final IOException e) {
            Sable.LOGGER.error("Failed to write default sub-level blacklist config at {}", path, e);
        }
    }

    private static Set<ResourceLocation> readFromFile(final Path path) {
        try {
            final JsonObject root = GSON.fromJson(Files.readString(path), JsonObject.class);
            final Set<ResourceLocation> result = new HashSet<>();

            if (root != null && root.has("blacklisted_blocks") && root.get("blacklisted_blocks").isJsonArray()) {
                for (final var element : root.getAsJsonArray("blacklisted_blocks")) {
                    final String id = element.getAsString();
                    try {
                        result.add(ResourceLocation.parse(id));
                    } catch (final Exception e) {
                        Sable.LOGGER.warn("Skipping invalid block ID '{}' in sub-level blacklist config", id);
                    }
                }
            }

            return Set.copyOf(result);
        } catch (final IOException e) {
            Sable.LOGGER.error("Failed to read sub-level blacklist config from {}", path, e);
            return Set.of();
        }
    }
}
