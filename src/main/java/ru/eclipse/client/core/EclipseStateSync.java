package ru.eclipse.client.core;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.setting.*;
import ru.example.donorprefix.ClientPrefixConfig;
import ru.example.donorprefix.hud.HudElement;
import ru.example.donorprefix.hud.HudManager;
import ru.example.donorprefix.visual.VisualConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Единый менеджер локальной конфигурации ECLIPSE CLIENT.
 * Хранит настройки модулей, префиксов, визуалов и позиций HUD в config/eclipse-client.json.
 */
public final class EclipseStateSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("EclipseConfig");
    private static final EclipseStateSync INSTANCE = new EclipseStateSync();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private File configFile;
    private volatile boolean initialized = false;
    private volatile boolean isSaving = false;

    public static EclipseStateSync getInstance() {
        return INSTANCE;
    }

    private EclipseStateSync() {}

    public void init() {
        resolveConfigFile();
        loadState();
        initialized = true;
    }

    private void resolveConfigFile() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File dir = configDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.configFile = new File(dir, "eclipse-client.json");
    }

    public File getConfigFile() {
        if (this.configFile == null) {
            resolveConfigFile();
        }
        return this.configFile;
    }

    public synchronized JsonObject readRootIfExists() {
        File file = getConfigFile();
        if (!file.exists()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (Throwable t) {
            LOGGER.error("Ошибка при чтении {}: {}", file.getName(), t.getMessage());
        }
        return null;
    }

    public synchronized void loadState() {
        File file = getConfigFile();
        if (!file.exists()) {
            // Однократная инициализация (при наличии старых конфигов подхватятся через loadOrCreate/loadConfig)
            saveState();
            return;
        }

        JsonObject root = readRootIfExists();
        if (root == null) return;

        try {
            // 1. Визуалы
            if (root.has("visuals") && root.get("visuals").isJsonObject()) {
                VisualConfig.applyFromJson(root.getAsJsonObject("visuals"));
            }

            // 2. Модули и их настройки
            if (root.has("modules") && root.get("modules").isJsonObject()) {
                JsonObject modulesObj = root.getAsJsonObject("modules");
                applyModulesJson(modulesObj);
            }

            // 3. Префиксы
            if (root.has("prefixes") && root.get("prefixes").isJsonObject()) {
                ClientPrefixConfig.applyFromJson(root.getAsJsonObject("prefixes"));
            }

            // 4. HUD Элементы
            if (root.has("hud") && root.get("hud").isJsonObject()) {
                JsonObject hudObj = root.getAsJsonObject("hud");
                for (HudElement el : HudManager.getInstance().getElements()) {
                    JsonObject elObj = null;
                    if (hudObj.has(el.id) && hudObj.get(el.id).isJsonObject()) {
                        elObj = hudObj.getAsJsonObject(el.id);
                    } else if (hudObj.has(el.name) && hudObj.get(el.name).isJsonObject()) {
                        elObj = hudObj.getAsJsonObject(el.name);
                    }
                    if (elObj != null) {
                        if (elObj.has("enabled")) {
                            el.enabled = elObj.get("enabled").getAsBoolean();
                        }
                        if (elObj.has("x")) {
                            el.x = elObj.get("x").getAsFloat();
                        }
                        if (elObj.has("y")) {
                            el.y = elObj.get("y").getAsFloat();
                        }
                        if (elObj.has("scale")) {
                            el.scale = elObj.get("scale").getAsFloat();
                        }
                    }
                }
            }

            LOGGER.info("Конфигурация ECLIPSE загружена из {}", file.getAbsolutePath());
        } catch (Throwable t) {
            LOGGER.error("Ошибка при применении eclipse-client.json: {}", t.getMessage());
        }
    }

    public static void applyModulesJson(JsonObject modulesObj) {
        if (modulesObj == null) return;
        ModuleManager mm = ModuleManager.getInstance();
        for (Module m : mm.getModules()) {
            if (modulesObj.has(m.getName()) && modulesObj.get(m.getName()).isJsonObject()) {
                JsonObject modObj = modulesObj.getAsJsonObject(m.getName());
                if (modObj.has("enabled")) {
                    boolean targetEnabled = modObj.get("enabled").getAsBoolean();
                    if (m.isEnabled() != targetEnabled) {
                        m.setEnabled(targetEnabled);
                    }
                }
                if (modObj.has("keybind")) {
                    m.setKeybind(modObj.get("keybind").getAsInt());
                }

                JsonObject settingsSource = modObj.has("settings") && modObj.get("settings").isJsonObject()
                        ? modObj.getAsJsonObject("settings")
                        : modObj;

                for (Setting<?> s : m.getSettings()) {
                    if (settingsSource.has(s.getName())) {
                        applySettingValue(s, settingsSource.get(s.getName()));
                    }
                }
            }
        }
    }

    public synchronized void saveState() {
        if (isSaving) return;
        File file = getConfigFile();
        isSaving = true;

        try {
            JsonObject root = new JsonObject();

            // 1. Метаданные клиента
            JsonObject clientObj = new JsonObject();
            clientObj.addProperty("name", EclipseClient.CLIENT_NAME);
            clientObj.addProperty("version", EclipseClient.VERSION);
            clientObj.addProperty("author", EclipseClient.AUTHOR);
            root.add("client", clientObj);

            // 2. Модули
            JsonObject modulesObj = new JsonObject();
            for (Module m : ModuleManager.getInstance().getModules()) {
                JsonObject modObj = new JsonObject();
                modObj.addProperty("enabled", m.isEnabled());
                modObj.addProperty("keybind", m.getKeybind());

                JsonObject settingsObj = new JsonObject();
                for (Setting<?> s : m.getSettings()) {
                    if (s instanceof BooleanSetting bs) {
                        settingsObj.addProperty(bs.getName(), bs.getValue());
                    } else if (s instanceof NumberSetting ns) {
                        settingsObj.addProperty(ns.getName(), ns.getValue());
                    } else if (s instanceof ModeSetting ms) {
                        settingsObj.addProperty(ms.getName(), ms.getValue());
                    } else if (s instanceof ColorSetting cs) {
                        settingsObj.addProperty(cs.getName(), cs.getColor());
                    } else if (s instanceof StringSetting ss) {
                        settingsObj.addProperty(ss.getName(), ss.getValue());
                    }
                }
                modObj.add("settings", settingsObj);
                modulesObj.add(m.getName(), modObj);
            }
            root.add("modules", modulesObj);

            // 3. Префиксы
            ClientPrefixConfig prefixConfig = ClientPrefixConfig.getCurrentOrNull();
            if (prefixConfig != null) {
                root.add("prefixes", GSON.toJsonTree(prefixConfig));
            }

            // 4. HUD Элементы
            JsonObject hudObj = new JsonObject();
            for (HudElement el : HudManager.getInstance().getElements()) {
                JsonObject elObj = new JsonObject();
                elObj.addProperty("enabled", el.enabled);
                elObj.addProperty("x", el.x);
                elObj.addProperty("y", el.y);
                elObj.addProperty("scale", el.scale);
                hudObj.add(el.id, elObj);
            }
            root.add("hud", hudObj);

            // 5. Визуальные настройки
            VisualConfig visualConfig = VisualConfig.getCurrentOrNull();
            if (visualConfig != null) {
                root.add("visuals", GSON.toJsonTree(visualConfig));
            }

            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Throwable t) {
            LOGGER.error("Ошибка при сохранении eclipse-client.json: {}", t.getMessage());
        } finally {
            isSaving = false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private static void applySettingValue(Setting<?> s, JsonElement valElem) {
        try {
            if (s instanceof BooleanSetting bs) {
                bs.setValue(valElem.getAsBoolean());
            } else if (s instanceof NumberSetting ns) {
                ns.setValue(valElem.getAsDouble());
            } else if (s instanceof ModeSetting ms) {
                ms.setValue(valElem.getAsString());
            } else if (s instanceof ColorSetting cs) {
                cs.setValue(valElem.getAsInt());
            } else if (s instanceof StringSetting ss) {
                ss.setValue(valElem.getAsString());
            }
        } catch (Throwable ignored) {}
    }
}
