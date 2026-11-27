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
import ru.example.donorprefix.gui.EclipseColors;
import ru.example.donorprefix.gui.EclipseTheme;
import ru.example.donorprefix.gui.HudEditorScreen;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.gui.UpdateNoticeOverlay;
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

    private static final Text HEADER_TITLE = Text.literal("§lECLIPSE§r §7CLIENT");
    private static final String HEADER_SUB_PREFIX = "| 1.21.11 ";
    private static final String HEADER_SUB_AUTHOR = "by alone.vendetta";
    private static final Text HUD_BTN_TEXT = Text.literal("❖ HUD РЕДАКТОР");
    private static final Text PFX_BTN_TEXT = Text.literal("✦ ПРЕФИКСЫ");

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
    private boolean sliderDirty = false;
    private int sliderModuleY = 0;
    private NumberSetting cachedSliderSetting = null;
    private double cachedSliderValue = Double.NaN;
    private Text cachedSliderText = Text.empty();

    private float openProgress = 0.0f;
    private long lastRenderTime = 0;

    // Плавные анимации состояний
    private final Map<Module, Float> moduleToggleAnims = new HashMap<>();
    private final UpdateNoticeOverlay updateOverlay = new UpdateNoticeOverlay();

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
        updateOverlay.renderModal(context, this.textRenderer, this.width, this.height, mouseX, mouseY);
    }

    private void handleMouseDragging(int mouseX, int mouseY) {
        if (updateOverlay.isModalOpen()) {
            draggingWindow = false;
            draggingSlider = null;
            return;
        }
        if (this.client == null || this.client.getWindow() == null) return;
        long handle = this.client.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (!mouseDown) {
            draggingWindow = false;
            draggingSlider = null;
            if (sliderDirty) {
                sliderDirty = false;
                ModuleManager.getInstance().saveConfig();
            }
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

        // Фон главного окна ClickGUI (плоский тёмный #09090B с тонкой 1px рамкой #24242A)
        RenderUtils.drawFlatCard(context, guiX, guiY, guiW, guiH, EclipseColors.BG_WINDOW_TRANSLUCENT, EclipseColors.BORDER);

        // ══════════════════════════════════════════════════════════════════════
        // ШАПКА ОКНА (Title Bar)
        // ══════════════════════════════════════════════════════════════════════
        int headerH = 38;
        context.fill(guiX + 1, guiY + 1, guiX + guiW - 1, guiY + headerH, EclipseColors.BG_CARD);
        context.fill(guiX + 1, guiY + headerH - 1, guiX + guiW - 1, guiY + headerH, EclipseColors.BORDER);

        // Иконка и логотип ECLIPSE
        EclipseTheme.drawEclipseBadge(context, guiX + 12, guiY + 11, 16, alpha);
        RenderUtils.drawTextWithShadow(context, tr, HEADER_TITLE, guiX + 34, guiY + 15, EclipseColors.TEXT_PRIMARY);

        // Кнопка HUD редактора
        int hudBtnW = 86;
        int hudBtnH = 20;
        int hudBtnX = guiX + guiW - hudBtnW - 10;
        int hudBtnY = guiY + 9;
        boolean hoverHud = mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH;
        int hudBg = hoverHud ? EclipseColors.ACCENT_WHITE : EclipseColors.BG_CARD;
        int hudBorder = hoverHud ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
        int hudText = hoverHud ? EclipseColors.TEXT_ON_ACCENT : EclipseColors.TEXT_PRIMARY;
        RenderUtils.drawFlatCard(context, hudBtnX, hudBtnY, hudBtnW, hudBtnH, hudBg, hudBorder);
        RenderUtils.drawCenteredTextWithShadow(context, tr, HUD_BTN_TEXT, hudBtnX + hudBtnW / 2, hudBtnY + 6, hudText);

        // Кнопка меню префиксов
        int pfxBtnW = 76;
        int pfxBtnX = hudBtnX - pfxBtnW - 6;
        boolean hoverPfx = mouseX >= pfxBtnX && mouseX <= pfxBtnX + pfxBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH;
        int pfxBg = hoverPfx ? EclipseColors.ACCENT_WHITE : EclipseColors.BG_CARD;
        int pfxBorder = hoverPfx ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
        int pfxText = hoverPfx ? EclipseColors.TEXT_ON_ACCENT : EclipseColors.TEXT_PRIMARY;
        RenderUtils.drawFlatCard(context, pfxBtnX, hudBtnY, pfxBtnW, hudBtnH, pfxBg, pfxBorder);
        RenderUtils.drawCenteredTextWithShadow(context, tr, PFX_BTN_TEXT, pfxBtnX + pfxBtnW / 2, hudBtnY + 6, pfxText);

        // Бейдж обновления в шапке (если есть новая версия)
        int badgeW = updateOverlay.getBadgeWidth(tr);
        int badgeX = pfxBtnX - badgeW - 6;
        if (badgeW == 0 || badgeX >= guiX + 255) {
            RenderUtils.drawTextWithShadow(context, tr, HEADER_SUB_PREFIX, guiX + 126, guiY + 15, EclipseColors.TEXT_SECONDARY);
            int authorX = guiX + 126 + tr.getWidth(HEADER_SUB_PREFIX);
            RenderUtils.drawMonochromeShimmer(context, tr, HEADER_SUB_AUTHOR, authorX, guiY + 15, alpha);
        }
        if (badgeW > 0) {
            updateOverlay.renderBadge(context, tr, badgeX, hudBtnY, badgeW, hudBtnH, mouseX, mouseY);
        }

        // ══════════════════════════════════════════════════════════════════════
        // ЛЕВАЯ ПАНЕЛЬ КАТЕГОРИЙ (Sidebar)
        // ══════════════════════════════════════════════════════════════════════
        int sidebarW = 140;
        int contentY = guiY + headerH;

        context.fill(guiX + 1, contentY, guiX + sidebarW, guiY + guiH - 1, EclipseColors.BG_WINDOW);
        context.fill(guiX + sidebarW - 1, contentY, guiX + sidebarW, guiY + guiH - 1, EclipseColors.BORDER);

        int catY = contentY + 10;
        int catH = 28;
        for (Category cat : Category.values()) {
            boolean isSelected = cat == selectedCategory;
            boolean hovered = mouseX >= guiX + 8 && mouseX <= guiX + sidebarW - 8 && mouseY >= catY && mouseY <= catY + catH;

            int itemBg = isSelected ? EclipseColors.BG_SIDEBAR_ACTIVE : (hovered ? EclipseColors.BG_HOVER : 0x00000000);
            if ((itemBg & 0xFF000000) != 0) {
                context.fill(guiX + 8, catY, guiX + sidebarW - 8, catY + catH, itemBg);
            }

            if (isSelected) {
                context.fill(guiX + 8, catY + 4, guiX + 10, catY + catH - 4, EclipseColors.ACCENT_WHITE);
            }

            int textColor = (isSelected || hovered) ? EclipseColors.TEXT_PRIMARY : EclipseColors.TEXT_SECONDARY;
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
            int cardBg = cardHover ? EclipseColors.BG_HOVER : EclipseColors.BG_CARD;
            int cardBorder = (m.isEnabled() || cardHover) ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;

            // Плоская карточка модуля с чёткой 1px рамкой
            RenderUtils.drawFlatCard(context, areaX, curY, areaW, cardH, cardBg, cardBorder);

            // Название и описание модуля
            int nameCol = m.isEnabled() ? EclipseColors.TEXT_PRIMARY : EclipseColors.TEXT_VERSION;
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(m.getName()), areaX + 10, curY + 8, nameCol);
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(m.getDescription()), areaX + 10, curY + 21, EclipseColors.TEXT_SECONDARY);

            // Кнопка бинда клавиши
            int bindW = 42;
            int bindH = 18;
            int bindX = areaX + areaW - 88;
            int bindY = curY + 9;
            boolean isListening = m == listeningKeybindModule;
            String bindName = isListening ? "..." : (m.getKeybind() > 0 ? GLFW.glfwGetKeyName(m.getKeybind(), 0) : "NONE");
            if (bindName == null || bindName.isEmpty()) bindName = "K" + m.getKeybind();
            if (m.getKeybind() == 0) bindName = "-";

            int bindBg = isListening ? EclipseColors.ACCENT_WHITE : EclipseColors.BG_WINDOW;
            int bindBorder = isListening ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
            int bindText = isListening ? EclipseColors.TEXT_ON_ACCENT : EclipseColors.TEXT_SECONDARY;
            RenderUtils.drawFlatCard(context, bindX, bindY, bindW, bindH, bindBg, bindBorder);
            RenderUtils.drawCenteredTextWithShadow(context, tr, Text.literal(bindName), bindX + bindW / 2, bindY + 5, bindText);

            // Монохромный тумблер
            int toggleW = 30;
            int toggleH = 16;
            int toggleX = areaX + areaW - 38;
            int toggleY = curY + 10;

            int toggleBg = AnimationUtil.lerpColor(EclipseColors.TRACK_BG, EclipseColors.ACCENT_WHITE, anim);
            int toggleBorder = AnimationUtil.lerpColor(EclipseColors.BORDER, EclipseColors.BORDER_ACTIVE, anim);
            RenderUtils.drawFlatCard(context, toggleX, toggleY, toggleW, toggleH, toggleBg, toggleBorder);

            int knobSize = 12;
            int knobX = (int) (toggleX + 2 + anim * (toggleW - knobSize - 4));
            int knobCol = AnimationUtil.lerpColor(EclipseColors.TEXT_SECONDARY, EclipseColors.TEXT_ON_ACCENT, anim);
            context.fill(knobX, toggleY + 2, knobX + knobSize, toggleY + 2 + knobSize, knobCol);

            curY += cardH + 6;

            // Если модуль раскрыт — рендерим его настройки
            if (isExpanded && !m.getSettings().isEmpty()) {
                int settingsPadding = 8;
                int settingsH = m.getSettings().size() * 26 + settingsPadding;
                RenderUtils.drawFlatCard(context, areaX + 10, curY, areaW - 20, settingsH, EclipseColors.BG_WINDOW, EclipseColors.BORDER);

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
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(bs.getName()), x, y + 4, EclipseColors.TEXT_PRIMARY);
            int toggleW = 24;
            int toggleH = 13;
            int toggleX = x + w - toggleW;
            boolean on = bs.isEnabled();
            int toggleBg = on ? EclipseColors.ACCENT_WHITE : EclipseColors.TRACK_BG;
            int toggleBorder = on ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
            RenderUtils.drawFlatCard(context, toggleX, y + 2, toggleW, toggleH, toggleBg, toggleBorder);
            int knobX = on ? toggleX + toggleW - 11 : toggleX + 2;
            int knobCol = on ? EclipseColors.TEXT_ON_ACCENT : EclipseColors.TEXT_SECONDARY;
            context.fill(knobX, y + 4, knobX + 9, y + 13, knobCol);
        } else if (s instanceof NumberSetting ns) {
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(ns.getName()), x, y + 4, EclipseColors.TEXT_PRIMARY);

            int sliderW = 100;
            int sliderH = 6;
            int sliderX = x + w - sliderW - 35;
            int sliderY = y + 7;

            // Ползунок
            double pct = (ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin());
            pct = Math.max(0.0, Math.min(1.0, pct));

            // Проверка перетаскивания слайдера мышью (без записи на диск на каждом пикселе)
            if (this.client != null && this.client.getWindow() != null) {
                long handle = this.client.getWindow().getHandle();
                boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                if (mouseDown && mouseX >= sliderX - 4 && mouseX <= sliderX + sliderW + 4 && mouseY >= y && mouseY <= y + 20) {
                    double newPct = (double) (mouseX - sliderX) / (double) sliderW;
                    double newVal = ns.getMin() + newPct * (ns.getMax() - ns.getMin());
                    if (Math.abs(newVal - ns.getValue()) > 1e-6) {
                        ns.setValue(newVal);
                        sliderDirty = true;
                    }
                }
            }

            RenderUtils.drawFlatCard(context, sliderX, sliderY, sliderW, sliderH, EclipseColors.TRACK_BG, EclipseColors.BORDER);
            int fillW = (int) (sliderW * pct);
            if (fillW > 0) {
                context.fill(sliderX, sliderY, sliderX + fillW, sliderY + sliderH, EclipseColors.ACCENT_WHITE);
            }
            context.fill(sliderX + fillW - 3, sliderY - 2, sliderX + fillW + 5, sliderY + sliderH + 2, EclipseColors.ACCENT_WHITE);
            RenderUtils.drawBorder(context, sliderX + fillW - 3, sliderY - 2, 8, sliderH + 4, EclipseColors.BG_WINDOW);

            double curVal = ns.getValue();
            if (cachedSliderSetting != ns || Double.compare(cachedSliderValue, curVal) != 0) {
                cachedSliderSetting = ns;
                cachedSliderValue = curVal;
                int scaled = (int) Math.round(curVal * 100.0);
                int sign = scaled < 0 ? -1 : 1;
                int abs = Math.abs(scaled);
                cachedSliderText = Text.literal((sign < 0 ? "-" : "") + (abs / 100) + "."
                        + ((abs % 100) < 10 ? "0" : "") + (abs % 100) + ns.getSuffix());
            }
            RenderUtils.drawTextWithShadow(context, tr, cachedSliderText, sliderX + sliderW + 6, y + 4, EclipseColors.TEXT_SECONDARY);
        } else if (s instanceof ModeSetting ms) {
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(ms.getName()), x, y + 4, EclipseColors.TEXT_PRIMARY);

            int tagW = tr.getWidth(ms.getValue()) + 14;
            int tagH = 16;
            int tagX = x + w - tagW;
            RenderUtils.drawFlatCard(context, tagX, y + 2, tagW, tagH, EclipseColors.TRACK_BG, EclipseColors.BORDER);
            RenderUtils.drawCenteredTextWithShadow(context, tr, Text.literal(ms.getValue()), tagX + tagW / 2, y + 6, EclipseColors.TEXT_PRIMARY);
        } else if (s instanceof ColorSetting cs) {
            RenderUtils.drawTextWithShadow(context, tr, Text.literal(cs.getName()), x, y + 4, EclipseColors.TEXT_PRIMARY);
            int boxSize = 14;
            int boxX = x + w - boxSize;
            int boxY = y + 3;
            RenderUtils.drawFlatCard(context, boxX, boxY, boxSize, boxSize, cs.getColor(), EclipseColors.BORDER_ACTIVE);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (click == null) return false;
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (this.textRenderer != null && updateOverlay.handleModalClick(mouseX, mouseY, button, this.width, this.height, this.textRenderer)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int hudBtnW = 86;
            int hudBtnH = 20;
            int hudBtnX = guiX + guiW - hudBtnW - 10;
            int hudBtnY = guiY + 9;
            int pfxBtnW = 76;
            int pfxBtnX = hudBtnX - pfxBtnW - 6;
            int badgeW = updateOverlay.getBadgeWidth(this.textRenderer);
            if (badgeW > 0) {
                int badgeX = pfxBtnX - badgeW - 6;
                if (updateOverlay.handleBadgeClick(mouseX, mouseY, button, badgeX, hudBtnY, badgeW, hudBtnH)) {
                    return true;
                }
            }

            // Клик по шапке для перемещения окна
            if (mouseX >= guiX && mouseX <= guiX + guiW - 180 && mouseY >= guiY && mouseY <= guiY + 38) {
                draggingWindow = true;
                dragOffsetX = (int) (mouseX - guiX);
                dragOffsetY = (int) (mouseY - guiY);
                return true;
            }

            // Кнопка HUD редактора
            if (mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH) {
                if (this.client != null) {
                    this.client.setScreen(new HudEditorScreen(this));
                }
                return true;
            }

            // Кнопка префиксов
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
                                int[] palette = {0xFFF4F4F5, 0xFFD4D4D8, 0xFFA1A1AA, 0xFF71717A, 0xFF24242A, 0xFF09090B};
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

        if (updateOverlay.handleKeyPressed(keyCode)) {
            return true;
        }

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

    @Override
    public void close() {
        if (sliderDirty) {
            sliderDirty = false;
            ModuleManager.getInstance().syncModulesToConfigs();
        }
        ru.eclipse.client.core.EclipseStateSync.getInstance().flushPendingSave();
        super.close();
    }
}
