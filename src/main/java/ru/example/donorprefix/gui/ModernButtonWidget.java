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
            bgColor = 0x1F0F1218;
            borderColor = 0x28202836;
            textColor = 0x445A6578;
        } else {
            switch (style) {
                case PRIMARY -> {
                    bgColor = hovered ? 0xFF1F2233 : 0xFF161824;
                    borderColor = 0;
                    textColor = hovered ? 0xFFFFFFFF : 0xFFEAECEF;
                }
                case SECONDARY -> {
                    if (hovered) {
                        bgColor = 0x33232533;
                        borderColor = 0xFF6A67CE;
                        textColor = 0xFFFFFFFF;
                    } else {
                        bgColor = 0xFF101118;
                        borderColor = 0xFF1C1E2B;
                        textColor = 0xFF7D8296;
                    }
                }
                case DANGER, ACTION_DELETE -> {
                    if (hovered) {
                        bgColor = 0xFF4D242D;
                        borderColor = 0xFFE06C75;
                        textColor = 0xFFFFFFFF;
                    } else {
                        bgColor = 0xFF3A1D24;
                        borderColor = 0x55E06C75;
                        textColor = 0xFFE06C75;
                    }
                }
                case ACTION_TEST -> {
                    if (hovered) {
                        bgColor = 0x4D6A67CE;
                        borderColor = 0xFFF5A65B;
                        textColor = 0xFFFFFFFF;
                    } else {
                        bgColor = 0x266A67CE;
                        borderColor = 0xFF6A67CE;
                        textColor = 0xFF6A67CE;
                    }
                }
                case ACTION_NAV -> {
                    if (hovered) {
                        bgColor = 0xFF1C1E2B;
                        borderColor = 0xFF383A52;
                        textColor = 0xFFFFFFFF;
                    } else {
                        bgColor = 0xFF101118;
                        borderColor = 0xFF232533;
                        textColor = 0xFF7D8296;
                    }
                }
                case STEPPER -> {
                    if (hovered) {
                        bgColor = 0xFF1C1E2B;
                        borderColor = 0xFF383A52;
                        textColor = 0xFFFFFFFF;
                    } else {
                        bgColor = 0xFF101118;
                        borderColor = 0xFF232533;
                        textColor = 0xFFA0A5B8;
                    }
                }
                case STATUS_ACTIVE -> {
                    if (hovered) {
                        bgColor = 0x44065F46;
                        borderColor = 0xFF34D399;
                        textColor = 0xFF6EE7B7;
                    } else {
                        bgColor = 0x28064E3B;
                        borderColor = 0x77059669;
                        textColor = 0xFFA7F3D0;
                    }
                }
                case STATUS_PAUSED -> {
                    if (hovered) {
                        bgColor = 0xFF241F1A;
                        borderColor = 0xFF6A4A32;
                        textColor = 0xFFFFD38A;
                    } else {
                        bgColor = 0xFF191614;
                        borderColor = 0xFF4A3525;
                        textColor = 0xFFF5A65B;
                    }
                }
                case TAB_ACTIVE -> {
                    bgColor = hovered ? 0xFF1F2233 : 0xFF161824;
                    borderColor = 0xFF383A52;
                    textColor = 0xFFFFFFFF;
                }
                case TAB_INACTIVE -> {
                    if (hovered) {
                        bgColor = 0x2A1C2030;
                        borderColor = 0xFF282A3A;
                        textColor = 0xFFEAECEF;
                    } else {
                        bgColor = 0x14101118;
                        borderColor = 0x00000000;
                        textColor = 0xFF7D8296;
                    }
                }

                case TOGGLE_ON -> {
                    if (hovered) {
                        bgColor = 0x667C3AED;
                        borderColor = 0xFFC084FC;
                        textColor = 0xFFFFFFFF;
                    } else {
                        bgColor = 0x446D28D9;
                        borderColor = 0xFFA855F7;
                        textColor = 0xFFF3E8FF;
                    }
                }
                case TOGGLE_OFF -> {
                    if (hovered) {
                        bgColor = 0x351E293B;
                        borderColor = 0x5564748B;
                        textColor = 0xFFE2E8F0;
                    } else {
                        bgColor = 0x20111827;
                        borderColor = 0x33334155;
                        textColor = 0xFF94A3B8;
                    }
                }
                case PRESET_ACTIVE -> {
                    bgColor = hovered ? 0x887C3AED : 0x666D28D9;
                    borderColor = 0xFFA855F7;
                    textColor = 0xFFFFFFFF;
                }
                default -> {
                    bgColor = 0x28161F2E;
                    borderColor = 0x33334155;
                    textColor = 0xFFCBD5E1;
                }
            }
        }

        // Плавное внешнее свечение при наведении
        if (this.hoverAnim > 0.05f && this.active) {
            int glowColor = (style == Style.PRIMARY)
                    ? RenderUtils.multiplyAlpha(0xFF6A67CE, this.hoverAnim * 0.45f)
                    : EclipseTheme.multiplyAlpha(EclipseTheme.ACCENT_INDIGO, this.hoverAnim * 0.35f);
            RenderUtils.drawSoftGlow(context, x, y, w, h, cornerRadius, 3, glowColor);
        }

        // Отрисовка фона и рамки со сглаженными углами
        if (style == Style.PRIMARY && this.active) {
            RenderUtils.drawRoundedRect(context, x, y, w, h, cornerRadius, bgColor);
            int gold = hovered ? 0xFFFFD38A : 0xFFF5A65B;
            int indigo = hovered ? 0xFF8B88EE : 0xFF6A67CE;
            RenderUtils.drawGradientRoundedOutline(context, x, y, w, h, cornerRadius, gold, indigo);
        } else {
            RenderUtils.drawRoundedRect(context, x, y, w, h, cornerRadius, bgColor);
            if ((borderColor & 0xFF000000) != 0) {
                RenderUtils.drawRoundedOutline(context, x, y, w, h, cornerRadius, borderColor);
            }
        }

        // Текст кнопки по центру с микро-сдвигом на 1.5px вверх при наведении и пружинящим откликом
        int yShift = Math.round(-1.5f * this.hoverAnim + this.clickAnim * 1.5f);
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
