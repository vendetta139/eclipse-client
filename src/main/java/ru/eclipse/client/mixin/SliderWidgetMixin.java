package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.example.donorprefix.gui.AnimationUtil;
import ru.example.donorprefix.gui.EclipseTheme;
import ru.example.donorprefix.gui.RenderUtils;

/**
 * ✦ SliderWidgetMixin — Глобальная стилизация слайдеров ECLIPSE CLIENT:
 * - Темная матовая подложка #12131C с обводкой #232533.
 * - Плавный градиент заполнения от золотистого #F5A65B к индиго #6A67CE.
 * - Плавная доводка бегунка через deltaTime-lerp без рывков.
 */
@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin extends ClickableWidget {

    @Shadow protected double value;

    @Unique
    private float eclipseSmoothValue = -1.0f;
    @Unique
    private float eclipseHoverAnim = 0.0f;
    @Unique
    private long eclipseLastTime = 0;

    public SliderWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    private void onRenderEclipseSlider(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        long now = System.currentTimeMillis();
        float deltaSec = eclipseLastTime > 0 ? (now - eclipseLastTime) / 1000.0f : 0.016f;
        deltaSec = Math.max(0.0f, Math.min(0.1f, deltaSec));
        eclipseLastTime = now;

        if (eclipseSmoothValue < 0.0f) {
            eclipseSmoothValue = (float) this.value;
        }
        eclipseSmoothValue = AnimationUtil.deltaLerp(eclipseSmoothValue, (float) this.value, 18.0f, deltaSec);

        boolean hovered = this.isHovered() || this.isFocused();
        eclipseHoverAnim = AnimationUtil.deltaLerp(eclipseHoverAnim, hovered ? 1.0f : 0.0f, 14.0f, deltaSec);

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        // 1. Темная матовая подложка #12131C
        int bgCol = 0xE612131C;
        RenderUtils.drawRoundedRect(context, x, y, w, h, 5, bgCol);

        // 2. Рамка #232533 с мягким переходом к акценту при наведении
        int borderCol = AnimationUtil.lerpColor(EclipseTheme.PANEL_BORDER, EclipseTheme.ACCENT_INDIGO, eclipseHoverAnim);
        RenderUtils.drawRoundedOutline(context, x, y, w, h, 5, borderCol);

        // 3. Заполненная часть прогресса (горизонтальный градиент золото #F5A65B -> индиго #6A67CE)
        int fillW = Math.round((w - 4) * Math.max(0.0f, Math.min(1.0f, eclipseSmoothValue)));
        if (fillW > 0) {
            EclipseTheme.drawHorizontalGradient(context, x + 2, y + 2, fillW, h - 4, EclipseTheme.ACCENT_GOLD, EclipseTheme.ACCENT_INDIGO);
        }

        // 4. Минималистичный стильный бегунок
        int knobX = x + 2 + fillW;
        int knobW = 4;
        int actualKnobX = Math.max(x + 2, Math.min(x + w - knobW - 2, knobX - knobW / 2));
        RenderUtils.drawRoundedRect(context, actualKnobX, y + 1, knobW, h - 2, 2, 0xFFFFFFFF);

        // 5. Мягкое свечение при наведении
        if (eclipseHoverAnim > 0.05f) {
            int glowCol = EclipseTheme.multiplyAlpha(EclipseTheme.ACCENT_INDIGO, eclipseHoverAnim * 0.35f);
            RenderUtils.drawSoftGlow(context, x, y, w, h, 5, 2, glowCol);
        }

        // 6. Текст со значением (центрирован)
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc != null ? mc.textRenderer : null;
        if (tr != null) {
            int textColor = this.active ? 0xFFFFFFFF : 0xFF7D8296;
            RenderUtils.drawCenteredTextWithShadow(context, tr, this.getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
        }
    }
}
