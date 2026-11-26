package ru.example.donorprefix.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import ru.example.donorprefix.hud.HudElement;
import ru.example.donorprefix.hud.HudManager;

import java.util.List;

/**
 * Полноценный экран визуального редактирования HUD (HudEditorScreen) для Minecraft 1.21.11+:
 * - Прямой опрос GLFW кнопок мыши и клавиш внутри render() для 100% совместимости
 * - Свободное перетаскивание элементов (ЛКМ) с сохранением позиции
 * - Включение / выключение элементов (ПКМ)
 * - Изменение масштаба через экранные кнопки [-] [1.0x] [+] и клавиши ВВЕРХ/ВНИЗ (+/-)
 * - Защита от пропажи элементов за границы экрана
 * - Полупрозрачный темный фон, координатная сетка выравнивания и центральные направляющие
 */
public class HudEditorScreen extends Screen {

    private final Screen parent;

    private HudElement draggingElement = null;
    private HudElement selectedElement = null;
    private HudElement hoveredElement = null;

    private float dragOffsetX = 0.0f;
    private float dragOffsetY = 0.0f;

    private boolean wasLeftDown = false;
    private boolean wasRightDown = false;

    private int keyUpHeldFrames = 0;
    private int keyDownHeldFrames = 0;
    private boolean scaleDirty = false;

    private HudElement cachedToolbarElement = null;
    private float cachedToolbarScale = Float.NaN;
    private String cachedToolbarLabel = "Не выбран: -";

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Редактор HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        HudManager.getInstance().loadConfigs();
        List<HudElement> elements = HudManager.getInstance().getElements();
        for (HudElement el : elements) {
            el.clampOrReset(this.width, this.height);
        }
        if (selectedElement == null && !elements.isEmpty()) {
            selectedElement = elements.get(0);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (context == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        long window = mc.getWindow().getHandle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        boolean keyUp = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS 
                      || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_EQUAL) == GLFW.GLFW_PRESS 
                      || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_ADD) == GLFW.GLFW_PRESS);
        boolean keyDown = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS 
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_MINUS) == GLFW.GLFW_PRESS 
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_SUBTRACT) == GLFW.GLFW_PRESS);

        List<HudElement> elements = HudManager.getInstance().getElements();

        // 1. Определение наведенного элемента
        hoveredElement = null;
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement el = elements.get(i);
            if (el.isMouseOver(mouseX, mouseY) && draggingElement == null) {
                hoveredElement = el;
                break;
            }
        }

        HudElement targetScaleElement = (selectedElement != null) ? selectedElement : hoveredElement;

        // 2. Координаты экранной панели управления масштабом (кнопки [-], [1.0x], [+])
        int toolbarW = 210;
        int toolbarH = 18;
        int toolbarX = (this.width - toolbarW) / 2;
        int toolbarY = 24;

        int btnMinusX = toolbarX + 112;
        int btnMinusY = toolbarY + 2;
        int btnMinusW = 22;
        int btnMinusH = 14;

        int btnResetX = toolbarX + 138;
        int btnResetY = toolbarY + 2;
        int btnResetW = 38;
        int btnResetH = 14;

        int btnPlusX = toolbarX + 180;
        int btnPlusY = toolbarY + 2;
        int btnPlusW = 22;
        int btnPlusH = 14;

        boolean hoverMinus = mouseX >= btnMinusX && mouseX <= btnMinusX + btnMinusW && mouseY >= btnMinusY && mouseY <= btnMinusY + btnMinusH;
        boolean hoverReset = mouseX >= btnResetX && mouseX <= btnResetX + btnResetW && mouseY >= btnResetY && mouseY <= btnResetY + btnResetH;
        boolean hoverPlus  = mouseX >= btnPlusX  && mouseX <= btnPlusX  + btnPlusW  && mouseY >= btnPlusY  && mouseY <= btnPlusY  + btnPlusH;

        // 3. Обработка клика по экранным кнопкам масштабирования
        boolean btnClicked = false;
        if (leftDown && !wasLeftDown && targetScaleElement != null) {
            if (hoverMinus) {
                targetScaleElement.scale = MathHelper.clamp(targetScaleElement.scale - 0.05f, 0.4f, 2.5f);
                HudManager.getInstance().saveConfigs();
                btnClicked = true;
            } else if (hoverReset) {
                targetScaleElement.scale = 1.0f;
                HudManager.getInstance().saveConfigs();
                btnClicked = true;
            } else if (hoverPlus) {
                targetScaleElement.scale = MathHelper.clamp(targetScaleElement.scale + 0.05f, 0.4f, 2.5f);
                HudManager.getInstance().saveConfigs();
                btnClicked = true;
            }
        }

        // 4. Обработка клавиш клавиатуры для масштабирования (UP/DOWN, =/-, numpad +/-) без спама записью на диск
        if (keyUp) {
            keyUpHeldFrames++;
            if (keyUpHeldFrames == 1 || (keyUpHeldFrames > 18 && keyUpHeldFrames % 4 == 0)) {
                if (targetScaleElement != null) {
                    targetScaleElement.scale = MathHelper.clamp(targetScaleElement.scale + 0.05f, 0.4f, 2.5f);
                    scaleDirty = true;
                }
            }
        } else {
            keyUpHeldFrames = 0;
        }

        if (keyDown) {
            keyDownHeldFrames++;
            if (keyDownHeldFrames == 1 || (keyDownHeldFrames > 18 && keyDownHeldFrames % 4 == 0)) {
                if (targetScaleElement != null) {
                    targetScaleElement.scale = MathHelper.clamp(targetScaleElement.scale - 0.05f, 0.4f, 2.5f);
                    scaleDirty = true;
                }
            }
        } else {
            keyDownHeldFrames = 0;
        }

        if (!keyUp && !keyDown && scaleDirty) {
            scaleDirty = false;
            HudManager.getInstance().saveConfigs();
        }

        // 5. Обработка перетаскивания элементов мышью (GLFW Drag & Drop)
        if (leftDown && !wasLeftDown && !btnClicked) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                HudElement el = elements.get(i);
                if (el.isMouseOver(mouseX, mouseY)) {
                    draggingElement = el;
                    selectedElement = el;
                    dragOffsetX = (float) mouseX - el.x;
                    dragOffsetY = (float) mouseY - el.y;
                    break;
                }
            }
        }

        // Удержание ЛКМ (перемещение элемента за курсором):
        if (leftDown && draggingElement != null) {
            float newX = (float) mouseX - dragOffsetX;
            float newY = (float) mouseY - dragOffsetY;
            float maxW = this.width - draggingElement.width * draggingElement.scale;
            float maxH = this.height - draggingElement.height * draggingElement.scale;
            draggingElement.x = MathHelper.clamp(newX, 0, Math.max(0, maxW));
            draggingElement.y = MathHelper.clamp(newY, 0, Math.max(0, maxH));
        }

        // Отпускание ЛКМ (сохранение позиции):
        if (!leftDown && wasLeftDown && draggingElement != null) {
            draggingElement = null;
            HudManager.getInstance().saveConfigs();
        }

        // Нажатие ПКМ по элементу (вкл/выкл элемента):
        if (rightDown && !wasRightDown) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                HudElement el = elements.get(i);
                if (el.isMouseOver(mouseX, mouseY)) {
                    selectedElement = el;
                    el.enabled = !el.enabled;
                    HudManager.getInstance().saveConfigs();
                    break;
                }
            }
        }

        wasLeftDown = leftDown;
        wasRightDown = rightDown;

        // 6. Полупрозрачный темный фон
        context.fill(0, 0, this.width, this.height, 0xD0070103);

        // 7. Координатная сетка (#15FFFFFF)
        int gridSize = 20;
        int gridColor = 0x15FFFFFF;
        for (int gx = 0; gx < this.width; gx += gridSize) {
            context.fill(gx, 0, gx + 1, this.height, gridColor);
        }
        for (int gy = 0; gy < this.height; gy += gridSize) {
            context.fill(0, gy, this.width, gy + 1, gridColor);
        }

        // 8. Центральные направляющие линии
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int guideColor = 0x33FF1744;
        context.fill(centerX, 0, centerX + 1, this.height, guideColor);
        context.fill(0, centerY, this.width, centerY + 1, guideColor);

        // 9. Отрисовка всех HUD-элементов в режиме редактора
        for (HudElement el : elements) {
            boolean isSelected = (el == selectedElement || el == draggingElement);
            boolean isHovered = (el == hoveredElement);
            el.render(context, true, isSelected, isHovered);
        }

        // 10. Информационная подсказка вверху экрана
        String hint = "ЛКМ — Перетащить │ ПКМ — Вкл/Выкл │ Стрелки / +/- / Кнопки — Размер │ ESC — Выход";
        int hintW = this.textRenderer.getWidth(hint);
        int hintX = (this.width - hintW) / 2;
        int hintY = 6;
        context.fill(hintX - 8, hintY - 3, hintX + hintW + 8, hintY + 12, 0xD00A0A10);
        RenderUtils.drawBorder(context, hintX - 8, hintY - 3, hintW + 16, 15, 0x40F5A65B);
        context.drawTextWithShadow(this.textRenderer, hint, hintX, hintY + 1, 0xFFEAECEF);

        // 11. Отрисовка панели управления масштабом
        context.fill(toolbarX, toolbarY, toolbarX + toolbarW, toolbarY + toolbarH, 0xD50E0E18);
        RenderUtils.drawBorder(context, toolbarX, toolbarY, toolbarW, toolbarH, 0x50F5A65B);

        float curScale = (targetScaleElement != null) ? targetScaleElement.scale : Float.NaN;
        if (cachedToolbarElement != targetScaleElement || Float.compare(cachedToolbarScale, curScale) != 0) {
            cachedToolbarElement = targetScaleElement;
            cachedToolbarScale = curScale;
            if (targetScaleElement == null) {
                cachedToolbarLabel = "Не выбран: -";
            } else {
                int pct = Math.round(curScale * 100.0f);
                String scaleStr = (pct / 100) + "." + ((pct % 100) < 10 ? "0" : "") + (pct % 100) + "x";
                String targetName = targetScaleElement.name;
                String candidate = targetName + ": " + scaleStr;
                if (this.textRenderer.getWidth(candidate) > 102) {
                    candidate = targetName.substring(0, Math.min(targetName.length(), 6)) + "..: " + scaleStr;
                }
                cachedToolbarLabel = candidate;
            }
        }
        context.drawTextWithShadow(this.textRenderer, cachedToolbarLabel, toolbarX + 6, toolbarY + 5, 0xFFEAECEF);

        // Кнопка [-]
        int bgMinus = hoverMinus ? 0xFF4A4B65 : 0xFF232435;
        context.fill(btnMinusX, btnMinusY, btnMinusX + btnMinusW, btnMinusY + btnMinusH, bgMinus);
        RenderUtils.drawBorder(context, btnMinusX, btnMinusY, btnMinusW, btnMinusH, hoverMinus ? 0xFFF5A65B : 0xFF3D3E56);
        context.drawTextWithShadow(this.textRenderer, "-", btnMinusX + 8, btnMinusY + 3, 0xFFFFFFFF);

        // Кнопка [1.0x]
        int bgReset = hoverReset ? 0xFF4A4B65 : 0xFF232435;
        context.fill(btnResetX, btnResetY, btnResetX + btnResetW, btnResetY + btnResetH, bgReset);
        RenderUtils.drawBorder(context, btnResetX, btnResetY, btnResetW, btnResetH, hoverReset ? 0xFFF5A65B : 0xFF3D3E56);
        context.drawTextWithShadow(this.textRenderer, "1.0x", btnResetX + 8, btnResetY + 3, 0xFFFFFFFF);

        // Кнопка [+]
        int bgPlus = hoverPlus ? 0xFF4A4B65 : 0xFF232435;
        context.fill(btnPlusX, btnPlusY, btnPlusX + btnPlusW, btnPlusY + btnPlusH, bgPlus);
        RenderUtils.drawBorder(context, btnPlusX, btnPlusY, btnPlusW, btnPlusH, hoverPlus ? 0xFFF5A65B : 0xFF3D3E56);
        context.drawTextWithShadow(this.textRenderer, "+", btnPlusX + 7, btnPlusY + 3, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        scaleDirty = false;
        HudManager.getInstance().saveConfigs();
        ru.eclipse.client.core.EclipseStateSync.getInstance().flushPendingSave();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
