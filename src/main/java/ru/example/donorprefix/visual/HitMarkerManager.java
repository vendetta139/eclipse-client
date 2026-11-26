package ru.example.donorprefix.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Менеджер боевых хитмаркеров на 1.21.11:
 * воспроизводит звук попадания (PositionedSoundInstance.ui)
 * и рисует индикатор "X" вокруг прицела.
 */
public class HitMarkerManager {
    private static long lastHitTime = 0;
    private static final long DURATION_MS = 380;

    public static void triggerHit() {
        lastHitTime = System.currentTimeMillis();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        VisualConfig cfg = VisualConfig.getInstance();
        if (!cfg.hitSoundEnabled) return;

        SoundEvent sound = switch (cfg.hitSoundType) {
            case 1 -> SoundEvents.BLOCK_BELL_USE;
            case 2 -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case 3 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case 4 -> SoundEvents.BLOCK_ANVIL_LAND;
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };

        float pitch = (cfg.hitSoundType == 0) ? 2.0f : (cfg.hitSoundType == 4 ? 1.8f : 1.5f);
        float volume = (cfg.hitSoundType == 4) ? 0.65f : 0.95f;

        try {
            if (client.getSoundManager() != null) {
                client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(sound, pitch, volume));
            }
        } catch (Throwable t) {
            try {
                if (client.world != null) {
                    client.world.playSound(null, client.player.getX(), client.player.getY(), client.player.getZ(), sound, net.minecraft.sound.SoundCategory.PLAYERS, volume, pitch);
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void render(DrawContext context, MinecraftClient client) {
        if (!VisualConfig.getInstance().hitMarkerEnabled || client.getWindow() == null) return;
        long elapsed = System.currentTimeMillis() - lastHitTime;
        if (elapsed >= DURATION_MS) return;

        float alphaProgress = 1.0f - ((float) elapsed / (float) DURATION_MS);
        int alpha = (int) (alphaProgress * 255.0f);
        if (alpha <= 6) return;

        int color = (alpha << 24) | 0xFFFFFF;
        int border = ((alpha / 2) << 24) | 0x000000;

        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;

        for (int i = 3; i <= 9; i++) {
            context.fill(cx - i - 1, cy - i - 1, cx - i + 2, cy - i + 2, border);
            context.fill(cx + i - 1, cy - i - 1, cx + i + 2, cy - i + 2, border);
            context.fill(cx - i - 1, cy + i - 1, cx - i + 2, cy + i + 2, border);
            context.fill(cx + i - 1, cy + i - 1, cx + i + 2, cy + i + 2, border);
        }
        for (int i = 3; i <= 8; i++) {
            context.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, color);
            context.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, color);
            context.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, color);
            context.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
        }
    }
}
