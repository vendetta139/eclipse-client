package ru.example.donorprefix.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

/**
 * Интерактивная палитра выбора цветов (Spectrum Color Picker):
 * - Поле выбора насыщенности и яркости (Saturation & Value Box)
 * - Полоса спектра оттенков (Hue Slider)
 * - Полоса прозрачности (Alpha Slider)
 * - Режим переливающейся радуги (Rainbow Mode)
 * - Быстрые пресеты популярных неоновых цветов
 * - Живой предпросмотр и Hex-код
 */
public class ColorPickerModal {

    @FunctionalInterface
    public interface ColorCallback {
        void onColorChanged(int newColor, boolean isRainbow, float alpha);
    }

    private boolean visible = false;
    private String title = "Выбор цвета";
    private int x = 0;
    private int y = 0;
    private final int width = 178;
    private int height = 210;

    private float hue = 0f;
    private float saturation = 1f;
    private float value = 1f;
    private float alpha = 1f;
    private boolean rainbow = false;
    private boolean hasAlpha = true;

    private ColorCallback callback;

    private enum DragMode {
        NONE, SV_BOX, HUE, ALPHA
    }

    private DragMode dragMode = DragMode.NONE;

    // Предустановленные быстрые цвета
    private static final int[] PRESETS = {
            0xFFFF0055, // Неоновый розовый
            0xFFFF2222, // Алый
            0xFF9900FF, // Пурпурный
            0xFF00E5FF, // Циан
            0xFF00FF88, // Неоновая мята
            0xFFFFD700, // Золото
            0xFFFFFFFF  // Белый
    };

    public void open(String title, int initialColor, boolean isRainbow, float initialAlpha, boolean hasAlpha, int anchorX, int anchorY, ColorCallback callback) {
        this.title = title;
        this.rainbow = isRainbow;
        this.alpha = MathHelper.clamp(initialAlpha, 0f, 1f);
        this.hasAlpha = hasAlpha;
        this.height = hasAlpha ? 212 : 192;
        this.callback = callback;

        float[] hsv = new float[3];
        RenderUtils.rgbToHsv(initialColor, hsv);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        // Позиционируем модалку около кнопки-якоря
        MinecraftClient client = MinecraftClient.getInstance();
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        this.x = MathHelper.clamp(anchorX - width / 2, 8, screenW - width - 8);
        this.y = MathHelper.clamp(anchorY + 8, 8, screenH - height - 8);
        if (this.y + height > screenH - 8) {
            this.y = Math.max(8, anchorY - height - 8);
        }

        this.visible = true;
        this.dragMode = DragMode.NONE;
    }

    public void close() {
        this.visible = false;
        this.dragMode = DragMode.NONE;
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(DrawContext context, MinecraftClient client, int mouseX, int mouseY) {
        if (!visible || context == null) return;
        try {
            // 1. Плоская подложка #09090B и тонкая 1px рамка #24242A
            RenderUtils.drawFlatCard(context, x, y, width, height, EclipseColors.BG_WINDOW_TRANSLUCENT, EclipseColors.BORDER);

            // 2. Заголовок и кнопка закрытия [✕]
            RenderUtils.drawTextWithShadow(context, client.textRenderer, Text.literal(title).formatted(Formatting.BOLD), x + 10, y + 8, EclipseColors.TEXT_PRIMARY);
            int closeBtnX = x + width - 18;
            int closeBtnY = y + 7;
            boolean closeHovered = mouseX >= closeBtnX && mouseX <= closeBtnX + 12 && mouseY >= closeBtnY && mouseY <= closeBtnY + 12;
            RenderUtils.drawTextWithShadow(context, client.textRenderer, Text.literal("✕"), closeBtnX, closeBtnY,
                    closeHovered ? EclipseColors.TEXT_PRIMARY : EclipseColors.TEXT_SECONDARY);

        int curY = y + 23;
        int innerW = width - 20;
        int innerX = x + 10;

        // 3. Saturation & Value Box (размер innerW x 70)
        int svBoxH = 68;
        RenderUtils.drawSaturationValueBox(context, innerX, curY, innerW, svBoxH, hue);

        // Индикатор выбора (круглый прицел)
        int crosshairX = (int) (innerX + saturation * (innerW - 1));
        int crosshairY = (int) (curY + (1f - value) * (svBoxH - 1));
        RenderUtils.drawRoundedOutline(context, crosshairX - 3, crosshairY - 3, 7, 7, 3, 0xFFFFFFFF);
        RenderUtils.drawRoundedOutline(context, crosshairX - 4, crosshairY - 4, 9, 9, 4, 0xFF000000);

        curY += svBoxH + 6;

        // 4. Hue Bar
        int hueH = 10;
        RenderUtils.drawHueBar(context, innerX, curY, innerW, hueH);

        // Курсор оттенка
        int hueCursorX = (int) (innerX + (hue / 360f) * (innerW - 1));
        context.fill(hueCursorX - 1, curY - 1, hueCursorX + 2, curY + hueH + 1, 0xFFFFFFFF);
        RenderUtils.drawBorder(context, hueCursorX - 2, curY - 2, 4, hueH + 4, 0xFF000000);

        curY += hueH + 6;

        // 5. Alpha Bar (если поддерживается)
        int currentRgb = RenderUtils.hsvToRgb(hue, saturation, value);
        if (hasAlpha) {
            int alphaH = 8;
            int transparentCol = currentRgb & 0x00FFFFFF;
            int fullCol = (0xFF << 24) | (currentRgb & 0x00FFFFFF);
            context.fillGradient(innerX, curY, innerX + innerW, curY + alphaH, transparentCol, fullCol);
            RenderUtils.drawRoundedOutline(context, innerX, curY, innerW, alphaH, 2, 0x44FFFFFF);

            int alphaCursorX = (int) (innerX + alpha * (innerW - 1));
            context.fill(alphaCursorX - 1, curY - 1, alphaCursorX + 2, curY + alphaH + 1, 0xFFFFFFFF);
            RenderUtils.drawBorder(context, alphaCursorX - 2, curY - 2, 4, alphaH + 4, 0xFF000000);

            curY += alphaH + 6;
        }

        // 6. Быстрые пресеты (точки)
        int dotSize = 13;
        int dotSpacing = 4;
        int dotsStartX = innerX;
        for (int pCol : PRESETS) {
            boolean active = !rainbow && ((currentRgb & 0x00FFFFFF) == (pCol & 0x00FFFFFF));
            RenderUtils.drawFlatCard(context, dotsStartX, curY, dotSize, dotSize, pCol,
                    active ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER);
            dotsStartX += dotSize + dotSpacing;
        }

        // Кнопка [Радуга]
        int rainbowBtnW = 44;
        int rainbowBtnX = innerX + innerW - rainbowBtnW;
        int rainbowBg = rainbow ? EclipseColors.ACCENT_WHITE : EclipseColors.TRACK_BG;
        int rainbowBorder = rainbow ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
        RenderUtils.drawFlatCard(context, rainbowBtnX, curY, rainbowBtnW, dotSize + 1, rainbowBg, rainbowBorder);
        RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, Text.literal("Радуга"), rainbowBtnX + rainbowBtnW / 2, curY + 3,
                rainbow ? EclipseColors.TEXT_ON_ACCENT : EclipseColors.TEXT_SECONDARY);

        curY += dotSize + 8;

        // 7. Нижняя плашка: Превью цвета + Hex код + Кнопка Готово
        int previewSize = 16;
        int effectiveRgb = rainbow ? RenderUtils.lerpColor(0xFFFFFFFF, 0xFF71717A, (float) ((System.currentTimeMillis() % 2000L) / 2000.0)) : currentRgb;
        int previewCol = ((int) (alpha * 255f) << 24) | (effectiveRgb & 0x00FFFFFF);

        RenderUtils.drawFlatCard(context, innerX, curY, previewSize, previewSize, previewCol, EclipseColors.BORDER);

        String hexText = String.format("#%06X", effectiveRgb & 0x00FFFFFF);
        if (hasAlpha) {
            hexText = String.format("#%02X%06X", (int) (alpha * 255f), effectiveRgb & 0x00FFFFFF);
        }
        RenderUtils.drawTextWithShadow(context, client.textRenderer, Text.literal(hexText), innerX + previewSize + 6, curY + 4, EclipseColors.TEXT_PRIMARY);

        // Кнопка [Готово]
        int doneBtnW = 48;
        int doneBtnH = 16;
        int doneBtnX = innerX + innerW - doneBtnW;
        RenderUtils.drawFlatCard(context, doneBtnX, curY, doneBtnW, doneBtnH, EclipseColors.ACCENT_WHITE, EclipseColors.BORDER_ACTIVE);
        RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, Text.literal("Готово"), doneBtnX + doneBtnW / 2, curY + 4, EclipseColors.TEXT_ON_ACCENT);
        } catch (Throwable ignored) {}
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        // Клик вне окна модалки закрывает её
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            close();
            return true;
        }

        // Кнопка [✕]
        if (mouseX >= x + width - 20 && mouseX <= x + width - 4 && mouseY >= y + 4 && mouseY <= y + 20) {
            close();
            return true;
        }

        int innerW = width - 20;
        int innerX = x + 10;
        int curY = y + 23;

        // 1. SV Box
        int svBoxH = 68;
        if (mouseX >= innerX && mouseX <= innerX + innerW && mouseY >= curY && mouseY <= curY + svBoxH) {
            dragMode = DragMode.SV_BOX;
            updateSVFromMouse(mouseX, mouseY, innerX, curY, innerW, svBoxH);
            rainbow = false;
            notifyCallback();
            return true;
        }

        curY += svBoxH + 6;

        // 2. Hue Bar
        int hueH = 10;
        if (mouseX >= innerX && mouseX <= innerX + innerW && mouseY >= curY && mouseY <= curY + hueH) {
            dragMode = DragMode.HUE;
            updateHueFromMouse(mouseX, innerX, innerW);
            rainbow = false;
            notifyCallback();
            return true;
        }

        curY += hueH + 6;

        // 3. Alpha Bar
        if (hasAlpha) {
            int alphaH = 8;
            if (mouseX >= innerX && mouseX <= innerX + innerW && mouseY >= curY && mouseY <= curY + alphaH) {
                dragMode = DragMode.ALPHA;
                updateAlphaFromMouse(mouseX, innerX, innerW);
                notifyCallback();
                return true;
            }
            curY += alphaH + 6;
        }

        // 4. Пресеты
        int dotSize = 13;
        int dotSpacing = 4;
        int dotsStartX = innerX;
        for (int pCol : PRESETS) {
            if (mouseX >= dotsStartX && mouseX <= dotsStartX + dotSize && mouseY >= curY && mouseY <= curY + dotSize) {
                float[] hsv = new float[3];
                RenderUtils.rgbToHsv(pCol, hsv);
                this.hue = hsv[0];
                this.saturation = hsv[1];
                this.value = hsv[2];
                this.rainbow = false;
                notifyCallback();
                return true;
            }
            dotsStartX += dotSize + dotSpacing;
        }

        // Кнопка [Радуга]
        int rainbowBtnW = 44;
        int rainbowBtnX = innerX + innerW - rainbowBtnW;
        if (mouseX >= rainbowBtnX && mouseX <= rainbowBtnX + rainbowBtnW && mouseY >= curY && mouseY <= curY + dotSize + 1) {
            rainbow = !rainbow;
            notifyCallback();
            return true;
        }

        curY += dotSize + 8;

        // Кнопка [Готово]
        int doneBtnW = 48;
        int doneBtnH = 16;
        int doneBtnX = innerX + innerW - doneBtnW;
        if (mouseX >= doneBtnX && mouseX <= doneBtnX + doneBtnW && mouseY >= curY && mouseY <= curY + doneBtnH) {
            close();
            return true;
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible || dragMode == DragMode.NONE) return false;

        int innerW = width - 20;
        int innerX = x + 10;
        int curY = y + 23;
        int svBoxH = 68;

        if (dragMode == DragMode.SV_BOX) {
            updateSVFromMouse(mouseX, mouseY, innerX, curY, innerW, svBoxH);
            rainbow = false;
            notifyCallback();
            return true;
        }

        curY += svBoxH + 6;
        int hueH = 10;
        if (dragMode == DragMode.HUE) {
            updateHueFromMouse(mouseX, innerX, innerW);
            rainbow = false;
            notifyCallback();
            return true;
        }

        curY += hueH + 6;
        if (hasAlpha && dragMode == DragMode.ALPHA) {
            updateAlphaFromMouse(mouseX, innerX, innerW);
            notifyCallback();
            return true;
        }

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragMode != DragMode.NONE) {
            dragMode = DragMode.NONE;
            return true;
        }
        return false;
    }

    private void updateSVFromMouse(double mouseX, double mouseY, int boxX, int boxY, int boxW, int boxH) {
        this.saturation = MathHelper.clamp((float) (mouseX - boxX) / (float) (boxW - 1), 0f, 1f);
        this.value = MathHelper.clamp(1f - (float) (mouseY - boxY) / (float) (boxH - 1), 0f, 1f);
    }

    private void updateHueFromMouse(double mouseX, int barX, int barW) {
        float ratio = MathHelper.clamp((float) (mouseX - barX) / (float) (barW - 1), 0f, 1f);
        this.hue = ratio * 360f;
    }

    private void updateAlphaFromMouse(double mouseX, int barX, int barW) {
        this.alpha = MathHelper.clamp((float) (mouseX - barX) / (float) (barW - 1), 0f, 1f);
    }

    private void notifyCallback() {
        if (callback != null) {
            int rgb = RenderUtils.hsvToRgb(hue, saturation, value);
            int argb = ((int) (alpha * 255f) << 24) | (rgb & 0x00FFFFFF);
            callback.onColorChanged(argb, rainbow, alpha);
        }
    }
}
