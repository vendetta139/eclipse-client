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
        this.setEditableColor(EclipseColors.TEXT_PRIMARY);
        this.setUneditableColor(EclipseColors.TEXT_SECONDARY);
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

        // Плоская подложка #09090B и тонкая 1px рамка (#24242A -> #E4E4E7 при фокусе)
        int borderCol = AnimationUtil.lerpColor(EclipseColors.BORDER, EclipseColors.BORDER_ACTIVE, this.focusAnim);
        RenderUtils.drawFlatCard(context, x, y, w, h, EclipseColors.BG_WINDOW, borderCol);

        // Подсказка (Placeholder) при пустом поле
        if (this.getText().isEmpty() && !focused && customPlaceholder != null) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            RenderUtils.drawTextWithShadow(context, tr, customPlaceholder, x + 5, y + (h - 8) / 2, EclipseColors.TEXT_SECONDARY);
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
