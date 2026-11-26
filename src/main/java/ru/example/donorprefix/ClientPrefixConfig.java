package ru.example.donorprefix;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import ru.eclipse.client.core.EclipseStateSync;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Конфиг префиксов — сохраняется в единый файл config/eclipse-client.json.
 */
public class ClientPrefixConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path LEGACY_PATH = FabricLoader.getInstance().getConfigDir().resolve("donor-prefix-client.json");
    private static ClientPrefixConfig INSTANCE;

    /** Шаблон команды. %s заменяется на текущий префикс из списка ниже (без ведущего "/"). */
    public String commandTemplate = "prefix set %s";

    /** Включена ли автоматическая ротация префиксов. */
    public boolean enabled = true;

    /** Через сколько секунд переключать на следующий префикс. */
    public int cooldownSeconds = 60;

    /**
     * Список префиксов по кругу.
     */
    public List<String> prefixes = new ArrayList<>(List.of(
            "&f&lovendetta_",
            "&6&lVIP_",
            "&b&lStar_"
    ));

    public static ClientPrefixConfig getCurrentOrNull() {
        return INSTANCE;
    }

    public static ClientPrefixConfig loadOrCreate() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        // 1. Чтение из единого config/eclipse-client.json
        JsonObject root = EclipseStateSync.getInstance().readRootIfExists();
        if (root != null && root.has("prefixes") && root.get("prefixes").isJsonObject()) {
            ClientPrefixConfig cfg = new ClientPrefixConfig();
            INSTANCE = cfg;
            applyFromJson(root.getAsJsonObject("prefixes"));
            return cfg;
        }

        // 2. Миграция из старого config/donor-prefix-client.json, если существует
        if (Files.exists(LEGACY_PATH)) {
            try (Reader reader = Files.newBufferedReader(LEGACY_PATH, StandardCharsets.UTF_8)) {
                ClientPrefixConfig config = GSON.fromJson(reader, ClientPrefixConfig.class);
                if (config != null) {
                    if (config.prefixes == null) config.prefixes = new ArrayList<>();
                    INSTANCE = config;
                    return config;
                }
            } catch (IOException e) {
                DonorPrefixClientMod.LOGGER.error("Не удалось прочитать legacy donor-prefix-client.json", e);
            }
        }

        ClientPrefixConfig defaultConfig = new ClientPrefixConfig();
        INSTANCE = defaultConfig;
        defaultConfig.save();
        return defaultConfig;
    }

    public static void applyFromJson(JsonObject prefixObj) {
        if (prefixObj == null) return;
        ClientPrefixConfig cfg = INSTANCE != null ? INSTANCE : new ClientPrefixConfig();
        INSTANCE = cfg;

        if (prefixObj.has("enabled")) {
            cfg.enabled = prefixObj.get("enabled").getAsBoolean();
        } else if (prefixObj.has("rotationEnabled")) {
            cfg.enabled = prefixObj.get("rotationEnabled").getAsBoolean();
        }
        if (prefixObj.has("cooldownSeconds")) {
            cfg.cooldownSeconds = Math.max(1, prefixObj.get("cooldownSeconds").getAsInt());
        }
        if (prefixObj.has("commandTemplate")) {
            cfg.commandTemplate = prefixObj.get("commandTemplate").getAsString();
        }
        JsonArray arr = null;
        if (prefixObj.has("prefixes") && prefixObj.get("prefixes").isJsonArray()) {
            arr = prefixObj.getAsJsonArray("prefixes");
        } else if (prefixObj.has("list") && prefixObj.get("list").isJsonArray()) {
            arr = prefixObj.getAsJsonArray("list");
        }
        if (arr != null) {
            List<String> loaded = new ArrayList<>();
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject item = el.getAsJsonObject();
                    boolean active = !item.has("active") || item.get("active").getAsBoolean();
                    if (active && item.has("text")) {
                        loaded.add(item.get("text").getAsString());
                    }
                } else if (el.isJsonPrimitive()) {
                    loaded.add(el.getAsString());
                }
            }
            if (!loaded.isEmpty()) {
                cfg.prefixes = loaded;
            }
        }
    }

    public void save() {
        INSTANCE = this;
        EclipseStateSync.getInstance().saveStateDebounced();
    }
}
