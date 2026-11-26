package ru.example.donorprefix.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Премиальное поле ввода в стиле Space Eclipse:
 * - Глубокий темный фон #08090D
 * - Тонкая сдержанная рамка #232533
 * - Плавная подсветка рамки активного поля (#6A67CE -> #F5A65B)
 * - Кастомный placeholder при пустом вводе (#4A4E60)
 * - Отсутствие резких белых рамок ванильного Minecraft
 */
public class EclipseTextFieldWidget extends TextFieldWidget {

    private float focusAnim = 0.0f;
    private long lastRenderTime = 0;
    private Text customPlaceholder = null;
    private boolean suppressVanillaBackground = false;

    public EclipseTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
        this.setDrawsBackground(true);
        this.setEditableColor(0xFFEAECEF);
        this.setUneditableColor(0xFF7D8296);
    }

    public void setCustomPlaceholder(Text placeholder) {
        this.customPlaceholder = placeholder;
    }

    @Override
    public boolean drawsBackground() {
        if (suppressVanillaBackground) {
            return false;
        }
        return super.drawsBackground();
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        long now = System.currentTimeMillis();
        float deltaSec = lastRenderTime > 0 ? (now - lastRenderTime) / 1000.0f : 0.016f;
        lastRenderTime = now;

        boolean focused = this.isFocused();
        this.focusAnim = AnimationUtil.deltaLerp(this.focusAnim, focused ? 1.0f : 0.0f, 14.0f, deltaSec);

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        // 1. Глубокий темный фон #08090D
        RenderUtils.drawRoundedRect(context, x, y, w, h, 4, 0xFF08090D);

        // 2. Рамка #232533 с плавной подсветкой #6A67CE при клике/фокусе
        int normalBorder = 0xFF232533;
        int activeBorder = 0xFF6A67CE;
        int borderCol = AnimationUtil.lerpColor(normalBorder, activeBorder, this.focusAnim);

        if (this.focusAnim > 0.05f) {
            RenderUtils.drawSoftGlow(context, x, y, w, h, 4, 2, RenderUtils.multiplyAlpha(activeBorder, this.focusAnim * 0.35f));
        }
        RenderUtils.drawRoundedOutline(context, x, y, w, h, 4, borderCol);

        // 3. Подсказка (Placeholder) при пустом поле
        if (this.getText().isEmpty() && !focused && customPlaceholder != null) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            RenderUtils.drawTextWithShadow(context, tr, customPlaceholder, x + 5, y + (h - 8) / 2, 0xFF4A4E60);
        }

        // 4. Отрисовка внутреннего текста с подавлением ванильной текстуры
        this.suppressVanillaBackground = true;
        try {
            super.renderWidget(context, mouseX, mouseY, delta);
        } finally {
            this.suppressVanillaBackground = false;
        }
    }
}
