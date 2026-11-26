package ru.eclipse.client.core;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.ui.clickgui.EclipseClickGui;
import ru.example.donorprefix.ClientPrefixConfig;
import ru.example.donorprefix.DonorPrefixClientMod;
import ru.example.donorprefix.hud.HudManager;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * EclipseClient — Главное ядро клиента ECLIPSE CLIENT 1.21.11.
 * Разработчик: alone.vendetta
 */
public final class EclipseClient implements ClientModInitializer {
    public static final String MOD_ID = "eclipse-client";
    public static final String CLIENT_NAME = "ECLIPSE CLIENT";
    public static final String VERSION = "1.21.11";
    public static final String AUTHOR = "alone.vendetta";
    public static final String WINDOW_TITLE = "ECLIPSE CLIENT 1.21.11 | by alone.vendetta";
    public static final String BRAND = "eclipse";

    public static final Logger LOGGER = LoggerFactory.getLogger(CLIENT_NAME);
    private static EclipseClient instance;

    public static EclipseClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("==========================================================");
        LOGGER.info("Запуск ядра {} v{} | by {}", CLIENT_NAME, VERSION, AUTHOR);
        LOGGER.info("==========================================================");

        VisualConfig.loadOrCreate();
        ClientPrefixConfig.loadOrCreate();
        ModuleManager.getInstance().init();
        HudManager.getInstance().loadConfigs();
        EclipseStateSync.getInstance().init();

        DonorPrefixClientMod.initFromCore();
        UpdateChecker.registerChatNotifier();
        UpdateChecker.checkAsync();

        LOGGER.info("{} успешно инициализирован под Minecraft 1.21.11!", CLIENT_NAME);
    }

    public static void onKey(int key, int action) {
        if (action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.currentScreen == null) {
                    mc.setScreen(new EclipseClickGui());
                }
            }
            ModuleManager.getInstance().onKeyPress(key, action);
        }
    }
}
