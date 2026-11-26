package ru.example.donorprefix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.AutoPrefixModule;
import ru.example.donorprefix.visual.TargetHUDManager;
import ru.example.donorprefix.visual.VisualConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * Модуль авто-ротации донат-префиксов и открытия меню префиксов (клавиша P).
 */
public class DonorPrefixClientMod implements ClientModInitializer {
    public static final String MOD_ID = "donor-prefix-client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Random RANDOM = new Random();

    private static DonorPrefixClientMod INSTANCE;

    private ClientPrefixConfig config;
    private int tickCounter = 0;
    private int prefixIndex = 0;
    private static KeyBinding openMenuKey;
    private boolean pKeyHeld = false;

    public static DonorPrefixClientMod getInstance() {
        return INSTANCE;
    }

    public ClientPrefixConfig getConfig() {
        return config;
    }

    public int getSecondsUntilNext() {
        if (config == null || !config.enabled || config.prefixes.isEmpty()) {
            return -1;
        }
        int cooldownTicks = Math.max(1, config.cooldownSeconds) * 20;
        int remainingTicks = Math.max(0, cooldownTicks - tickCounter);
        return (remainingTicks + 19) / 20;
    }

    public int getCurrentPrefixIndex() {
        return prefixIndex;
    }

    public void resetTimer() {
        this.tickCounter = 0;
    }

    @Override
    public void onInitializeClient() {
        initFromCore();
    }

    public static void initFromCore() {
        if (INSTANCE != null) {
            return;
        }
        DonorPrefixClientMod mod = new DonorPrefixClientMod();
        INSTANCE = mod;
        mod.config = ClientPrefixConfig.loadOrCreate();
        VisualConfig.loadOrCreate();
        LOGGER.info("[EclipseClient] Загружено {} префиксов, кулдаун {} сек.",
                mod.config.prefixes.size(), mod.config.cooldownSeconds);

        KeyBinding kb = createKeyBinding("key.donor-prefix-client.open_menu", GLFW.GLFW_KEY_P, "key.categories.donor-prefix-client");
        if (kb != null) {
            try {
                openMenuKey = KeyBindingHelper.registerKeyBinding(kb);
            } catch (Throwable t) {
                LOGGER.warn("[EclipseClient] KeyBindingHelper failed, using GLFW fallback for P", t);
                openMenuKey = kb;
            }
        }

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> mod.tickCounter = 0);
        ClientTickEvents.END_CLIENT_TICK.register(mod::onTick);
    }

    private static Object createCategoryArg(Class<?> categoryClass, String categoryName) {
        if (categoryClass == String.class) {
            return categoryName;
        }
        for (Method m : categoryClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getReturnType() == categoryClass && m.getParameterCount() == 1) {
                if (m.getParameterTypes()[0] == String.class) {
                    try {
                        return m.invoke(null, categoryName);
                    } catch (Throwable ignored) {}
                }
            }
        }
        for (Field f : categoryClass.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == categoryClass) {
                try {
                    return f.get(null);
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static KeyBinding createKeyBinding(String translationKey, int defaultKey, String category) {
        try {
            for (java.lang.reflect.Constructor<?> c : KeyBinding.class.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 3 && params[0] == String.class && (params[1] == int.class || params[1] == Integer.class)) {
                    Object catArg = createCategoryArg(params[2], category);
                    if (catArg != null) {
                        try {
                            return (KeyBinding) c.newInstance(translationKey, defaultKey, catArg);
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[EclipseClient] Error creating KeyBinding", t);
        }
        return null;
    }

    private void onTick(MinecraftClient client) {
        try {
            this.config = ClientPrefixConfig.loadOrCreate();

            if (client.currentScreen == null) {
                boolean pressed = false;
                if (openMenuKey != null) {
                    while (openMenuKey.wasPressed()) {
                        pressed = true;
                    }
                }
                if (!pressed && client.getWindow() != null) {
                    long handle = client.getWindow().getHandle();
                    if (handle != 0 && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_P) == GLFW.GLFW_PRESS) {
                        if (!this.pKeyHeld) {
                            this.pKeyHeld = true;
                            pressed = true;
                        }
                    } else {
                        this.pKeyHeld = false;
                    }
                }

                if (pressed) {
                    client.setScreen(new PrefixMenuScreen(config));
                }
            }

            if (client.targetedEntity instanceof net.minecraft.entity.LivingEntity living && living.isAlive() && living != client.player) {
                TargetHUDManager.setTarget(living);
            }

            if (client.player == null || client.getNetworkHandler() == null) {
                return;
            }
            if (!config.enabled || config.prefixes.isEmpty()) {
                return;
            }

            int cooldownTicks = Math.max(1, config.cooldownSeconds) * 20;
            tickCounter++;
            if (tickCounter >= cooldownTicks) {
                tickCounter = 0;
                sendCurrentPrefixCommand(client);

                AutoPrefixModule autoPrefix = ModuleManager.getInstance().getModule(AutoPrefixModule.class);
                boolean randomOrder = autoPrefix != null && autoPrefix.getRandom().isEnabled();
                if (randomOrder && config.prefixes.size() > 1) {
                    int next = prefixIndex;
                    while (next == prefixIndex) {
                        next = RANDOM.nextInt(config.prefixes.size());
                    }
                    prefixIndex = next;
                } else {
                    prefixIndex = (prefixIndex + 1) % config.prefixes.size();
                }
            }
        } catch (Throwable ignored) {}
    }

    public static boolean sendPrefixCommand(MinecraftClient client, String template, String prefixValue) {
        if (client == null || prefixValue == null || prefixValue.trim().isEmpty()) {
            return false;
        }
        try {
            String command = String.format(template, prefixValue);
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (client.player != null && client.player.networkHandler != null) {
                client.player.networkHandler.sendChatCommand(command);
                LOGGER.info("[EclipseClient] Отправлена команда: /{}", command);
                return true;
            } else if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendChatCommand(command);
                LOGGER.info("[EclipseClient] Отправлена команда: /{}", command);
                return true;
            }
        } catch (Throwable e) {
            LOGGER.error("[EclipseClient] Ошибка при отправке команды", e);
        }
        return false;
    }

    private void sendCurrentPrefixCommand(MinecraftClient client) {
        if (prefixIndex >= config.prefixes.size()) {
            prefixIndex = 0;
        }
        String prefixValue = config.prefixes.get(prefixIndex);
        sendPrefixCommand(client, config.commandTemplate, prefixValue);
    }
}
