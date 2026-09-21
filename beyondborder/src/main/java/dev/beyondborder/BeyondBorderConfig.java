package dev.beyondborder;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads {@code config/beyondborder.json}.
 *
 * <p>This class must not touch any Minecraft classes: the mixin plugin loads it before the game is set up.
 */
public final class BeyondBorderConfig {
	public static final double DEFAULT_LIMIT = 1.0E12;
	/** Keeps the limit finite so border maths never produces Infinity. */
	private static final double MAX_LIMIT = 1.0E300;

	private static final Logger LOGGER = LoggerFactory.getLogger("BeyondBorder");
	private static BeyondBorderConfig instance;

	private final double limit;
	private final Set<String> disabledMixins;

	private BeyondBorderConfig(double limit, Set<String> disabledMixins) {
		this.limit = limit;
		this.disabledMixins = disabledMixins;
	}

	public static synchronized BeyondBorderConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	/** Half-width of the playable square, in blocks. The world border and all horizontal clamps use this. */
	public double limit() {
		return limit;
	}

	/** True if the mixin with this simple class name (e.g. "EntityMixin") was switched off in the config. */
	public boolean isMixinDisabled(String simpleName) {
		return disabledMixins.contains(simpleName);
	}

	private static BeyondBorderConfig load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("beyondborder.json");
		double limit = DEFAULT_LIMIT;
		Set<String> disabled = new HashSet<>();

		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

				JsonElement limitElement = root.get("limit");
				if (limitElement != null && limitElement.isJsonPrimitive()) {
					double parsed = limitElement.getAsDouble();
					if (Double.isFinite(parsed) && parsed >= 1.0) {
						limit = Math.min(parsed, MAX_LIMIT);
					} else {
						LOGGER.warn("Ignoring invalid 'limit' ({}) in {}; using {}", parsed, file.getFileName(), limit);
					}
				}

				JsonElement disabledElement = root.get("disabledMixins");
				if (disabledElement != null && disabledElement.isJsonArray()) {
					for (JsonElement entry : disabledElement.getAsJsonArray()) {
						disabled.add(entry.getAsString());
					}
				}
			} catch (Exception e) {
				LOGGER.error("Could not read {}; using defaults", file, e);
			}
		} else {
			writeDefaults(file);
		}

		return new BeyondBorderConfig(limit, disabled);
	}

	private static void writeDefaults(Path file) {
		JsonObject root = new JsonObject();
		root.addProperty("_readme",
			"limit = half-width of the playable square in blocks (border + coordinate clamps). "
				+ "Vanilla is about 3.0E7. Interesting thresholds: 3.3554432E7 (BlockPos/chunk-section packing wraps), "
				+ "2.147483647E9 (int overflow), 9.0E15 (doubles can no longer resolve a 1-block step). "
				+ "disabledMixins = simple class names of mixins to switch off if one fails to apply, "
				+ "e.g. [\"EntityMixin\"]. Restart the game after editing.");
		root.addProperty("limit", DEFAULT_LIMIT);
		root.add("disabledMixins", new JsonArray());

		try {
			Files.createDirectories(file.getParent());
			String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root);
			Files.writeString(file, json + System.lineSeparator(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("Could not write default config to {}", file, e);
		}
	}
}
