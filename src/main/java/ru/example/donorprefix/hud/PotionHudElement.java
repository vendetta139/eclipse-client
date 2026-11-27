package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.example.donorprefix.gui.EclipseColors;
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

    private static final Text PREVIEW_TITLE_1 = Text.literal("Скорость II").formatted(Formatting.WHITE);
    private static final Text PREVIEW_DUR_1 = Text.literal("02:45").formatted(Formatting.GRAY);
    private static final Text PREVIEW_TITLE_2 = Text.literal("Сила II").formatted(Formatting.WHITE);
    private static final Text PREVIEW_DUR_2 = Text.literal("01:18").formatted(Formatting.GRAY);
    private static final Text INFINITE_DUR = Text.literal("**:**").formatted(Formatting.GRAY);

    private final StatusEffect[] cachedType = new StatusEffect[2];
    private final int[] cachedAmp = {Integer.MIN_VALUE, Integer.MIN_VALUE};
    private final Text[] cachedTitleText = new Text[2];
    private final int[] cachedSec = {Integer.MIN_VALUE, Integer.MIN_VALUE};
    private final Text[] cachedDurText = new Text[2];

    public PotionHudElement() {
        super("potionhud", "Potion HUD", BOX_WIDTH, TOTAL_HEIGHT);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig cfg = VisualConfig.getInstance();

        Collection<StatusEffectInstance> effects = client.player != null ? client.player.getStatusEffects() : null;
        boolean hasEffects = effects != null && !effects.isEmpty();

        if (hasEffects) {
            int curY = 0;
            int count = 0;
            for (StatusEffectInstance effect : effects) {
                if (count >= 2) break; // вмещаем в базовый размер
                StatusEffect type = effect.getEffectType().value();
                int amp = effect.getAmplifier();
                if (cachedType[count] != type || cachedAmp[count] != amp || cachedTitleText[count] == null) {
                    cachedType[count] = type;
                    cachedAmp[count] = amp;
                    String name = type.getName().getString();
                    if (amp > 0) {
                        name = name + " " + (amp + 1);
                    }
                    cachedTitleText[count] = Text.literal(name).formatted(Formatting.WHITE);
                }

                int totalSeconds = effect.isInfinite() ? -1 : Math.max(0, effect.getDuration() / 20);
                if (cachedSec[count] != totalSeconds || cachedDurText[count] == null) {
                    cachedSec[count] = totalSeconds;
                    cachedDurText[count] = totalSeconds < 0 ? INFINITE_DUR : Text.literal(formatDurationMmSs(totalSeconds)).formatted(Formatting.GRAY);
                }

                drawPotionBadge(context, client, cfg, 0, curY, cachedTitleText[count], cachedDurText[count], type.getColor());
                curY += ITEM_HEIGHT + 4;
                count++;
            }
        } else {
            // Превью-эффекты
            drawPotionBadge(context, client, cfg, 0, 0, PREVIEW_TITLE_1, PREVIEW_DUR_1, 0x33EBFF);
            drawPotionBadge(context, client, cfg, 0, ITEM_HEIGHT + 4, PREVIEW_TITLE_2, PREVIEW_DUR_2, 0x990000);
        }
    }

    private static String formatDurationMmSs(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder(5);
        if (minutes < 10) sb.append('0');
        sb.append(minutes).append(':');
        if (seconds < 10) sb.append('0');
        sb.append(seconds);
        return sb.toString();
    }

    private void drawPotionBadge(DrawContext context, MinecraftClient client, VisualConfig cfg, int x, int y, Text name, Text duration, int effectColor) {
        int bg = cfg.themePreset == 2 ? cfg.themeBgColor : EclipseColors.BG_HUD;
        int border = cfg.themePreset == 2 ? cfg.themeOutlineColor : EclipseColors.BORDER;
        RenderUtils.drawFlatCard(context, x, y, BOX_WIDTH, ITEM_HEIGHT, bg, border);
        context.fill(x + 7, y + 7, x + 13, y + 13, EclipseColors.ACCENT_WHITE);

        RenderUtils.drawTextWithShadow(context, client.textRenderer, name, x + 18, y + 3, EclipseColors.TEXT_PRIMARY);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, duration, x + 18, y + 11, EclipseColors.TEXT_MUTED_HUD);
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
