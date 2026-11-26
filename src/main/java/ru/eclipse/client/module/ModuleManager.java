package ru.eclipse.client.module;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import ru.eclipse.client.module.impl.*;
import ru.example.donorprefix.ClientPrefixConfig;
import ru.example.donorprefix.visual.VisualConfig;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ModuleManager — Центральный менеджер рабочих модулей ECLIPSE CLIENT.
 */
public final class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();

    // Прямые ссылки для O(1) проверки в миксинах без поиска в Map
    public static volatile AutoPrefixModule AUTO_PREFIX;
    public static volatile HitMarkerModule HIT_MARKER;
    public static volatile CustomCrosshairModule CUSTOM_CROSSHAIR;
    public static volatile NoHurtCamModule NO_HURT_CAM;
    public static volatile ViewModelModule VIEW_MODEL;
    public static volatile FullbrightModule FULLBRIGHT;
    public static volatile NoFogModule NO_FOG;
    public static volatile CameraClipModule CAMERA_CLIP;
    public static volatile TimeChangerModule TIME_CHANGER;
    public static volatile WeatherChangerModule WEATHER_CHANGER;
    public static volatile TargetHUDModule TARGET_HUD;
    public static volatile LowFireModule LOW_FIRE;
    public static volatile NoOverlayModule NO_OVERLAY;

    private final List<Module> modules = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends Module>, Module> moduleClassMap = new HashMap<>();

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    private ModuleManager() {}

    public void init() {
        modules.clear();
        moduleClassMap.clear();

        // 1. Префиксы
        AUTO_PREFIX = register(new AutoPrefixModule());

        // 2. PvP & Бой
        HIT_MARKER = register(new HitMarkerModule());
        CUSTOM_CROSSHAIR = register(new CustomCrosshairModule());
        NO_HURT_CAM = register(new NoHurtCamModule());

        // 3. Анимации
        VIEW_MODEL = register(new ViewModelModule());

        // 4. Мир & Туман
        FULLBRIGHT = register(new FullbrightModule());
        NO_FOG = register(new NoFogModule());
        CAMERA_CLIP = register(new CameraClipModule());
        TIME_CHANGER = register(new TimeChangerModule());
        WEATHER_CHANGER = register(new WeatherChangerModule());

        // 5. HUD & Оверлеи
        TARGET_HUD = register(new TargetHUDModule());
        LOW_FIRE = register(new LowFireModule());
        NO_OVERLAY = register(new NoOverlayModule());

        loadConfig();
        syncModulesToConfigs();
    }

    private <T extends Module> T register(T module) {
        modules.add(module);
        moduleClassMap.put(module.getClass(), module);
        if (module.isEnabled()) {
            module.onEnable();
        }
        return module;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) moduleClassMap.get(clazz);
    }

    public Module getModuleByName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) {
                list.add(m);
            }
        }
        return list;
    }

    public void onKeyPress(int key, int action) {
        if (action != GLFW.GLFW_PRESS || key <= 0) return;

        boolean toggled = false;
        for (Module m : modules) {
            if (m.getKeybind() == key) {
                m.toggle();
                toggled = true;
            }
        }
        if (toggled) {
            saveConfig();
        }
    }

    private File getLegacyConfigFile() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), "eclipse_client_config.json");
    }

    public void syncModulesToConfigs() {
        try {
            VisualConfig vcfg = VisualConfig.getInstance();
            ClientPrefixConfig pcfg = ClientPrefixConfig.loadOrCreate();

            AutoPrefixModule autoPrefix = getModule(AutoPrefixModule.class);
            if (autoPrefix != null) {
                pcfg.enabled = autoPrefix.isEnabled();
                pcfg.cooldownSeconds = Math.max(1, autoPrefix.getInterval().getIntValue());
            }

            FullbrightModule fullbright = getModule(FullbrightModule.class);
            if (fullbright != null) vcfg.fullbright = fullbright.isEnabled();

            NoFogModule noFog = getModule(NoFogModule.class);
            if (noFog != null) vcfg.noFog = noFog.isEnabled();

            LowFireModule lowFire = getModule(LowFireModule.class);
            if (lowFire != null) {
                vcfg.lowFireEnabled = lowFire.isEnabled();
                vcfg.lowFireOffset = lowFire.getHeight().getFloatValue();
            }

            NoHurtCamModule noHurtCam = getModule(NoHurtCamModule.class);
            if (noHurtCam != null) vcfg.noHurtCam = noHurtCam.isEnabled();

            TimeChangerModule timeChanger = getModule(TimeChangerModule.class);
            if (timeChanger != null) {
                vcfg.timeChangerEnabled = timeChanger.isEnabled();
                long fixed = timeChanger.getFixedTime();
                if (fixed >= 0) vcfg.customTime = fixed;
            }

            WeatherChangerModule weatherChanger = getModule(WeatherChangerModule.class);
            if (weatherChanger != null) {
                vcfg.clearWeather = weatherChanger.isEnabled() && weatherChanger.getMode().is("Clear");
            }

            HitMarkerModule hitMarker = getModule(HitMarkerModule.class);
            if (hitMarker != null) {
                vcfg.hitMarkerEnabled = hitMarker.isEnabled();
                vcfg.hitSoundEnabled = hitMarker.isEnabled() && hitMarker.getSound().isEnabled();
                String st = hitMarker.getSoundType().getValue();
                vcfg.hitSoundType = switch (st) {
                    case "Skeet" -> 1;
                    case "Chime" -> 2;
                    case "Arrow" -> 3;
                    case "Bonk" -> 4;
                    default -> 0;
                };
            }

            ViewModelModule vm = getModule(ViewModelModule.class);
            if (vm != null) {
                vcfg.blockHit = vm.isEnabled() && vm.getBlockHit().isEnabled();
                vcfg.smallShield = vm.isEnabled() && vm.getLowShield().isEnabled();
            }

            TargetHUDModule targetHud = getModule(TargetHUDModule.class);
            if (targetHud != null) {
                vcfg.targetHudEnabled = targetHud.isEnabled();
                vcfg.targetHud3DModel = targetHud.getShow3DModel().isEnabled();
            }

            CustomCrosshairModule crosshair = getModule(CustomCrosshairModule.class);
            if (crosshair != null) {
                vcfg.customCrosshair = crosshair.isEnabled();
                vcfg.crosshairColor = crosshair.getColor().getColor();
                vcfg.crosshairSize = Math.max(1, crosshair.getSize().getIntValue());
                vcfg.crosshairGap = Math.max(0, crosshair.getGap().getIntValue());
                vcfg.crosshairThickness = Math.max(1, crosshair.getThickness().getIntValue());
                vcfg.crosshairCenterDot = crosshair.getCenterDot().isEnabled();
                if (crosshair.getStyle().is("Dot")) vcfg.crosshairType = 0;
                else if (crosshair.getStyle().is("Cross")) vcfg.crosshairType = 1;
                else if (crosshair.getStyle().is("Circle")) vcfg.crosshairType = 2;
                else vcfg.crosshairType = 3;
            }
        } catch (Throwable ignored) {}
    }

    public void syncConfigsToModules() {
        try {
            VisualConfig vcfg = VisualConfig.getInstance();
            ClientPrefixConfig pcfg = ClientPrefixConfig.loadOrCreate();

            AutoPrefixModule autoPrefix = getModule(AutoPrefixModule.class);
            if (autoPrefix != null) {
                if (autoPrefix.isEnabled() != pcfg.enabled) autoPrefix.setEnabled(pcfg.enabled);
                autoPrefix.getInterval().setValue((double) Math.max(5, Math.min(300, pcfg.cooldownSeconds)));
            }

            FullbrightModule fullbright = getModule(FullbrightModule.class);
            if (fullbright != null && fullbright.isEnabled() != vcfg.fullbright) fullbright.setEnabled(vcfg.fullbright);

            NoFogModule noFog = getModule(NoFogModule.class);
            if (noFog != null && noFog.isEnabled() != vcfg.noFog) noFog.setEnabled(vcfg.noFog);

            LowFireModule lowFire = getModule(LowFireModule.class);
            if (lowFire != null) {
                if (lowFire.isEnabled() != vcfg.lowFireEnabled) lowFire.setEnabled(vcfg.lowFireEnabled);
                lowFire.getHeight().setValue((double) vcfg.lowFireOffset);
            }

            NoHurtCamModule noHurtCam = getModule(NoHurtCamModule.class);
            if (noHurtCam != null && noHurtCam.isEnabled() != vcfg.noHurtCam) noHurtCam.setEnabled(vcfg.noHurtCam);

            TimeChangerModule timeChanger = getModule(TimeChangerModule.class);
            if (timeChanger != null) {
                if (timeChanger.isEnabled() != vcfg.timeChangerEnabled) timeChanger.setEnabled(vcfg.timeChangerEnabled);
            }

            WeatherChangerModule weatherChanger = getModule(WeatherChangerModule.class);
            if (weatherChanger != null) {
                if (vcfg.clearWeather) {
                    if (!weatherChanger.isEnabled()) weatherChanger.setEnabled(true);
                    weatherChanger.getMode().setValue("Clear");
                } else if (weatherChanger.isEnabled() && weatherChanger.getMode().is("Clear")) {
                    weatherChanger.setEnabled(false);
                }
            }

            HitMarkerModule hitMarker = getModule(HitMarkerModule.class);
            if (hitMarker != null) {
                if (hitMarker.isEnabled() != vcfg.hitMarkerEnabled) hitMarker.setEnabled(vcfg.hitMarkerEnabled);
                hitMarker.getSound().setValue(vcfg.hitSoundEnabled);
                String[] names = {"Ding", "Skeet", "Chime", "Arrow", "Bonk"};
                if (vcfg.hitSoundType >= 0 && vcfg.hitSoundType < names.length) {
                    hitMarker.getSoundType().setValue(names[vcfg.hitSoundType]);
                }
            }

            ViewModelModule vm = getModule(ViewModelModule.class);
            if (vm != null) {
                vm.getBlockHit().setValue(vcfg.blockHit);
                vm.getLowShield().setValue(vcfg.smallShield);
                if ((vcfg.blockHit || vcfg.smallShield) && !vm.isEnabled()) {
                    vm.setEnabled(true);
                }
            }

            TargetHUDModule targetHud = getModule(TargetHUDModule.class);
            if (targetHud != null) {
                if (targetHud.isEnabled() != vcfg.targetHudEnabled) targetHud.setEnabled(vcfg.targetHudEnabled);
                targetHud.getShow3DModel().setValue(vcfg.targetHud3DModel);
            }

            CustomCrosshairModule crosshair = getModule(CustomCrosshairModule.class);
            if (crosshair != null) {
                if (crosshair.isEnabled() != vcfg.customCrosshair) crosshair.setEnabled(vcfg.customCrosshair);
                crosshair.getColor().setValue(vcfg.crosshairColor);
                crosshair.getSize().setValue((double) vcfg.crosshairSize);
                crosshair.getGap().setValue((double) vcfg.crosshairGap);
                crosshair.getThickness().setValue((double) vcfg.crosshairThickness);
                crosshair.getCenterDot().setValue(vcfg.crosshairCenterDot);
                String[] styles = {"Dot", "Cross", "Circle", "T-Shape"};
                if (vcfg.crosshairType >= 0 && vcfg.crosshairType < styles.length) {
                    crosshair.getStyle().setValue(styles[vcfg.crosshairType]);
                }
            }
        } catch (Throwable ignored) {}
    }

    public void saveConfig() {
        try {
            syncModulesToConfigs();
            ru.eclipse.client.core.EclipseStateSync.getInstance().saveStateDebounced();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void loadConfig() {
        JsonObject unifiedRoot = ru.eclipse.client.core.EclipseStateSync.getInstance().readRootIfExists();
        if (unifiedRoot != null && unifiedRoot.has("modules") && unifiedRoot.get("modules").isJsonObject()) {
            ru.eclipse.client.core.EclipseStateSync.applyModulesJson(unifiedRoot.getAsJsonObject("modules"));
            return;
        }

        File legacyFile = getLegacyConfigFile();
        if (!legacyFile.exists()) return;

        try (FileReader reader = new FileReader(legacyFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            ru.eclipse.client.core.EclipseStateSync.applyModulesJson(root);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
