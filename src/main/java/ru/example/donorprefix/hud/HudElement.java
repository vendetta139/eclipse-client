package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import ru.example.donorprefix.gui.RenderUtils;

/**
 * Базовый класс любого HUD-элемента:
 * - Координаты x, y в экранном пространстве
 * - Локальная отрисовка контента СТРОГО от (0, 0)
 * - Единая матричная трансформация (translate -> scale)
 * - Синхронное отображение рамки и подписи в редакторе
 */
public abstract class HudElement {
    public final String id;
    public final String name;
    public float x;
    public float y;
    public int width;
    public int height;
    public float scale = 1.0f;
    public boolean enabled = true;

    public HudElement(String id, String name, int defaultWidth, int defaultHeight) {
        this.id = id;
        this.name = name;
        this.width = defaultWidth;
        this.height = defaultHeight;
        this.scale = 1.0f;
        this.enabled = true;
    }

    /**
     * Отрисовка контента элемента.
     * ВНИМАНИЕ: Отрисовка ведется СТРОГО от точки (0, 0) в локальных координатах!
     * Внутри метода запрещено обращаться к this.x, this.y или this.scale.
     */
    public abstract void renderContent(DrawContext context);

    public abstract void loadConfig();

    public abstract void saveConfig();

    public abstract void resetDefaultPosition(int screenWidth, int screenHeight);

    private float cachedLabelScale = Float.NaN;
    private boolean cachedLabelEnabled = false;
    private String cachedEditorLabel = "";

    /**
     * Единый финальный метод трансформации и отрисовки для игры и редактора.
     */
    public final void render(DrawContext context, boolean isEditor, boolean isSelected, boolean isHovered) {
        if (!this.enabled && !isEditor) return;
        if (this.scale < 0.25f || this.scale > 3.0f || Float.isNaN(this.scale)) this.scale = 1.0f;

        boolean needScale = Math.abs(this.scale - 1.0f) >= 0.0001f;
        boolean needTranslate = this.x != 0.0f || this.y != 0.0f;
        Matrix3x2fStack matrices = (needScale || needTranslate) ? context.getMatrices() : null;
        if (matrices != null) {
            matrices.pushMatrix();
            if (needTranslate) {
                matrices.translate(this.x, this.y);
            }
            if (needScale) {
                matrices.scale(this.scale, this.scale);
            }
        }

        try {
            // 3. Рисуем сам элемент в локальных координатах (0, 0 .. width, height)
            this.renderContent(context);

            // 4. Если открыт HUD Редактор — рисуем рамку и подпись ЗДЕСЬ ЖЕ в (0, 0 .. width, height)!
            if (isEditor) {
                int borderColor = isSelected ? 0xFFF5A65B : (isHovered ? 0xFF6A67CE : (this.enabled ? 0x80383A52 : 0x80702020));
                RenderUtils.drawBorder(context, 0, 0, this.width, this.height, borderColor);
                String label = getEditorLabel();
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.textRenderer != null) {
                    context.drawTextWithShadow(mc.textRenderer, label, 0, -11, this.enabled ? 0xFFEAECEF : 0xFFAAAAAA);
                }
            }
        } finally {
            if (matrices != null) {
                matrices.popMatrix();
            }
        }
    }

    private String getEditorLabel() {
        if (cachedLabelScale != this.scale || cachedLabelEnabled != this.enabled) {
            cachedLabelScale = this.scale;
            cachedLabelEnabled = this.enabled;
            int scaledPct = Math.round(this.scale * 100.0f);
            int whole = scaledPct / 100;
            int frac = scaledPct % 100;
            StringBuilder sb = new StringBuilder(this.name.length() + 18);
            sb.append(this.name).append(" [").append(whole).append('.');
            if (frac < 10) sb.append('0');
            sb.append(frac).append("x]");
            if (!this.enabled) sb.append(" (ВЫКЛ)");
            cachedEditorLabel = sb.toString();
        }
        return cachedEditorLabel;
    }

    /**
     * Точная проверка наведения курсора мыши на масштабированный элемент.
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        float scaledW = this.width * this.scale;
        float scaledH = this.height * this.scale;
        return mouseX >= this.x && mouseX <= this.x + scaledW 
            && mouseY >= this.y && mouseY <= this.y + scaledH;
    }

    /**
     * Защита от потери и вылета элемента за экран.
     */
    public void clampOrReset(int screenWidth, int screenHeight) {
        if (this.scale <= 0.2f || this.scale > 3.0f || Float.isNaN(this.scale)) {
            this.scale = 1.0f;
        }
        float scaledW = this.width * this.scale;
        float scaledH = this.height * this.scale;
        float maxX = Math.max(2, screenWidth - scaledW - 2);
        float maxY = Math.max(14, screenHeight - scaledH - 2);

        if (this.x < 0 || this.x > screenWidth || this.y < 0 || this.y > screenHeight || Float.isNaN(this.x) || Float.isNaN(this.y)) {
            resetDefaultPosition(screenWidth, screenHeight);
            this.scale = 1.0f;
        } else {
            this.x = MathHelper.clamp(this.x, 2, maxX);
            this.y = MathHelper.clamp(this.y, 14, maxY);
        }
    }
}
