package ru.eclipse.client.ui.clickgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.ModuleManager;
import ru.eclipse.client.module.setting.*;
import ru.example.donorprefix.gui.AnimationUtil;
import ru.example.donorprefix.gui.EclipseTheme;
import ru.example.donorprefix.gui.HudEditorScreen;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.PrefixMenuScreen;
import ru.example.donorprefix.ClientPrefixConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✦ EclipseClickGui — Флагманский интерфейс управления клиентом ECLIPSE CLIENT 1.21.11.
 * Выполнен в фирменном стиле Dark Minimalism: глубокий обсидиан #0B0B10, золотистый янтарь #F5A65B
 * и сумрачный индиго #6D28D9.
 */
public class EclipseClickGui extends Screen {

    private int guiX = 100;
    private int guiY = 60;
    private int guiW = 560;
    private int guiH = 360;

    private boolean draggingWindow = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private Category selectedCategory = Category.COMBAT_PVP;
    private String searchQuery = "";
    private Module expandedModule = null;
    private Module listeningKeybindModule = null;

    private NumberSetting draggingSlider = null;
    private int sliderModuleY = 0;

    private float openProgress = 0.0f;
    private long lastRenderTime = 0;

    // Плавные анимации состояний
    private final Map<Module, Float> moduleToggleAnims = new HashMap<>();

    public EclipseClickGui() {
        super(Text.literal("ECLIPSE CLIENT — ClickGUI"));
    }

    @Override
    protected void init() {
        this.openProgress = 0.0f;
        this.lastRenderTime = System.currentTimeMillis();

        this.guiW = Math.min(580, this.width - 40);
        this.guiH = Math.min(380, this.height - 40);
        this.guiX = (this.width - this.guiW) / 2;
        this.guiY = (this.height - this.guiH) / 2;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float deltaSec = lastRenderTime > 0 ? Math.min(0.1f, (now - lastRenderTime) / 1000.0f) : 0.016f;
        lastRenderTime = now;

        openProgress = AnimationUtil.deltaLerp(openProgress, 1.0f, 16.0f, deltaSec);
        float alpha = AnimationUtil.easeOutCubic(openProgress);

        // Затемнение игрового мира
        int bgDarkness = (int) (alpha * 160.0f);
        context.fill(0, 0, this.width, this.height, (bgDarkness << 24) | 0x050508);

        // Обработка непрерывного перетаскивания окна и слайдеров мыши
        handleMouseDragging(mouseX, mouseY);

        // Отрисовка главного окна
        renderGuiWindow(context, mouseX, mouseY, alpha, deltaSec);

        super.render(context, mouseX, mouseY, delta);
    }

    private void handleMouseDragging(int mouseX, int mouseY) {
        if (this.client == null || this.client.getWindow() == null) return;
        long handle = this.client.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (!mouseDown) {
            draggingWindow = false;
            draggingSlider = null;
            return;
        }

        if (draggingWindow) {
            guiX = Math.max(10, Math.min(this.width - guiW - 10, mouseX - dragOffsetX));
            guiY = Math.max(10, Math.min(this.height - guiH - 10, mouseY - dragOffsetY));
        }
    }

    private void renderGuiWindow(DrawContext context, int mouseX, int mouseY, float alpha, float deltaSec) {
        TextRenderer tr = this.textRenderer;
        if (tr == null) return;

        // Фон главного окна ClickGUI (глубокий обсидиановый кард с мягким свечением)
        RenderUtils.drawRoundedRect(context, guiX, guiY, guiW, guiH, 8, 0xF50B0B10);
        RenderUtils.drawRoundedOutline(context, guiX, guiY, guiW, guiH, 8, 0xFF232533);
        RenderUtils.drawSoftGlow(context, guiX, guiY, guiW, guiH, 8, 4, 0x336D28D9);

        // ══════════════════════════════════════════════════════════════════════
        // ШАПКА ОКНА (Title Bar)
        // ══════════════════════════════════════════════════════════════════════
        int headerH = 38;
        context.fill(guiX, guiY, guiX + guiW, guiY + headerH, 0x6612131C);
        context.fill(guiX, guiY + headerH - 1, guiX + guiW, guiY + headerH, 0xFF1E202C);

        // Иконка и логотип ECLIPSE
        EclipseTheme.drawEclipseBadge(context, guiX + 12, guiY + 11, 16, alpha);
        RenderUtils.drawTextWithShadow(context, tr, Text.literal("§lECLIPSE§r §7CLIENT"), guiX + 34, guiY + 15, 0xFFFFFFFF);
        RenderUtils.drawTextWithShadow(context, tr, Text.literal("§8| 1.21.11 by alone.vendetta"), guiX + 126, guiY + 15, 0xFF7D8296);

        // Кнопка HUD редактора
        int hudBtnW = 86;
        int hudBtnH = 20;
        int hudBtnX = guiX + guiW - hudBtnW - 10;
        int hudBtnY = guiY + 9;
        boolean hoverHud = mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH;
        int hudBg = hoverHud ? 0xE6F5A65B : 0xCC1A1B28;
        int hudText = hoverHud ? 0xFF0B0B10 : 0xFFF5A65B;
        RenderUtils.drawRoundedRect(context, hudBtnX, hudBtnY, hudBtnW, hudBtnH, 4, hudBg);
        RenderUtils.drawRoundedOutline(context, hudBtnX, hudBtnY, hudBtnW, hudBtnH, 4, 0x88F5A65B);
        RenderUtils.drawCenteredTextWithShadow(context, tr, Text.literal("❖ HUD РЕДАКТОР"), hudBtnX + hudBtnW / 2, hudBtnY + 6, hudText);

        // Кнопка меню префиксов
        int pfxBtnW = 76;
        int pfxBtnX = hudBtnX - pfxBtnW - 6;
        boolean hoverPfx = mouseX >= pfxBtnX && mouseX <= pfxBtnX + pfxBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH;
        int pfxBg = hoverPfx ? 0xE66D28D9 : 0xCC1A1B28;
        int pfxText = hoverPfx ? 0xFFFFFFFF : 0xFFD946EF;
        RenderUtils.drawRoundedRect(context, pfxBtnX, hudBtnY, pfxBtnW, hudBtnH, 4, pfxBg);
        RenderUtils.drawRoundedOutline(context, pfxBtnX, hudBtnY, pfxBtnW, hudBtnH, 4, 0x886D28D9);
        RenderUtils.drawCenteredTextWithShadow(context, tr, Text.literal("✦ ПРЕФИКСЫ"), pfxBtnX + pfxBtnW / 2, hudBtnY + 6, pfxText);

        // ══════════════════════════════════════════════════════════════════════
        // ЛЕВАЯ ПАНЕЛЬ КАТЕГОРИЙ (Sidebar)
        // ══════════════════════════════════════════════════════════════════════
        int sidebarW = 140;
        int contentY = guiY + headerH;
        int contentH = guiH - headerH;

        context.fill(guiX, contentY, guiX + sidebarW, guiY + guiH, 0x4D0E0F16);
        context.fill(guiX + sidebarW - 1, contentY, guiX + sidebarW, guiY + guiH, 0xFF1E202C);

        int catY = contentY + 10;
        int catH = 28;
        for (Category cat : Category.values()) {
            boolean isSelected = cat == selectedCategory;
            boolean hovered = mouseX >= guiX + 8 && mouseX <= guiX + sidebarW - 8 && mouseY >= catY && mouseY <= catY + catH;

            int itemBg = isSelected ? 0xCC232536 : (hovered ? 0x661A1B28 : 0x00000000);
            RenderUtils.drawRoundedRect(context, guiX + 8, catY, sidebarW - 16, catH, 5, itemBg);

            if (isSelected) {
                // Золотистая вертикальная линия активной вкладки
                context.fill(guiX + 9, catY + 5, guiX + 11, catY + catH - 5, 0xFFF5A65B);
            }

            int textColor = isSelected ? 0xFFFFFFFF : (hovered ? 0xFFE0E2EC : 0xFF8A8E9E);
            String title = cat.getIcon() + "  " + cat.getDisplayName();
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(title), guiX + 18, catY + 10, textColor);

            catY += catH + 4;
        }

        // ══════════════════════════════════════════════════════════════════════
        // ОСНОВНАЯ ОБЛАСТЬ МОДУЛЕЙ (Content Area)
        // ══════════════════════════════════════════════════════════════════════
        int areaX = guiX + sidebarW + 12;
        int areaY = contentY + 10;
        int areaW = guiW - sidebarW - 24;

        List<Module> moduleList = ModuleManager.getInstance().getModulesByCategory(selectedCategory);

        int curY = areaY;
        for (Module m : moduleList) {
            float anim = moduleToggleAnims.getOrDefault(m, m.isEnabled() ? 1.0f : 0.0f);
            anim = AnimationUtil.deltaLerp(anim, m.isEnabled() ? 1.0f : 0.0f, 16.0f, deltaSec);
            moduleToggleAnims.put(m, anim);

            boolean isExpanded = m == expandedModule;
            int cardH = 36;

            boolean cardHover = mouseX >= areaX && mouseX <= areaX + areaW && mouseY >= curY && mouseY <= curY + cardH;
            int cardBg = cardHover ? 0xF0181A26 : 0xD912131C;
            int cardBorder = m.isEnabled() ? AnimationUtil.lerpColor(0xFF2B2E40, 0xFFF5A65B, anim) : 0xFF1F212E;

            // Карточка модуля
            RenderUtils.drawRoundedRect(context, areaX, curY, areaW, cardH, 6, cardBg);
            RenderUtils.drawRoundedOutline(context, areaX, curY, areaW, cardH, 6, cardBorder);

            // Название и описание модуля
            int nameCol = m.isEnabled() ? 0xFFFFFFFF : 0xFFB0B4C8;
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(m.getName()), areaX + 10, curY + 8, nameCol);
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(m.getDescription()), areaX + 10, curY + 21, 0xFF65697C);

            // Кнопка бинда клавиши
            int bindW = 42;
            int bindH = 18;
            int bindX = areaX + areaW - 88;
            int bindY = curY + 9;
            boolean isListening = m == listeningKeybindModule;
            String bindName = isListening ? "..." : (m.getKeybind() > 0 ? GLFW.glfwGetKeyName(m.getKeybind(), 0) : "NONE");
            if (bindName == null || bindName.isEmpty()) bindName = "K" + m.getKeybind();
            if (m.getKeybind() == 0) bindName = "-";

            RenderUtils.drawRoundedRect(context, bindX, bindY, bindW, bindH, 3, isListening ? 0xFF6D28D9 : 0x881E202C);
            RenderUtils.drawCenteredTextWithShadow(context, tr, Text.literal(bindName), bindX + bindW / 2, bindY + 5, isListening ? 0xFFFFFFFF : 0xFF8A8E9E);

            // Переключатель (Toggle Capsule)
            int toggleW = 30;
            int toggleH = 16;
            int toggleX = areaX + areaW - 38;
            int toggleY = curY + 10;

            int toggleBg = AnimationUtil.lerpColor(0xFF232533, 0xFFF5A65B, anim);
            RenderUtils.drawPill(context, toggleX, toggleY, toggleW, toggleH, toggleBg, toggleBg);

            // Ползунок внутри переключателя
            int knobSize = 12;
            int knobX = (int) (toggleX + 2 + anim * (toggleW - knobSize - 4));
            RenderUtils.drawPill(context, knobX, toggleY + 2, knobSize, knobSize, 0xFFFFFFFF, 0xFFFFFFFF);

            curY += cardH + 6;

            // Если модуль раскрыт — рендерим его настройки
            if (isExpanded && !m.getSettings().isEmpty()) {
                int settingsPadding = 8;
                int settingsH = m.getSettings().size() * 26 + settingsPadding;
                RenderUtils.drawRoundedRect(context, areaX + 10, curY, areaW - 20, settingsH, 4, 0xAA0E0F16);
                RenderUtils.drawRoundedOutline(context, areaX + 10, curY, areaW - 20, settingsH, 4, 0x44232533);

                int setY = curY + 6;
                for (Setting<?> s : m.getSettings()) {
                    renderSetting(context, tr, s, areaX + 20, setY, areaW - 40, mouseX, mouseY);
                    setY += 26;
                }
                curY += settingsH + 6;
            }
        }
    }

    private void renderSetting(DrawContext context, TextRenderer tr, Setting<?> s, int x, int y, int w, int mouseX, int mouseY) {
        if (s instanceof BooleanSetting bs) {
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(bs.getName()), x, y + 4, 0xFFC0C4D4);
            int toggleW = 24;
            int toggleH = 13;
            int toggleX = x + w - toggleW;
            int toggleBg = bs.isEnabled() ? 0xFFF5A65B : 0xFF232533;
            RenderUtils.drawPill(context, toggleX, y + 2, toggleW, toggleH, toggleBg, toggleBg);
            int knobX = bs.isEnabled() ? toggleX + toggleW - 11 : toggleX + 2;
            RenderUtils.drawPill(context, knobX, y + 4, 9, 9, 0xFFFFFFFF, 0xFFFFFFFF);
        } else if (s instanceof NumberSetting ns) {
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(ns.getName()), x, y + 4, 0xFFC0C4D4);

            int sliderW = 100;
            int sliderH = 6;
            int sliderX = x + w - sliderW - 35;
            int sliderY = y + 7;

            // Ползунок
            double pct = (ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin());
            pct = Math.max(0.0, Math.min(1.0, pct));

            // Проверка перетаскивания слайдера мышью
            if (this.client != null && this.client.getWindow() != null) {
                long handle = this.client.getWindow().getHandle();
                boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                if (mouseDown && mouseX >= sliderX - 4 && mouseX <= sliderX + sliderW + 4 && mouseY >= y && mouseY <= y + 20) {
                    double newPct = (double) (mouseX - sliderX) / (double) sliderW;
                    double newVal = ns.getMin() + newPct * (ns.getMax() - ns.getMin());
                    ns.setValue(newVal);
                    ModuleManager.getInstance().saveConfig();
                }
            }

            RenderUtils.drawPill(context, sliderX, sliderY, sliderW, sliderH, 0xFF1F212E, 0xFF1F212E);
            int fillW = (int) (sliderW * pct);
            if (fillW > 0) {
                RenderUtils.drawPill(context, sliderX, sliderY, fillW, sliderH, 0xFFF5A65B, 0xFFF5A65B);
            }
            RenderUtils.drawPill(context, sliderX + fillW - 4, sliderY - 2, 10, 10, 0xFFFFFFFF, 0xFFFFFFFF);

            String valStr = String.format("%.2f%s", ns.getValue(), ns.getSuffix());
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(valStr), sliderX + sliderW + 6, y + 4, 0xFF8A8E9E);
        } else if (s instanceof ModeSetting ms) {
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(ms.getName()), x, y + 4, 0xFFC0C4D4);

            int tagW = tr.getWidth(ms.getValue()) + 14;
            int tagH = 16;
            int tagX = x + w - tagW;
            RenderUtils.drawRoundedRect(context, tagX, y + 2, tagW, tagH, 3, 0xFF1F212E);
            RenderUtils.drawRoundedOutline(context, tagX, y + 2, tagW, tagH, 3, 0xFF35384B);
            RenderUtils.drawCenteredTextWithShadow(context, tr, Text.literal(ms.getValue()), tagX + tagW / 2, y + 6, 0xFFF5A65B);
        } else if (s instanceof ColorSetting cs) {
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(cs.getName()), x, y + 4, 0xFFC0C4D4);
            int boxSize = 14;
            int boxX = x + w - boxSize;
            int boxY = y + 3;
            RenderUtils.drawRoundedRect(context, boxX, boxY, boxSize, boxSize, 3, cs.getColor());
            RenderUtils.drawRoundedOutline(context, boxX, boxY, boxSize, boxSize, 3, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (click == null) return false;
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Клик по шапке для перемещения окна
            if (mouseX >= guiX && mouseX <= guiX + guiW - 180 && mouseY >= guiY && mouseY <= guiY + 38) {
                draggingWindow = true;
                dragOffsetX = (int) (mouseX - guiX);
                dragOffsetY = (int) (mouseY - guiY);
                return true;
            }

            // Кнопка HUD редактора
            int hudBtnW = 86;
            int hudBtnH = 20;
            int hudBtnX = guiX + guiW - hudBtnW - 10;
            int hudBtnY = guiY + 9;
            if (mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH) {
                if (this.client != null) {
                    this.client.setScreen(new HudEditorScreen(this));
                }
                return true;
            }

            // Кнопка префиксов
            int pfxBtnW = 76;
            int pfxBtnX = hudBtnX - pfxBtnW - 6;
            if (mouseX >= pfxBtnX && mouseX <= pfxBtnX + pfxBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH) {
                if (this.client != null) {
                    this.client.setScreen(new PrefixMenuScreen(ClientPrefixConfig.loadOrCreate()));
                }
                return true;
            }

            // Клик по категориям слева
            int sidebarW = 140;
            int catY = guiY + 38 + 10;
            int catH = 28;
            for (Category cat : Category.values()) {
                if (mouseX >= guiX + 8 && mouseX <= guiX + sidebarW - 8 && mouseY >= catY && mouseY <= catY + catH) {
                    selectedCategory = cat;
                    expandedModule = null;
                    return true;
                }
                catY += catH + 4;
            }

            // Клик по модулям
            int areaX = guiX + sidebarW + 12;
            int areaY = guiY + 38 + 10;
            int areaW = guiW - sidebarW - 24;

            List<Module> moduleList = ModuleManager.getInstance().getModulesByCategory(selectedCategory);
            int curY = areaY;

            for (Module m : moduleList) {
                int cardH = 36;

                // Клик по бинду
                int bindW = 42;
                int bindH = 18;
                int bindX = areaX + areaW - 88;
                int bindY = curY + 9;
                if (mouseX >= bindX && mouseX <= bindX + bindW && mouseY >= bindY && mouseY <= bindY + bindH) {
                    listeningKeybindModule = (listeningKeybindModule == m) ? null : m;
                    return true;
                }

                // Клик по переключателю модуля
                int toggleW = 30;
                int toggleH = 16;
                int toggleX = areaX + areaW - 38;
                int toggleY = curY + 10;
                if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH) {
                    m.toggle();
                    ModuleManager.getInstance().saveConfig();
                    return true;
                }

                // Клик по карточке модуля (раскрытие настроек)
                if (mouseX >= areaX && mouseX <= areaX + areaW && mouseY >= curY && mouseY <= curY + cardH) {
                    expandedModule = (expandedModule == m) ? null : m;
                    return true;
                }

                curY += cardH + 6;

                if (m == expandedModule && !m.getSettings().isEmpty()) {
                    int setY = curY + 6;
                    for (Setting<?> s : m.getSettings()) {
                        if (s instanceof BooleanSetting bs) {
                            if (mouseX >= areaX + 20 && mouseX <= areaX + areaW - 20 && mouseY >= setY && mouseY <= setY + 20) {
                                bs.toggle();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            }
                        } else if (s instanceof ModeSetting ms) {
                            if (mouseX >= areaX + 20 && mouseX <= areaX + areaW - 20 && mouseY >= setY && mouseY <= setY + 20) {
                                ms.cycle();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            }
                        } else if (s instanceof ColorSetting cs) {
                            if (mouseX >= areaX + 20 && mouseX <= areaX + areaW - 20 && mouseY >= setY && mouseY <= setY + 20) {
                                int[] palette = {0xFFFFFFFF, 0xFFF5A65B, 0xFF6D28D9, 0xFF06B6D4, 0xFF10B981, 0xFFFF2A55};
                                int cur = cs.getColor();
                                int nextIdx = 0;
                                for (int idx = 0; idx < palette.length; idx++) {
                                    if (palette[idx] == cur) {
                                        nextIdx = (idx + 1) % palette.length;
                                        break;
                                    }
                                }
                                cs.setValue(palette[nextIdx]);
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            }
                        }
                        setY += 26;
                    }
                    curY += (m.getSettings().size() * 26 + 8) + 6;
                }
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // Правый клик по модулю — раскрытие настроек
            int sidebarW = 140;
            int areaX = guiX + sidebarW + 12;
            int areaY = guiY + 38 + 10;
            int areaW = guiW - sidebarW - 24;

            List<Module> moduleList = ModuleManager.getInstance().getModulesByCategory(selectedCategory);
            int curY = areaY;

            for (Module m : moduleList) {
                if (mouseX >= areaX && mouseX <= areaX + areaW && mouseY >= curY && mouseY <= curY + 36) {
                    expandedModule = (expandedModule == m) ? null : m;
                    return true;
                }
                curY += 36 + 6;
                if (m == expandedModule) {
                    curY += (m.getSettings().size() * 26 + 8) + 6;
                }
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput == null) return false;
        int keyCode = keyInput.key();

        // Назначение клавиши модуля
        if (listeningKeybindModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                listeningKeybindModule.setKeybind(0);
            } else {
                listeningKeybindModule.setKeybind(keyCode);
            }
            listeningKeybindModule = null;
            ModuleManager.getInstance().saveConfig();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.close();
            return true;
        }

        return super.keyPressed(keyInput);
    }
}
