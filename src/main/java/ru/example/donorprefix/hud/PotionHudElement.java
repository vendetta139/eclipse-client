package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.VisualConfig;

import java.util.Collection;

/**
 * HUD-виджет Potion HUD:
 * Отображает активные эффекты зелий с цветными маркерами и обратным таймером.
 */
public class PotionHudElement extends HudElement {

    public static final int BOX_WIDTH = 104;
    public static final int ITEM_HEIGHT = 20;
    public static final int TOTAL_HEIGHT = (ITEM_HEIGHT + 4) * 2; // 44

    public PotionHudElement() {
        super("potionhud", "Potion HUD", BOX_WIDTH, TOTAL_HEIGHT);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();

        Collection<StatusEffectInstance> effects = client.player != null ? client.player.getStatusEffects() : null;
        boolean hasEffects = effects != null && !effects.isEmpty();

        if (hasEffects) {
            int curY = 0;
            int count = 0;
            for (StatusEffectInstance effect : effects) {
                if (count >= 2) break; // вмещаем в базовый размер
                StatusEffect type = effect.getEffectType().value();
                String name = type.getName().getString();
                int amp = effect.getAmplifier();
                if (amp > 0) {
                    name += " " + (amp + 1);
                }

                String duration = formatDuration(effect);
                int color = type.getColor();

                drawPotionBadge(context, client, 0, curY, name, duration, color);
                curY += ITEM_HEIGHT + 4;
                count++;
            }
        } else {
            // Превью-эффекты
            drawPotionBadge(context, client, 0, 0, "Скорость II", "02:45", 0x33EBFF);
            drawPotionBadge(context, client, 0, ITEM_HEIGHT + 4, "Сила II", "01:18", 0x990000);
        }
    }

    private String formatDuration(StatusEffectInstance effect) {
        if (effect.isInfinite()) return "**:**";
        int durationTicks = effect.getDuration();
        int totalSeconds = Math.max(0, durationTicks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void drawPotionBadge(DrawContext context, MinecraftClient client, int x, int y, String name, String duration, int effectColor) {
        VisualConfig cfg = VisualConfig.getInstance();
        int glowColor = (0x66 << 24) | (effectColor & 0x00FFFFFF);

        RenderUtils.drawPill(context, x, y, BOX_WIDTH, ITEM_HEIGHT, cfg.themeBgColor, glowColor);
        RenderUtils.drawRoundedRect(context, x + 6, y + 6, 8, 8, 4, (0xFF << 24) | (effectColor & 0x00FFFFFF));

        RenderUtils.drawTextWithShadow(context, client.textRenderer, Text.literal(name).formatted(Formatting.WHITE), x + 18, y + 3, cfg.themeTextColor);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, Text.literal(duration).formatted(Formatting.GRAY), x + 18, y + 11, cfg.themeSubtextColor);
    }

    @Override
    public void loadConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        this.x = cfg.potionHudX;
        this.y = cfg.potionHudY;
        this.scale = cfg.potionHudScale > 0.2f ? cfg.potionHudScale : 1.0f;
        this.enabled = cfg.potionHudEnabled;
    }

    @Override
    public void saveConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        cfg.potionHudX = this.x;
        cfg.potionHudY = this.y;
        cfg.potionHudScale = this.scale;
        cfg.potionHudEnabled = this.enabled;
    }

    @Override
    public void resetDefaultPosition(int screenWidth, int screenHeight) {
        this.x = Math.max(2, screenWidth - this.width - 12);
        this.y = 110.0f;
        this.scale = 1.0f;
    }
}
