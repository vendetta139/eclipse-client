package ru.example.donorprefix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.impl.AutoPrefixModule;
import ru.eclipse.client.module.impl.TargetHUDModule;
import ru.example.donorprefix.visual.TargetHUDManager;
import ru.example.donorprefix.visual.VisualConfig;

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

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "eclipse"));
        openMenuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.donor-prefix-client.open_menu", GLFW.GLFW_KEY_P, category)
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> mod.tickCounter = 0);
        ClientTickEvents.END_CLIENT_TICK.register(mod::onTick);
    }

    private void onTick(MinecraftClient client) {
        try {
            if (this.config == null) {
                this.config = ClientPrefixConfig.loadOrCreate();
            }

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

            TargetHUDModule targetHud = ModuleManager.TARGET_HUD;
            if (targetHud != null && targetHud.isEnabled()
                    && client.targetedEntity instanceof net.minecraft.entity.LivingEntity living
                    && living.isAlive() && living != client.player) {
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

                AutoPrefixModule autoPrefix = ModuleManager.AUTO_PREFIX;
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
            String command = (template != null && template.contains("%s"))
                    ? template.replace("%s", prefixValue)
                    : (template + " " + prefixValue);
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (client.player != null && client.player.networkHandler != null) {
                client.player.networkHandler.sendChatCommand(command);
                return true;
            } else if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendChatCommand(command);
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
