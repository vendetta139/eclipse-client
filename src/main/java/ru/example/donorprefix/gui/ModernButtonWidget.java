package ru.example.donorprefix.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Современная кнопка со скругленными углами, поддержкой Dark Glassmorphism,
 * кастомными цветовыми темами и плавной подсветкой при наведении.
 */
public class ModernButtonWidget extends ClickableWidget {

    @FunctionalInterface
    public interface PressAction {
        void onPress(ModernButtonWidget button);
    }

    public enum Style {
        PRIMARY,        // Основная акцентная (Фиолетовый градиент со свечением)
        SECONDARY,      // Второстепенная (Полупрозрачное темное стекло)
        DANGER,         // Деструктивная (Рубиновый акцент для закрытия/удаления)
        ACTION_TEST,    // Кнопка быстрого тестирования (Сиренево-пурпурная)
        ACTION_NAV,     // Кнопки перемещения вверх/вниз
        ACTION_DELETE,  // Компактная кнопка удаления (красный крестик)
        STEPPER,        // Кнопки +/- кулдауна (скругленный pill)
        STATUS_ACTIVE,  // Зеленая неоновая капсула активного статуса
        STATUS_PAUSED,  // Золотая капсула паузы
        TAB_ACTIVE,     // Активная верхняя вкладка (неон + акцент)
        TAB_INACTIVE,   // Неактивная верхняя вкладка (полупрозрачная)
        TOGGLE_ON,      // Переключатель эффекта: Включен
        TOGGLE_OFF,     // Переключатель эффекта: Выключен
        PRESET_ACTIVE   // Выбранная опция / пресет цвета
    }

    private final PressAction onPress;
    private Style style;
    private int cornerRadius = 6;
    private float hoverAnim = 0.0f;
    private float clickAnim = 0.0f;
    private long lastRenderTime = 0;

    public ModernButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Style style) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.style = style;
        if (style == Style.STATUS_ACTIVE || style == Style.STATUS_PAUSED) {
            this.cornerRadius = height / 2;
        } else if (style == Style.STEPPER || style == Style.ACTION_NAV || style == Style.ACTION_DELETE || style == Style.ACTION_TEST || style == Style.TOGGLE_ON || style == Style.TOGGLE_OFF || style == Style.PRESET_ACTIVE) {
            this.cornerRadius = 4;
        } else {
            this.cornerRadius = 6;
        }
    }

    public void onPress() {
        this.clickAnim = 1.0f;
        try {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        } catch (Throwable ignored) {}
        if (this.onPress != null) {
            this.onPress.onPress(this);
        }
    }

    @Override
    public void onClick(net.minecraft.client.gui.Click click, boolean bl) {
        try {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        } catch (Throwable ignored) {}
        this.onPress();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (!this.active || !this.visible) {
            return false;
        }
        int keyCode = input.key();
        if (keyCode == 257 || keyCode == 32 || keyCode == 335) {
            try {
                this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            } catch (Throwable ignored) {}
            this.onPress();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        try {
            this.appendDefaultNarrations(builder);
        } catch (Throwable ignored) {}
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public ModernButtonWidget setCornerRadius(int radius) {
        this.cornerRadius = radius;
        return this;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        long now = System.currentTimeMillis();
        float deltaSec = lastRenderTime > 0 ? (now - lastRenderTime) / 1000.0f : 0.016f;
        lastRenderTime = now;

        boolean hovered = this.isHovered();
        this.hoverAnim = AnimationUtil.deltaLerp(this.hoverAnim, (hovered && this.active) ? 1.0f : 0.0f, 14.0f, deltaSec);
        this.clickAnim = AnimationUtil.deltaLerp(this.clickAnim, 0.0f, 12.0f, deltaSec);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        int bgColor;
        int borderColor;
        int textColor;

        if (!this.active) {
            bgColor = EclipseColors.BG_WINDOW;
            borderColor = EclipseColors.BORDER;
            textColor = EclipseColors.TEXT_DISABLED;
        } else {
            switch (style) {
                case PRIMARY, STATUS_ACTIVE -> {
                    bgColor = hovered ? 0xFFFFFFFF : EclipseColors.ACCENT_WHITE;
                    borderColor = EclipseColors.BORDER_ACTIVE;
                    textColor = EclipseColors.TEXT_ON_ACCENT;
                }
                case SECONDARY, ACTION_NAV, STEPPER, TAB_INACTIVE -> {
                    if (hovered) {
                        bgColor = EclipseColors.BG_HOVER;
                        borderColor = EclipseColors.BORDER_ACTIVE;
                        textColor = EclipseColors.TEXT_PRIMARY;
                    } else {
                        bgColor = EclipseColors.BG_CARD;
                        borderColor = EclipseColors.BORDER;
                        textColor = EclipseColors.TEXT_SECONDARY;
                    }
                }
                case DANGER, ACTION_DELETE -> {
                    if (hovered) {
                        bgColor = EclipseColors.ACCENT_WHITE;
                        borderColor = EclipseColors.BORDER_ACTIVE;
                        textColor = EclipseColors.TEXT_ON_ACCENT;
                    } else {
                        bgColor = EclipseColors.BG_HOVER;
                        borderColor = EclipseColors.BORDER;
                        textColor = EclipseColors.TEXT_VERSION;
                    }
                }
                case ACTION_TEST -> {
                    if (hovered) {
                        bgColor = EclipseColors.ACCENT_WHITE;
                        borderColor = EclipseColors.BORDER_ACTIVE;
                        textColor = EclipseColors.TEXT_ON_ACCENT;
                    } else {
                        bgColor = EclipseColors.BG_SIDEBAR_ACTIVE;
                        borderColor = EclipseColors.BORDER;
                        textColor = EclipseColors.TEXT_PRIMARY;
                    }
                }
                case STATUS_PAUSED -> {
                    if (hovered) {
                        bgColor = EclipseColors.BG_HOVER;
                        borderColor = EclipseColors.BORDER_ACTIVE;
                        textColor = EclipseColors.TEXT_PRIMARY;
                    } else {
                        bgColor = EclipseColors.TRACK_BG;
                        borderColor = EclipseColors.BORDER;
                        textColor = EclipseColors.TEXT_SECONDARY;
                    }
                }
                case TAB_ACTIVE -> {
                    bgColor = hovered ? EclipseColors.BG_HOVER : EclipseColors.BG_SIDEBAR_ACTIVE;
                    borderColor = EclipseColors.BORDER_ACTIVE;
                    textColor = EclipseColors.TEXT_PRIMARY;
                }
                case TOGGLE_ON, PRESET_ACTIVE -> {
                    bgColor = hovered ? 0xFFFFFFFF : EclipseColors.ACCENT_WHITE;
                    borderColor = EclipseColors.BORDER_ACTIVE;
                    textColor = EclipseColors.TEXT_ON_ACCENT;
                }
                case TOGGLE_OFF -> {
                    if (hovered) {
                        bgColor = EclipseColors.BG_HOVER;
                        borderColor = EclipseColors.BORDER_ACTIVE;
                        textColor = EclipseColors.TEXT_PRIMARY;
                    } else {
                        bgColor = EclipseColors.TRACK_BG;
                        borderColor = EclipseColors.BORDER;
                        textColor = EclipseColors.TEXT_SECONDARY;
                    }
                }
                default -> {
                    bgColor = EclipseColors.BG_CARD;
                    borderColor = EclipseColors.BORDER;
                    textColor = EclipseColors.TEXT_PRIMARY;
                }
            }
        }

        // Строгая плоская монохромная кнопка с 1px рамкой
        RenderUtils.drawFlatCard(context, x, y, w, h, bgColor, borderColor);

        int yShift = Math.round(-1.0f * this.hoverAnim + this.clickAnim * 1.0f);
        int textY = y + (h - 8) / 2 + yShift;
        RenderUtils.drawCenteredTextWithShadow(context, textRenderer, this.getMessage(), x + w / 2, textY, textColor);
    }

    public static Builder modern(Text message, PressAction onPress) {
        return new Builder(message, onPress);
    }

    public static class Builder {
        private final Text message;
        private final PressAction onPress;
        private int x;
        private int y;
        private int width = 100;
        private int height = 20;
        private Style style = Style.SECONDARY;
        private Tooltip tooltip;

        public Builder(Text message, PressAction onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public ModernButtonWidget build() {
            ModernButtonWidget button = new ModernButtonWidget(x, y, width, height, message, onPress, style);
            if (this.tooltip != null) {
                button.setTooltip(this.tooltip);
            }
            return button;
        }
    }
}
