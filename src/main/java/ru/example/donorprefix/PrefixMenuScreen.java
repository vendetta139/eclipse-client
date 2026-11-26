package ru.example.donorprefix;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import ru.example.donorprefix.gui.AnimationUtil;
import ru.example.donorprefix.gui.ColorPickerModal;
import ru.example.donorprefix.gui.EclipseTextFieldWidget;
import ru.example.donorprefix.gui.EclipseTheme;
import ru.example.donorprefix.gui.HudEditorScreen;
import ru.example.donorprefix.gui.ModernButtonWidget;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.VisualConfig;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ✦ ECLIPSE CLIENT v2.0 — Премиальный переработанный интерфейс в стиле
 * Cyberpunk Dark Glassmorphism с неоновыми акцентами, живым поиском параметров,
 * интерактивными тумблерами и предпросмотром префиксов.
 */
public class PrefixMenuScreen extends Screen {

    public enum ScreenTab {
        PREFIXES("✦ Префиксы", "Ротация и управление донат-префиксами"),
        COMBAT("⚔ Бой & PvP", "Hurt Color, No Hurt Cam, хитмаркеры и цифры урона"),
        ANIMATIONS("🗡 Анимации", "1.7 Swing, BlockHit, размер рук и заниженный щит"),
        WORLD("🌌 Мир & Туман", "Fullbright, Low Fire, кастомный туман, погода и глит"),
        HUD("📊 HUD & Оверлеи", "Прицел, Target HUD, броня, зелья, CPS, Keystrokes"),
        THEME("🎨 Тема & Стиль", "Modern HD TrueType шрифт и стиль серверных панелей");

        public final String title;
        public final String description;

        ScreenTab(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private ScreenTab currentTab = ScreenTab.PREFIXES;
    private ScreenTab targetTab = ScreenTab.PREFIXES;

    // Габариты диалогового окна (Просторный современный интерфейс)
    private int dialogWidth = 720;
    private int dialogHeight = 440;
    private int dialogLeft;
    private int dialogTop;

    // Сайдбар
    private final int sidebarWidth = 160;

    // Область контента
    private int contentLeft;
    private int contentRight;
    private int contentTop;
    private int contentBottom;

    // Анимации открытия / закрытия / переключения
    private long openStartTime = 0;
    private boolean isClosing = false;
    private long closeStartTime = 0;
    private static final float OPEN_DURATION_MS = 220f;
    private static final float CLOSE_DURATION_MS = 180f;

    private long tabSwitchStartTime = 0;
    private static final float TAB_SWITCH_DURATION_MS = 180f;
    private float animatedTabY = 0f;

    // Анимации тумблеров (key -> progress 0..1)
    private final Map<String, Float> toggleAnimMap = new HashMap<>();
    private final Map<String, Float> sliderAnimMap = new HashMap<>();
    private long lastScreenRenderTime = 0;
    private float screenDeltaSec = 0.016f;
    private float animationTime = 0f;

    // Скролл для каждой вкладки
    private final Map<ScreenTab, Double> tabScrollMap = new EnumMap<>(ScreenTab.class);
    private boolean isDraggingScrollbar = false;
    private double dragStartY = 0;
    private double dragStartScroll = 0;
    private double searchScroll = 0;

    // Конфигурации
    private final ClientPrefixConfig config;
    private final VisualConfig visualConfig;

    // Интерактивная палитра цветов
    private final ColorPickerModal colorPicker = new ColorPickerModal();

    // Сайдбарные кнопки и заголовок
    private final List<ModernButtonWidget> sidebarTabButtons = new ArrayList<>();
    private ModernButtonWidget masterStatusButton;
    private ModernButtonWidget closeButton;
    private ModernButtonWidget hudEditBtn;

    // Глобальное поле поиска в шапке
    private EclipseTextFieldWidget searchField;
    private String searchQuery = "";

    // Виджеты вкладки "Префиксы"
    private final List<PrefixCard> prefixCards = new ArrayList<>();
    private ModernButtonWidget addPrefixButton;
    private EclipseTextFieldWidget cooldownField;
    private EclipseTextFieldWidget templateField;
    private ModernButtonWidget cooldownDecrBtn;
    private ModernButtonWidget cooldownIncrBtn;
    private ModernButtonWidget savePrefixesBtn;

    // Интерактивные строки настроек (тумблеры, слайдеры, селекторы, цвета)
    private final List<SettingRow> settingRows = new ArrayList<>();
    private SettingRow activeDraggingSlider = null;

    // Всплывающие уведомления (Toast)
    private Text toastText = null;
    private int toastColor = 0xFFFFFFFF;
    private long toastExpireTime = 0;
    private static final long TOAST_DURATION_MS = 2600;

    private static class PrefixCard {
        EclipseTextFieldWidget field;
        ModernButtonWidget upButton;
        ModernButtonWidget downButton;
        ModernButtonWidget testButton;
        ModernButtonWidget deleteButton;
        String initialText;
        String lastPreviewRaw;
        MutableText cachedPreview;

        PrefixCard(String initialText) {
            this.initialText = initialText != null ? initialText : "";
        }
    }

    public enum RowType {
        HEADER, TOGGLE, SLIDER, COLOR, SEGMENTED, BUTTON
    }

    public static class SettingRow {
        String key;
        String title;
        String description;
        RowType type;
        ScreenTab tab;

        // Позиция при рендере
        int x, y, width, height;

        // Для Toggle
        Supplier<Boolean> toggleGetter;
        Consumer<Boolean> toggleSetter;

        // Для Slider
        Supplier<Float> sliderGetter;
        Consumer<Float> sliderSetter;
        float sliderMin, sliderMax, sliderStep;
        String sliderSuffix;
        float lastSliderVal = Float.NaN;
        Text cachedSliderText = null;
        Text cachedTitleText = null;

        // Для Color
        Supplier<Integer> colorGetter;
        Consumer<Integer> colorSetter;
        Supplier<Boolean> rainbowGetter;
        Consumer<Boolean> rainbowSetter;

        // Для Segmented
        String[] options;
        Supplier<Integer> optionGetter;
        Consumer<Integer> optionSetter;

        // Для Action Button
        String buttonText;
        Runnable buttonAction;
    }

    public PrefixMenuScreen(ClientPrefixConfig config, VisualConfig visualConfig) {
        super(Text.literal("Eclipse Client Menu"));
        this.config = config != null ? config : ClientPrefixConfig.loadOrCreate();
        this.visualConfig = visualConfig != null ? visualConfig : VisualConfig.getInstance();
        for (ScreenTab tab : ScreenTab.values()) {
            tabScrollMap.put(tab, 0.0);
        }
    }

    public PrefixMenuScreen(ClientPrefixConfig config) {
        this(config, VisualConfig.getInstance());
    }

    public PrefixMenuScreen() {
        this(ClientPrefixConfig.loadOrCreate());
    }

    @Override
    protected void init() {
        super.init();
        try {
            this.openStartTime = System.currentTimeMillis();
            this.isClosing = false;
            this.tabSwitchStartTime = System.currentTimeMillis();

            this.dialogWidth = Math.min(730, Math.max(620, this.width - 36));
            this.dialogHeight = Math.min(445, Math.max(380, this.height - 28));

            this.dialogLeft = (this.width - dialogWidth) / 2;
            this.dialogTop = (this.height - dialogHeight) / 2;

            this.contentLeft = dialogLeft + sidebarWidth + 12;
            this.contentRight = dialogLeft + dialogWidth - 12;
            this.contentTop = dialogTop + 42; // Просторная шапка сверху
            this.contentBottom = dialogTop + dialogHeight - 12;

            initHeaderWidgets();
            initSidebar();
            initTabContent();
            rebuildSettingRows();
        } catch (Throwable t) {
            System.err.println("[EclipseClient] Error during PrefixMenuScreen.init(): " + t.getMessage());
            t.printStackTrace();
        }
    }

    private boolean isSearching() {
        return searchQuery != null && !searchQuery.isEmpty();
    }

    private void hidePrefixTabWidgets() {
        for (PrefixCard card : prefixCards) {
            setCardWidgetsVisible(card, false);
        }
        if (addPrefixButton != null) addPrefixButton.visible = false;
        if (cooldownField != null) cooldownField.setVisible(false);
        if (cooldownDecrBtn != null) cooldownDecrBtn.visible = false;
        if (cooldownIncrBtn != null) cooldownIncrBtn.visible = false;
        if (templateField != null) templateField.setVisible(false);
        if (savePrefixesBtn != null) savePrefixesBtn.visible = false;
    }

    private void initHeaderWidgets() {
        // Поле живого поиска в шапке
        int searchW = 180;
        int searchH = 18;
        int searchX = dialogLeft + sidebarWidth + 14;
        int searchY = dialogTop + 12;

        searchField = new EclipseTextFieldWidget(this.textRenderer, searchX, searchY, searchW, searchH, Text.literal(""));
        searchField.setCustomPlaceholder(Text.literal("Поиск..."));
        searchField.setMaxLength(30);
        searchField.setText(searchQuery);
        searchField.setChangedListener(text -> {
            String prev = this.searchQuery;
            this.searchQuery = text.trim().toLowerCase();
            if (!this.searchQuery.equals(prev)) {
                setScroll(0);
                if (isSearching()) {
                    hidePrefixTabWidgets();
                    repositionSettingRows();
                } else {
                    if (currentTab == ScreenTab.PREFIXES) {
                        repositionPrefixCards();
                    } else {
                        repositionSettingRows();
                    }
                }
            }
        });
        this.addDrawableChild(searchField);

        // Кнопка мастера ротации в правом углу шапки (• НА ПАУЗЕ / • РОТАЦИЯ: ВКЛ)
        int statusW = 120;
        int statusH = 18;
        int statusX = dialogLeft + dialogWidth - statusW - 32;
        int statusY = dialogTop + 12;

        masterStatusButton = ModernButtonWidget.modern(
                Text.literal(config.enabled ? "• РОТАЦИЯ: ВКЛ" : "• НА ПАУЗЕ"),
                b -> {
                    config.enabled = !config.enabled;
                    ru.eclipse.client.module.ModuleManager.getInstance().syncConfigsToModules();
                    config.save();
                    updateMasterStatusButton();
                    showToast(config.enabled ? "Авто-ротация активирована" : "Авто-ротация на паузе",
                            config.enabled ? 0xFF10B981 : 0xFFF5A65B);
                }
        ).dimensions(statusX, statusY, statusW, statusH)
                .style(config.enabled ? ModernButtonWidget.Style.STATUS_ACTIVE : ModernButtonWidget.Style.STATUS_PAUSED)
                .build();
        masterStatusButton.setCornerRadius(9);
        this.addDrawableChild(masterStatusButton);

        // Кнопка закрытия [✕]
        int closeSize = 18;
        int closeX = dialogLeft + dialogWidth - closeSize - 8;
        int closeY = dialogTop + 12;
        closeButton = ModernButtonWidget.modern(
                Text.literal("✕"),
                b -> startClosing()
        ).dimensions(closeX, closeY, closeSize, closeSize).style(ModernButtonWidget.Style.ACTION_NAV).build();
        closeButton.setCornerRadius(4);
        this.addDrawableChild(closeButton);
    }

    private void updateMasterStatusButton() {
        if (masterStatusButton != null) {
            masterStatusButton.setMessage(Text.literal(config.enabled ? "• РОТАЦИЯ: ВКЛ" : "• НА ПАУЗЕ"));
            masterStatusButton.setStyle(config.enabled ? ModernButtonWidget.Style.STATUS_ACTIVE : ModernButtonWidget.Style.STATUS_PAUSED);
        }
    }

    private void initSidebar() {
        sidebarTabButtons.clear();
        int btnY = dialogTop + 48;
        int btnW = sidebarWidth - 16;
        int btnH = 28;

        ScreenTab[] tabs = ScreenTab.values();
        for (int i = 0; i < tabs.length; i++) {
            ScreenTab tab = tabs[i];
            int finalY = btnY;
            ModernButtonWidget tabBtn = ModernButtonWidget.modern(
                    Text.literal(tab.title),
                    b -> switchTab(tab)
            ).dimensions(dialogLeft + 8, btnY, btnW, btnH)
                    .style(tab == currentTab ? ModernButtonWidget.Style.TAB_ACTIVE : ModernButtonWidget.Style.TAB_INACTIVE)
                    .build();
            tabBtn.setCornerRadius(6);
            sidebarTabButtons.add(tabBtn);
            this.addDrawableChild(tabBtn);

            if (currentTab == tab && animatedTabY == 0f) {
                animatedTabY = finalY;
            }
            btnY += btnH + 5;
        }

        // Кнопка HUD-редактора внизу сайдбара
        int hudBtnW = sidebarWidth - 16;
        int hudBtnH = 24;
        int hudBtnY = dialogTop + dialogHeight - 34;
        this.hudEditBtn = ModernButtonWidget.modern(
                Text.literal("✦ HUD Редактор"),
                b -> {
                    if (this.client != null) {
                        this.client.setScreen(new HudEditorScreen(this));
                    }
                }
        ).dimensions(dialogLeft + 8, hudBtnY, hudBtnW, hudBtnH).style(ModernButtonWidget.Style.PRIMARY).build();
        this.hudEditBtn.setCornerRadius(6);
        this.addDrawableChild(this.hudEditBtn);
    }

    private void switchTab(ScreenTab newTab) {
        if (this.currentTab != newTab || isSearching()) {
            this.currentTab = newTab;
            this.targetTab = newTab;
            this.tabSwitchStartTime = System.currentTimeMillis();
            if (isSearching()) {
                this.searchQuery = "";
                if (searchField != null) {
                    searchField.setText("");
                }
            }
            for (int i = 0; i < sidebarTabButtons.size(); i++) {
                sidebarTabButtons.get(i).setStyle(ScreenTab.values()[i] == currentTab ? ModernButtonWidget.Style.TAB_ACTIVE : ModernButtonWidget.Style.TAB_INACTIVE);
            }
            colorPicker.close();
            initTabContent();
        }
    }

    private void initTabContent() {
        clearContentWidgets();

        if (currentTab == ScreenTab.PREFIXES) {
            initPrefixTabWidgets();
        }
        rebuildSettingRows();
    }

    private void clearContentWidgets() {
        for (PrefixCard card : prefixCards) {
            if (card.field != null) safeRemove(card.field);
            if (card.upButton != null) safeRemove(card.upButton);
            if (card.downButton != null) safeRemove(card.downButton);
            if (card.testButton != null) safeRemove(card.testButton);
            if (card.deleteButton != null) safeRemove(card.deleteButton);
        }
        prefixCards.clear();

        if (addPrefixButton != null) { safeRemove(addPrefixButton); addPrefixButton = null; }
        if (cooldownField != null) { safeRemove(cooldownField); cooldownField = null; }
        if (templateField != null) { safeRemove(templateField); templateField = null; }
        if (cooldownDecrBtn != null) { safeRemove(cooldownDecrBtn); cooldownDecrBtn = null; }
        if (cooldownIncrBtn != null) { safeRemove(cooldownIncrBtn); cooldownIncrBtn = null; }
        if (savePrefixesBtn != null) { safeRemove(savePrefixesBtn); savePrefixesBtn = null; }

        settingRows.clear();
        activeDraggingSlider = null;
    }

    private void safeRemove(net.minecraft.client.gui.Element child) {
        if (child != null) {
            try {
                this.remove(child);
            } catch (Throwable ignored) {}
        }
    }

    // =========================================================================
    //  ВКЛАДКА "ПРЕФИКСЫ" (Prefix Rotator Manager)
    // =========================================================================
    private void initPrefixTabWidgets() {
        int listWidth = contentRight - contentLeft - 14;
        int cardH = 50;

        for (int i = 0; i < config.prefixes.size(); i++) {
            String pVal = config.prefixes.get(i);
            PrefixCard card = new PrefixCard(pVal);

            card.field = new EclipseTextFieldWidget(this.textRenderer, 0, 0, listWidth - 148, 18, Text.literal("Префикс"));
            card.field.setMaxLength(128);
            card.field.setText(pVal);
            this.addDrawableChild(card.field);

            int finalI = i;
            card.upButton = ModernButtonWidget.modern(Text.literal("▲"), b -> movePrefixUp(finalI))
                    .dimensions(0, 0, 20, 18).style(ModernButtonWidget.Style.ACTION_NAV).build();
            card.upButton.setCornerRadius(4);
            card.upButton.active = (i > 0);
            this.addDrawableChild(card.upButton);

            card.downButton = ModernButtonWidget.modern(Text.literal("▼"), b -> movePrefixDown(finalI))
                    .dimensions(0, 0, 20, 18).style(ModernButtonWidget.Style.ACTION_NAV).build();
            card.downButton.setCornerRadius(4);
            card.downButton.active = (i < config.prefixes.size() - 1);
            this.addDrawableChild(card.downButton);

            card.testButton = ModernButtonWidget.modern(Text.literal("►"), b -> testPrefixNow(finalI))
                    .dimensions(0, 0, 22, 18).style(ModernButtonWidget.Style.ACTION_TEST).build();
            card.testButton.setCornerRadius(4);
            this.addDrawableChild(card.testButton);

            card.deleteButton = ModernButtonWidget.modern(Text.literal("✕"), b -> deletePrefix(finalI))
                    .dimensions(0, 0, 20, 18).style(ModernButtonWidget.Style.ACTION_DELETE).build();
            card.deleteButton.setCornerRadius(4);
            this.addDrawableChild(card.deleteButton);

            prefixCards.add(card);
        }

        // Кнопка "+ Добавить префикс"
        addPrefixButton = ModernButtonWidget.modern(
                Text.literal("+ Добавить префикс"),
                b -> addNewPrefix()
        ).dimensions(0, 0, listWidth, 22).style(ModernButtonWidget.Style.SECONDARY).build();
        addPrefixButton.setCornerRadius(6);
        this.addDrawableChild(addPrefixButton);

        // Кулдаун
        cooldownField = new EclipseTextFieldWidget(this.textRenderer, 0, 0, 44, 18, Text.literal("Кулдаун"));
        cooldownField.setText(String.valueOf(config.cooldownSeconds));
        cooldownField.setMaxLength(5);
        this.addDrawableChild(cooldownField);

        cooldownDecrBtn = ModernButtonWidget.modern(Text.literal("-5с"), b -> adjustCooldown(-5))
                .dimensions(0, 0, 26, 18).style(ModernButtonWidget.Style.STEPPER).build();
        cooldownDecrBtn.setCornerRadius(4);
        this.addDrawableChild(cooldownDecrBtn);

        cooldownIncrBtn = ModernButtonWidget.modern(Text.literal("+5с"), b -> adjustCooldown(+5))
                .dimensions(0, 0, 26, 18).style(ModernButtonWidget.Style.STEPPER).build();
        cooldownIncrBtn.setCornerRadius(4);
        this.addDrawableChild(cooldownIncrBtn);

        // Шаблон команды
        templateField = new EclipseTextFieldWidget(this.textRenderer, 0, 0, 150, 18, Text.literal("Шаблон команды"));
        templateField.setText(config.commandTemplate != null ? config.commandTemplate : "prefix set %s");
        templateField.setMaxLength(128);
        this.addDrawableChild(templateField);

        // Кнопка "Сохранить настройки"
        savePrefixesBtn = ModernButtonWidget.modern(
                Text.literal("💾 Сохранить и применить"),
                b -> savePrefixSettings()
        ).dimensions(0, 0, 150, 22).style(ModernButtonWidget.Style.PRIMARY).build();
        savePrefixesBtn.setCornerRadius(6);
        this.addDrawableChild(savePrefixesBtn);

        repositionPrefixCards();
    }

    private void repositionPrefixCards() {
        if (isSearching()) {
            hidePrefixTabWidgets();
            return;
        }
        int listWidth = contentRight - contentLeft - 14;
        int cardH = 52;
        int startY = contentTop + 4 - (int) getScroll();

        for (int i = 0; i < prefixCards.size(); i++) {
            PrefixCard card = prefixCards.get(i);
            int cardY = startY + i * (cardH + 6);
            int innerX = contentLeft + 6;
            int innerY = cardY + 22;
            boolean inViewport = (cardY + cardH >= contentTop && cardY <= contentBottom);

            if (card.field != null) {
                card.field.setX(innerX + 4);
                card.field.setY(innerY);
                card.field.setVisible(inViewport);
            }

            int btnX = innerX + listWidth - 94;
            if (card.upButton != null) {
                card.upButton.setX(btnX);
                card.upButton.setY(innerY);
                card.upButton.visible = inViewport;
            }
            if (card.downButton != null) {
                card.downButton.setX(btnX + 22);
                card.downButton.setY(innerY);
                card.downButton.visible = inViewport;
            }
            if (card.testButton != null) {
                card.testButton.setX(btnX + 44);
                card.testButton.setY(innerY);
                card.testButton.visible = inViewport;
            }
            if (card.deleteButton != null) {
                card.deleteButton.setX(btnX + 68);
                card.deleteButton.setY(innerY);
                card.deleteButton.visible = inViewport;
            }
        }

        int addY = startY + prefixCards.size() * (cardH + 6) + 4;
        boolean addVisible = (addY + 22 >= contentTop && addY <= contentBottom);
        if (addPrefixButton != null) {
            addPrefixButton.setX(contentLeft + 6);
            addPrefixButton.setY(addY);
            addPrefixButton.visible = addVisible;
        }

        int settingsY = addY + 34;
        boolean settingsVisible = (settingsY + 106 >= contentTop && settingsY <= contentBottom);
        if (cooldownField != null) {
            cooldownField.setX(contentLeft + 72);
            cooldownField.setY(settingsY + 20);
            cooldownField.setVisible(settingsVisible);
        }
        if (cooldownDecrBtn != null) {
            cooldownDecrBtn.setX(contentLeft + 120);
            cooldownDecrBtn.setY(settingsY + 20);
            cooldownDecrBtn.visible = settingsVisible;
        }
        if (cooldownIncrBtn != null) {
            cooldownIncrBtn.setX(contentLeft + 149);
            cooldownIncrBtn.setY(settingsY + 20);
            cooldownIncrBtn.visible = settingsVisible;
        }
        if (templateField != null) {
            templateField.setX(contentLeft + 72);
            templateField.setY(settingsY + 46);
            templateField.setVisible(settingsVisible);
        }
        if (savePrefixesBtn != null) {
            savePrefixesBtn.setX(contentLeft + 6);
            savePrefixesBtn.setY(settingsY + 74);
            savePrefixesBtn.visible = settingsVisible;
        }
    }

    private void addNewPrefix() {
        collectCurrentPrefixFields();
        config.prefixes.add("&b&lНОВЫЙ");
        initTabContent();
        showToast("Новый префикс добавлен в очередь", 0xFF6A67CE);
    }

    private void deletePrefix(int index) {
        collectCurrentPrefixFields();
        if (index >= 0 && index < config.prefixes.size()) {
            config.prefixes.remove(index);
            initTabContent();
            showToast("Префикс удалён", 0xFFEF4444);
        }
    }

    private void movePrefixUp(int index) {
        if (index > 0) {
            collectCurrentPrefixFields();
            Collections.swap(config.prefixes, index, index - 1);
            initTabContent();
        }
    }

    private void movePrefixDown(int index) {
        if (index < config.prefixes.size() - 1) {
            collectCurrentPrefixFields();
            Collections.swap(config.prefixes, index, index + 1);
            initTabContent();
        }
    }

    private void testPrefixNow(int index) {
        collectCurrentPrefixFields();
        if (index >= 0 && index < config.prefixes.size()) {
            String val = config.prefixes.get(index);
            boolean ok = DonorPrefixClientMod.sendPrefixCommand(this.client, config.commandTemplate, val);
            if (ok) {
                showToast("Команда отправлена на сервер!", 0xFF10B981);
            } else {
                showToast("Ошибка отправки (не на сервере?)", 0xFFEF4444);
            }
        }
    }

    private void adjustCooldown(int delta) {
        try {
            int current = Integer.parseInt(cooldownField.getText().trim());
            int newVal = Math.max(1, current + delta);
            cooldownField.setText(String.valueOf(newVal));
        } catch (NumberFormatException ignored) {}
    }

    private void collectCurrentPrefixFields() {
        for (int i = 0; i < prefixCards.size(); i++) {
            PrefixCard card = prefixCards.get(i);
            if (card.field != null && i < config.prefixes.size()) {
                config.prefixes.set(i, card.field.getText().trim());
            }
        }
        if (cooldownField != null) {
            try {
                int cd = Integer.parseInt(cooldownField.getText().trim());
                if (cd > 0) config.cooldownSeconds = cd;
            } catch (NumberFormatException ignored) {}
        }
        if (templateField != null) {
            String tmpl = templateField.getText().trim();
            if (!tmpl.isEmpty()) config.commandTemplate = tmpl;
        }
    }

    private void savePrefixSettings() {
        collectCurrentPrefixFields();
        ru.eclipse.client.module.ModuleManager.getInstance().syncConfigsToModules();
        config.save();
        if (DonorPrefixClientMod.getInstance() != null) {
            DonorPrefixClientMod.getInstance().resetTimer();
        }
        showToast("Настройки успешно сохранены!", 0xFF10B981);
    }

    // =========================================================================
    //  ДИНАМИЧЕСКИЙ СПИСОК НАСТРОЕК (Только рабочие модули 1.21.11)
    // =========================================================================
    private void rebuildSettingRows() {
        settingRows.clear();

        // 1. Вкладка COMBAT
        addCategoryHeader(ScreenTab.COMBAT, "Камера и попадания");
        addSetting(ScreenTab.COMBAT, RowType.TOGGLE, "noHurtCam", "No Hurt Cam",
                "Полное отключение резкой тряски и наклона камеры при получении ударов",
                () -> visualConfig.noHurtCam, v -> visualConfig.noHurtCam = v);

        addSetting(ScreenTab.COMBAT, RowType.TOGGLE, "hitMarkers", "Хитмаркеры (HitMarkers)",
                "Тактический неоновый крестик на прицеле при попадании по врагам",
                () -> visualConfig.hitMarkerEnabled, v -> visualConfig.hitMarkerEnabled = v);

        addSetting(ScreenTab.COMBAT, RowType.TOGGLE, "hitSound", "Звук удара (Hit Sound)",
                "Четкий звонкий акустический щелчок при успешном нанесении урона",
                () -> visualConfig.hitSoundEnabled, v -> visualConfig.hitSoundEnabled = v);

        addSegmentedSetting(ScreenTab.COMBAT, "hitSoundType", "Тип звука удара (Sound Pack)",
                "Выбор акустического эффекта при нанесении урона цели",
                new String[]{"Ding", "Skeet", "Chime", "Arrow", "Bonk"},
                () -> visualConfig.hitSoundType,
                idx -> visualConfig.hitSoundType = idx);

        // 2. Вкладка ANIMATIONS
        addCategoryHeader(ScreenTab.ANIMATIONS, "Анимации оружия и щита");
        addSetting(ScreenTab.ANIMATIONS, RowType.TOGGLE, "blockHit", "BlockHit анимация",
                "Имитация блокирования мечом при удержании правой кнопки мыши (ПКМ)",
                () -> visualConfig.blockHit, v -> visualConfig.blockHit = v);

        addSetting(ScreenTab.ANIMATIONS, RowType.TOGGLE, "lowShield", "Заниженный щит (Low Shield)",
                "Опускает щит во второй руке вниз экрана, освобождая обзор в дуэлях",
                () -> visualConfig.smallShield, v -> visualConfig.smallShield = v);

        addSliderSetting(ScreenTab.ANIMATIONS, "handScale", "Масштаб рук (Hand Scale)",
                "Настройка размера модели рук и удерживаемого предмета от первого лица",
                () -> visualConfig.handScale, s -> visualConfig.handScale = s,
                0.5f, 1.4f, 0.05f, "x");

        // 3. Вкладка WORLD
        addCategoryHeader(ScreenTab.WORLD, "Освещение, туман и огонь");
        addSetting(ScreenTab.WORLD, RowType.TOGGLE, "fullbright", "Fullbright (Макс. яркость)",
                "Идеальная видимость в пещерах и ночью без зелья ночного зрения",
                () -> visualConfig.fullbright, v -> visualConfig.fullbright = v);

        addSetting(ScreenTab.WORLD, RowType.TOGGLE, "noFog", "Режим No Fog",
                "Полное отключение тумана в мире, воде и лаве",
                () -> visualConfig.noFog, v -> visualConfig.noFog = v);

        addSetting(ScreenTab.WORLD, RowType.TOGGLE, "lowFire", "Заниженный огонь (Low Fire)",
                "Опускает текстуру горения от первого лица вниз, убирая перекрытие обзора",
                () -> visualConfig.lowFireEnabled, v -> visualConfig.lowFireEnabled = v);

        addSliderSetting(ScreenTab.WORLD, "lowFireOffset", "Смещение огня",
                "Глубина опускания текстуры огня вниз",
                () -> visualConfig.lowFireOffset, off -> visualConfig.lowFireOffset = off,
                -0.60f, -0.05f, 0.05f, "");

        addCategoryHeader(ScreenTab.WORLD, "Погода и время суток");
        addSetting(ScreenTab.WORLD, RowType.TOGGLE, "clearWeather", "Чистая погода (No Rain)",
                "Полное отключение дождя, снега и грозы на клиенте",
                () -> visualConfig.clearWeather, v -> visualConfig.clearWeather = v);

        addSetting(ScreenTab.WORLD, RowType.TOGGLE, "timeChanger", "Фиксатор времени (Time Changer)",
                "Визуальная фиксация времени суток независимо от сервера",
                () -> visualConfig.timeChangerEnabled, v -> visualConfig.timeChangerEnabled = v);

        addSegmentedSetting(ScreenTab.WORLD, "timePreset", "Время суток",
                "Выбор фиксированного времени мира",
                new String[]{"День", "Закат", "Ночь"},
                () -> {
                    long t = visualConfig.customTime;
                    if (t == 6000L) return 0;
                    if (t == 12500L || t == 12000L) return 1;
                    return 2;
                },
                idx -> {
                    long[] times = {6000L, 12500L, 18000L};
                    visualConfig.customTime = times[idx];
                });

        // 4. Вкладка HUD
        addCategoryHeader(ScreenTab.HUD, "Быстрые действия");
        addButtonSetting(ScreenTab.HUD, "hudEditor", "✦ Интерактивный HUD Редактор",
                "Нажмите, чтобы визуально перемещать и масштабировать любые HUD виджеты на экране",
                "ОТКРЫТЬ", () -> {
                    if (this.client != null) {
                        this.client.setScreen(new HudEditorScreen(this));
                    }
                });

        addCategoryHeader(ScreenTab.HUD, "Прицел и Target HUD");
        addSetting(ScreenTab.HUD, RowType.TOGGLE, "crosshair", "Кастомный прицел",
                "Тактический прицел с настраиваемым цветом",
                () -> visualConfig.customCrosshair, v -> visualConfig.customCrosshair = v);

        addSetting(ScreenTab.HUD, RowType.COLOR, "crosshairColor", "Цвет прицела",
                "Оттенок тактического прицела",
                () -> visualConfig.crosshairColor, c -> visualConfig.crosshairColor = c,
                () -> visualConfig.crosshairRainbow, r -> visualConfig.crosshairRainbow = r);

        addSetting(ScreenTab.HUD, RowType.TOGGLE, "targetHud", "Target HUD",
                "Информационный виджет противника: моделька, имя, дистанция и полоса HP",
                () -> visualConfig.targetHudEnabled, v -> visualConfig.targetHudEnabled = v);

        addSliderSetting(ScreenTab.HUD, "targetHudScale", "Масштаб Target HUD",
                "Размер виджета противника на экране (от 0.25x до 2.0x)",
                () -> visualConfig.targetHudScale, s -> visualConfig.targetHudScale = s,
                0.25f, 2.0f, 0.05f, "x");

        addSetting(ScreenTab.HUD, RowType.TOGGLE, "targetHud3D", "3D Моделька в Target HUD",
                "Живая 3D модель цели вместо значка",
                () -> visualConfig.targetHud3DModel, v -> visualConfig.targetHud3DModel = v);

        addCategoryHeader(ScreenTab.HUD, "Информационные HUD панели");
        addSetting(ScreenTab.HUD, RowType.TOGGLE, "watermark", "Watermark HUD",
                "Панель статуса: название клиента, FPS и пинг",
                () -> visualConfig.watermarkEnabled, v -> visualConfig.watermarkEnabled = v);

        addSliderSetting(ScreenTab.HUD, "watermarkScale", "Масштаб Watermark",
                "Размер статус-бара клиента (от 0.25x до 2.0x)",
                () -> visualConfig.watermarkScale, s -> visualConfig.watermarkScale = s,
                0.25f, 2.0f, 0.05f, "x");

        addSetting(ScreenTab.HUD, RowType.TOGGLE, "fpsHud", "FPS HUD",
                "Отдельный счетчик кадров в секунду (FPS)",
                () -> visualConfig.fpsHudEnabled, v -> visualConfig.fpsHudEnabled = v);

        addSliderSetting(ScreenTab.HUD, "fpsHudScale", "Масштаб FPS HUD",
                "Размер счетчика FPS (от 0.25x до 2.0x)",
                () -> visualConfig.fpsHudScale, s -> visualConfig.fpsHudScale = s,
                0.25f, 2.0f, 0.05f, "x");

        addSetting(ScreenTab.HUD, RowType.TOGGLE, "keystrokes", "Keystrokes (Клавиши)",
                "Отображение нажатий клавиш W, A, S, D, ЛКМ, ПКМ и Пробела",
                () -> visualConfig.keystrokesEnabled, v -> visualConfig.keystrokesEnabled = v);

        addSliderSetting(ScreenTab.HUD, "keystrokesScale", "Масштаб Keystrokes",
                "Размер кнопок WASD и мыши (от 0.25x до 2.0x)",
                () -> visualConfig.keystrokesScale, s -> visualConfig.keystrokesScale = s,
                0.25f, 2.0f, 0.05f, "x");

        addSetting(ScreenTab.HUD, RowType.TOGGLE, "cpsCounter", "CPS Counter",
                "Счетчик кликов в секунду (LMB & RMB)",
                () -> visualConfig.cpsCounterEnabled, v -> visualConfig.cpsCounterEnabled = v);

        addSliderSetting(ScreenTab.HUD, "cpsCounterScale", "Масштаб CPS Counter",
                "Размер счетчика кликов в секунду (от 0.25x до 2.0x)",
                () -> visualConfig.cpsCounterScale, s -> visualConfig.cpsCounterScale = s,
                0.25f, 2.0f, 0.05f, "x");

        addSetting(ScreenTab.HUD, RowType.TOGGLE, "armorHud", "Armor HUD",
                "Отображение экипированной брони с её прочностью",
                () -> visualConfig.armorHudEnabled, v -> visualConfig.armorHudEnabled = v);

        addSliderSetting(ScreenTab.HUD, "armorHudScale", "Масштаб Armor HUD",
                "Размер панели брони (от 0.25x до 2.0x)",
                () -> visualConfig.armorHudScale, s -> visualConfig.armorHudScale = s,
                0.25f, 2.0f, 0.05f, "x");

        addSetting(ScreenTab.HUD, RowType.TOGGLE, "potionHud", "Potion HUD",
                "Компактный список наложенных эффектов зелий",
                () -> visualConfig.potionHudEnabled, v -> visualConfig.potionHudEnabled = v);

        addSliderSetting(ScreenTab.HUD, "potionHudScale", "Масштаб Potion HUD",
                "Размер списка активных зелий (от 0.25x до 2.0x)",
                () -> visualConfig.potionHudScale, s -> visualConfig.potionHudScale = s,
                0.25f, 2.0f, 0.05f, "x");

        // 5. Вкладка THEME
        addCategoryHeader(ScreenTab.THEME, "Цветовая схема HUD");
        addSegmentedSetting(ScreenTab.THEME, "themePreset", "Пресет оформления",
                "Быстрое переключение цветовой палитры клиента",
                new String[]{"Eclipse", "Violet", "Cyan", "Emerald", "Crimson"},
                () -> visualConfig.themePreset,
                idx -> {
                    visualConfig.applyThemePreset(idx);
                    showToast("Тема оформления обновлена!", visualConfig.themePrimaryColor);
                });

        addSetting(ScreenTab.THEME, RowType.COLOR, "themePrimary", "Основной акцентный цвет",
                "Главный цвет акцентов HUD-виджетов",
                () -> visualConfig.themePrimaryColor, c -> visualConfig.themePrimaryColor = c,
                () -> false, r -> {});

        addSetting(ScreenTab.THEME, RowType.COLOR, "themeGlow", "Цвет свечения HUD",
                "Оттенок мягкого ореола вокруг HUD панелей",
                () -> visualConfig.themeGlowColor, c -> visualConfig.themeGlowColor = c,
                () -> false, r -> {});

        addSetting(ScreenTab.THEME, RowType.TOGGLE, "checkUpdates", "Проверка обновлений",
                "Асинхронная проверка новых версий на GitHub",
                () -> visualConfig.checkUpdates, v -> visualConfig.checkUpdates = v);

        addSetting(ScreenTab.THEME, RowType.TOGGLE, "betaChannel", "Бета-канал обновлений",
                "Уведомлять о бета-сборках из блока beta в version.json",
                () -> visualConfig.betaChannel, v -> visualConfig.betaChannel = v);

        repositionSettingRows();
    }

    private void addCategoryHeader(ScreenTab tab, String title) {
        SettingRow r = new SettingRow();
        r.tab = tab;
        r.type = RowType.HEADER;
        r.title = title;
        settingRows.add(r);
    }

    private void addSetting(ScreenTab tab, RowType type, String key, String title, String desc,
                            Supplier<Boolean> getter, Consumer<Boolean> setter) {
        SettingRow r = new SettingRow();
        r.tab = tab;
        r.type = type;
        r.key = key;
        r.title = title;
        r.description = desc;
        r.toggleGetter = getter;
        r.toggleSetter = setter;
        settingRows.add(r);
    }

    private void addSetting(ScreenTab tab, RowType type, String key, String title, String desc,
                            Supplier<Integer> colGetter, Consumer<Integer> colSetter,
                            Supplier<Boolean> rainGetter, Consumer<Boolean> rainSetter) {
        SettingRow r = new SettingRow();
        r.tab = tab;
        r.type = type;
        r.key = key;
        r.title = title;
        r.description = desc;
        r.colorGetter = colGetter;
        r.colorSetter = colSetter;
        r.rainbowGetter = rainGetter;
        r.rainbowSetter = rainSetter;
        settingRows.add(r);
    }

    private void addSliderSetting(ScreenTab tab, String key, String title, String desc,
                                  Supplier<Float> getter, Consumer<Float> setter,
                                  float min, float max, float step, String suffix) {
        SettingRow r = new SettingRow();
        r.tab = tab;
        r.type = RowType.SLIDER;
        r.key = key;
        r.title = title;
        r.description = desc;
        r.sliderGetter = getter;
        r.sliderSetter = setter;
        r.sliderMin = min;
        r.sliderMax = max;
        r.sliderStep = step;
        r.sliderSuffix = suffix;
        settingRows.add(r);
    }

    private void addSegmentedSetting(ScreenTab tab, String key, String title, String desc,
                                     String[] options, Supplier<Integer> getter, Consumer<Integer> setter) {
        SettingRow r = new SettingRow();
        r.tab = tab;
        r.type = RowType.SEGMENTED;
        r.key = key;
        r.title = title;
        r.description = desc;
        r.options = options;
        r.optionGetter = getter;
        r.optionSetter = setter;
        settingRows.add(r);
    }

    private void addButtonSetting(ScreenTab tab, String key, String title, String desc,
                                  String btnText, Runnable action) {
        SettingRow r = new SettingRow();
        r.tab = tab;
        r.type = RowType.BUTTON;
        r.key = key;
        r.title = title;
        r.description = desc;
        r.buttonText = btnText;
        r.buttonAction = action;
        settingRows.add(r);
    }

    private void repositionSettingRows() {
        int listWidth = contentRight - contentLeft - 14;
        int rowH = 44;
        int headerH = 26;
        int startY = contentTop + 6 - (int) getScroll();

        int currentY = startY;
        if (isSearching()) {
            currentY += 28;
        }

        for (SettingRow r : settingRows) {
            if (!matchesSearch(r)) continue;
            if (!isSearching() && r.tab != currentTab) continue;

            r.x = contentLeft + 6;
            r.y = currentY;
            r.width = listWidth;
            if (r.type == RowType.HEADER) {
                r.height = headerH;
                currentY += headerH + 6;
            } else {
                r.height = rowH;
                currentY += rowH + 8;
            }
        }
    }

    private boolean matchesSearch(SettingRow r) {
        if (!isSearching()) return true;
        if (r.type == RowType.HEADER) return false;
        String q = searchQuery.toLowerCase().trim();
        if (r.title != null && r.title.toLowerCase().contains(q)) return true;
        if (r.description != null && r.description.toLowerCase().contains(q)) return true;
        if (r.tab != null && r.tab.title.toLowerCase().contains(q)) return true;
        if (r.key != null && r.key.toLowerCase().contains(q)) return true;
        return false;
    }

    // =========================================================================
    //  ОТРИСОВКА ЭКРАНА (Render Loop)
    // =========================================================================
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (context == null) return;
        try {
            float openProgress = getOpenProgress();
            if (openProgress <= 0.001f && isClosing) {
                this.close();
                return;
            }

            long now = System.currentTimeMillis();
            this.screenDeltaSec = lastScreenRenderTime > 0 ? (now - lastScreenRenderTime) / 1000.0f : 0.016f;
            this.screenDeltaSec = MathHelper.clamp(this.screenDeltaSec, 0.0f, 0.1f);
            this.lastScreenRenderTime = now;
            this.animationTime += this.screenDeltaSec;

            // Затемнение фона игры
            int bgAlpha = (int) (180 * openProgress);
            context.fill(0, 0, this.width, this.height, (bgAlpha << 24));

            // Обработка кликов мыши на уровне железа (гарантия 100% отклика в любых средах и лоадерах)
            handleHardwareMouse(mouseX, mouseY);

            // 1. Отрисовка главного окна (Dark Obsidian Glassmorphism)
            try {
                renderMainDialog(context, openProgress);
            } catch (Throwable ignored) {}

            // 2. Сайдбар
            try {
                renderSidebar(context, openProgress);
            } catch (Throwable ignored) {}

            // 3. Шапка (Header)
            try {
                renderHeader(context, openProgress);
            } catch (Throwable ignored) {}

            // 4. Контент выбранной вкладки с Scissor Clipping
            try {
                renderTabContent(context, mouseX, mouseY, openProgress);
            } catch (Throwable ignored) {}

            // 5. Стандартные дочерние виджеты (кнопки вкладок, поля ввода)
            try {
                super.render(context, mouseX, mouseY, delta);
            } catch (Throwable ignored) {}

            // 6. Скроллбар
            try {
                renderScrollbar(context, openProgress);
            } catch (Throwable ignored) {}

            // 7. Модальная палитра выбора цвета (поверх всех слоев)
            if (colorPicker != null && colorPicker.isVisible()) {
                try {
                    colorPicker.render(context, this.client, mouseX, mouseY);
                } catch (Throwable ignored) {}
            }

            // 8. Всплывающее уведомление (Toast)
            try {
                renderToast(context);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private void renderMainDialog(DrawContext context, float alpha) {
        // Мягкое космическое свечение вокруг окна
        RenderUtils.drawSoftGlow(context, dialogLeft, dialogTop, dialogWidth, dialogHeight, 12, 4, RenderUtils.multiplyAlpha(0x2A6A67CE, alpha));

        // 1. Основное тело окна: глубокий темный #0B0B10 (alpha 95%)
        int mainBg = RenderUtils.multiplyAlpha(0xF20B0B10, alpha);
        RenderUtils.drawRoundedRect(context, dialogLeft, dialogTop, dialogWidth, dialogHeight, 12, mainBg);

        // 2. Внешняя тонкая обводка окна (1px): сдержанная темно-графитовая #232533
        int outlineCol = RenderUtils.multiplyAlpha(0xFF232533, alpha);
        RenderUtils.drawRoundedOutline(context, dialogLeft, dialogTop, dialogWidth, dialogHeight, 12, outlineCol);

        // Мягкий градиентный перелив на верхнем и нижнем ребре от золотистого #F5A65B к индиго #6A67CE
        int gold = RenderUtils.multiplyAlpha(0xFFF5A65B, alpha * 0.9f);
        int indigo = RenderUtils.multiplyAlpha(0xFF6A67CE, alpha * 0.9f);
        RenderUtils.drawGradientLineH(context, dialogLeft + 12, dialogTop, dialogWidth - 24, 1, gold, indigo);
        RenderUtils.drawGradientLineH(context, dialogLeft + 12, dialogTop + dialogHeight - 1, dialogWidth - 24, 1, gold, indigo);
    }

    private void renderSidebar(DrawContext context, float alpha) {
        // Подложка сайдбара
        int sidebarBg = RenderUtils.multiplyAlpha(0x1E0B0B10, alpha);
        RenderUtils.drawRoundedRect(context, dialogLeft + 2, dialogTop + 2, sidebarWidth - 4, dialogHeight - 4, 10, sidebarBg);

        // Разделительная вертикальная линия
        int dividerCol = RenderUtils.multiplyAlpha(0xFF232533, alpha);
        context.fill(dialogLeft + sidebarWidth, dialogTop + 40, dialogLeft + sidebarWidth + 1, dialogTop + dialogHeight - 12, dividerCol);

        // Анимированный плавающий индикатор активной вкладки
        int targetY = dialogTop + 48;
        for (int i = 0; i < ScreenTab.values().length; i++) {
            if (ScreenTab.values()[i] == currentTab) {
                targetY = dialogTop + 48 + i * 33;
                break;
            }
        }
        animatedTabY = MathHelper.lerp(0.2f, animatedTabY, (float) targetY);

        // Вертикальный золотисто-фиолетовый индикатор слева (2px, градиент #F5A65B -> #6A67CE)
        int indicatorX = dialogLeft + 7;
        int indicatorW = 2;
        int indicatorH = 22;
        int gold = RenderUtils.multiplyAlpha(0xFFF5A65B, alpha);
        int indigo = RenderUtils.multiplyAlpha(0xFF6A67CE, alpha);
        EclipseTheme.drawVerticalGradient(context, indicatorX, (int) animatedTabY + 3, indicatorW, indicatorH, gold, indigo);
        RenderUtils.drawSoftGlow(context, indicatorX, (int) animatedTabY + 3, indicatorW, indicatorH, 2, 2, RenderUtils.multiplyAlpha(0x556A67CE, alpha));
    }

    private void renderHeader(DrawContext context, float alpha) {
        try {
            // 0. Плашка шапки в глубоком стиле #0D0E15
            int headerPlateBg = RenderUtils.multiplyAlpha(0xFF0D0E15, alpha);
            RenderUtils.drawRoundedRect(context, dialogLeft + 2, dialogTop + 2, dialogWidth - 4, 36, 10, headerPlateBg);

            // 1. Компактный значок затмения (Badge 22x22) с анимацией дыхания и вращения
            int badgeSize = 22;
            int badgeX = dialogLeft + 12;
            int badgeY = dialogTop + 8;
            EclipseTheme.drawEclipseBadge(context, badgeX, badgeY, badgeSize, alpha, this.animationTime);

            // 2. Название мода: "ECLIPSE" (#EAECEF) и "VISUAL MOD" (#7D8296) с отступом 6px
            int textX = badgeX + badgeSize + 8;
            MutableText logoText = Text.literal("ECLIPSE").formatted(Formatting.BOLD);
            RenderUtils.drawTextWithShadow(context, this.textRenderer, logoText, textX, dialogTop + 10, RenderUtils.multiplyAlpha(0xFFEAECEF, alpha));

            int eclipseWidth = this.textRenderer != null ? this.textRenderer.getWidth(logoText) : 48;
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal("VISUAL MOD"),
                    textX + eclipseWidth + 6, dialogTop + 10, RenderUtils.multiplyAlpha(0xFF7D8296, alpha));

            // 3. Фирменная подпись: "by " (#7D8296) + мягкий перелив "alone.vendetta" (#F5A65B -> #6A67CE)
            int subY = dialogTop + 22;
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal("by "), textX, subY, RenderUtils.multiplyAlpha(0xFF7D8296, alpha));
            int byWidth = this.textRenderer != null ? this.textRenderer.getWidth("by ") : 14;
            RenderUtils.drawGradientText(context, this.textRenderer, "alone.vendetta", textX + byWidth, subY, 0xFFF5A65B, 0xFF6A67CE, true, alpha);

            // Тонкая градиентная линия под шапкой от золотистого к фиолетовому (#F5A65B -> #6A67CE)
            RenderUtils.drawGradientLineH(context, dialogLeft + 10, dialogTop + 38, dialogWidth - 20, 1,
                    RenderUtils.multiplyAlpha(0xFFF5A65B, alpha * 0.85f),
                    RenderUtils.multiplyAlpha(0xFF6A67CE, alpha * 0.85f));

            // Индикатор FPS: мягкая зеленая точка #6BCB77, текст светло-серый #A0A5B8
            int fps = this.client != null ? this.client.getCurrentFps() : 60;
            int metricsX = dialogLeft + sidebarWidth + 205;
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal("• "),
                    metricsX, dialogTop + 16, RenderUtils.multiplyAlpha(0xFF6BCB77, alpha));
            int dotW = this.textRenderer != null ? this.textRenderer.getWidth("• ") : 6;
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(fps + " FPS"),
                    metricsX + dotW, dialogTop + 16, RenderUtils.multiplyAlpha(0xFFA0A5B8, alpha));
        } catch (Throwable t) {
            System.err.println("[ClickGUI] Error in renderHeader: " + t);
        }
    }

    private void renderTabContent(DrawContext context, int mouseX, int mouseY, float alpha) {
        // Scissor Clipping для отсечения выходящих за границы элементов
        context.enableScissor(contentLeft, contentTop, contentRight, contentBottom);

        if (isSearching()) {
            renderSearchTab(context, mouseX, mouseY, alpha);
        } else if (currentTab == ScreenTab.PREFIXES) {
            renderPrefixTab(context, mouseX, mouseY, alpha);
        } else {
            renderSettingsTab(context, mouseX, mouseY, alpha);
        }

        context.disableScissor();
    }

    private void renderPrefixTab(DrawContext context, int mouseX, int mouseY, float alpha) {
        repositionPrefixCards();
        int listWidth = contentRight - contentLeft - 14;
        int cardH = 52;
        int startY = contentTop + 4 - (int) getScroll();

        int activeIdx = DonorPrefixClientMod.getInstance() != null ? DonorPrefixClientMod.getInstance().getCurrentPrefixIndex() : 0;
        int secsUntil = DonorPrefixClientMod.getInstance() != null ? DonorPrefixClientMod.getInstance().getSecondsUntilNext() : -1;

        for (int i = 0; i < prefixCards.size(); i++) {
            PrefixCard card = prefixCards.get(i);
            int cardY = startY + i * (cardH + 6);

            // Видимость карточки в viewport
            if (cardY + cardH < contentTop || cardY > contentBottom) {
                setCardWidgetsVisible(card, false);
                continue;
            }
            setCardWidgetsVisible(card, true);

            boolean isActive = (i == activeIdx && config.enabled);
            int cardBg = RenderUtils.multiplyAlpha(0xFF101118, alpha);
            int cardBorder = isActive ? RenderUtils.multiplyAlpha(0xFF6A67CE, alpha) : RenderUtils.multiplyAlpha(0xFF1C1E2B, alpha);

            // Карточка префикса
            RenderUtils.drawCyberCard(context, contentLeft + 6, cardY, listWidth, cardH, 6, cardBg, cardBorder,
                    isActive ? RenderUtils.multiplyAlpha(0x336A67CE, alpha) : 0);

            // Номер позиции в очереди
            String numStr = "#" + (i + 1);
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(numStr).formatted(Formatting.DARK_GRAY),
                    contentLeft + 12, cardY + 7, RenderUtils.multiplyAlpha(0xFF64748B, alpha));

            // Статус карточки (АКТИВЕН / СЛЕДУЮЩИЙ)
            if (isActive) {
                String actStr = "● АКТИВЕН [Смена: " + (secsUntil > 0 ? secsUntil + "с" : "...") + "]";
                RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(actStr).formatted(Formatting.LIGHT_PURPLE),
                        contentLeft + 36, cardY + 7, RenderUtils.multiplyAlpha(0xFFC084FC, alpha));
            } else if (i == (activeIdx + 1) % Math.max(1, config.prefixes.size()) && config.enabled) {
                RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal("○ Следующий в очереди").formatted(Formatting.GRAY),
                        contentLeft + 36, cardY + 7, RenderUtils.multiplyAlpha(0xFF94A3B8, alpha));
            }

            // Живой предпросмотр префикса (кэшируется по тексту)
            String curText = card.field != null ? card.field.getText() : card.initialText;
            if (!curText.equals(card.lastPreviewRaw) || card.cachedPreview == null) {
                card.lastPreviewRaw = curText;
                card.cachedPreview = ColorFormatter.format(curText, true);
            }
            int prevX = contentLeft + 195;
            int prevY = cardY + 7;
            RenderUtils.drawTextWithShadow(context, this.textRenderer, card.cachedPreview, prevX, prevY, RenderUtils.multiplyAlpha(0xFFFFFFFF, alpha));
        }

        // Блок "Параметры ротации"
        int addY = startY + prefixCards.size() * (cardH + 6) + 4;
        int settingsY = addY + 34;

        if (settingsY + 100 >= contentTop && settingsY <= contentBottom) {
            RenderUtils.drawCyberCard(context, contentLeft + 6, settingsY, listWidth, 106, 6,
                    RenderUtils.multiplyAlpha(0xFF101118, alpha), RenderUtils.multiplyAlpha(0xFF1C1E2B, alpha), 0);

            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal("⚙ Параметры ротации").formatted(Formatting.BOLD),
                    contentLeft + 14, settingsY + 7, RenderUtils.multiplyAlpha(0xFFEAECEF, alpha));

            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal("Кулдаун:"),
                    contentLeft + 14, settingsY + 25, RenderUtils.multiplyAlpha(0xFFA0A5B8, alpha));

            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal("Шаблон:"),
                    contentLeft + 14, settingsY + 50, RenderUtils.multiplyAlpha(0xFFA0A5B8, alpha));
        }
    }

    private void setCardWidgetsVisible(PrefixCard card, boolean visible) {
        if (card.field != null) card.field.setVisible(visible);
        if (card.upButton != null) card.upButton.visible = visible;
        if (card.downButton != null) card.downButton.visible = visible;
        if (card.testButton != null) card.testButton.visible = visible;
        if (card.deleteButton != null) card.deleteButton.visible = visible;
    }

    private void renderSettingsTab(DrawContext context, int mouseX, int mouseY, float alpha) {
        repositionSettingRows();
        VisualConfig cfg = VisualConfig.getInstance();

        for (SettingRow r : settingRows) {
            if (r.tab != currentTab) continue;

            // Проверка видимости
            if (r.y + r.height < contentTop || r.y > contentBottom) continue;

            // 1. Отрисовка заголовка категории
            if (r.type == RowType.HEADER) {
                int dotColor = RenderUtils.multiplyAlpha(cfg.themePrimaryColor, alpha);
                RenderUtils.drawRoundedRect(context, r.x + 2, r.y + 10, 4, 10, 2, dotColor);
                RenderUtils.drawSoftGlow(context, r.x + 2, r.y + 10, 4, 10, 4, 1, RenderUtils.multiplyAlpha(cfg.themePrimaryColor, alpha * 0.6f));

                String titleText = r.title.toUpperCase();
                RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(titleText).formatted(Formatting.BOLD),
                        r.x + 12, r.y + 11, RenderUtils.multiplyAlpha(cfg.themePrimaryColor, alpha));

                int titleW = this.textRenderer.getWidth(titleText);
                int lineStart = r.x + 18 + titleW;
                int lineEnd = r.x + r.width - 8;
                if (lineEnd > lineStart) {
                    context.fillGradient(lineStart, r.y + 15, lineEnd, r.y + 16,
                            RenderUtils.multiplyAlpha(cfg.themePrimaryColor, alpha * 0.35f),
                            RenderUtils.multiplyAlpha(0x00000000, 0f));
                }
                continue;
            }

            // 2. Отрисовка интерактивной карточки
            boolean hovered = mouseX >= r.x && mouseX <= r.x + r.width && mouseY >= r.y && mouseY <= r.y + r.height;

            int rowBg = hovered ? RenderUtils.multiplyAlpha(0x331C2030, alpha) : RenderUtils.multiplyAlpha(0xFF101118, alpha);
            int rowBorder = hovered ? RenderUtils.multiplyAlpha(0xFF6A67CE, alpha * 0.85f) : RenderUtils.multiplyAlpha(0xFF1C1E2B, alpha);

            // Карточка опции с мягким космическим свечением при наведении
            if (hovered) {
                RenderUtils.drawSoftGlow(context, r.x, r.y, r.width, r.height, 6, 2, RenderUtils.multiplyAlpha(0x336A67CE, alpha));
            }
            RenderUtils.drawCyberCard(context, r.x, r.y, r.width, r.height, 6, rowBg, rowBorder, 0);

            // Название и описание
            int textY = r.y + (r.description != null ? 8 : 17);
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(r.title).formatted(Formatting.BOLD),
                    r.x + 12, textY, RenderUtils.multiplyAlpha(0xFFEAECEF, alpha));

            if (r.description != null) {
                RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(r.description).formatted(Formatting.GRAY),
                        r.x + 12, textY + 13, RenderUtils.multiplyAlpha(0xFF7D8296, alpha));
            }

            // Правая интерактивная часть
            int ctrlY = r.y + (r.height - 18) / 2;
            renderSettingControl(context, r, ctrlY, alpha);
        }
    }

    private void renderSearchTab(DrawContext context, int mouseX, int mouseY, float alpha) {
        repositionSettingRows();
        hidePrefixTabWidgets();

        int matchCount = 0;
        for (SettingRow r : settingRows) {
            if (matchesSearch(r)) matchCount++;
        }

        int startY = contentTop + 6 - (int) getScroll();

        // 1. Шапка результатов поиска
        if (startY + 20 >= contentTop && startY <= contentBottom) {
            String queryDisplay = searchQuery.length() > 22 ? searchQuery.substring(0, 20) + "..." : searchQuery;
            String headerTitle = "🔍 РЕЗУЛЬТАТЫ ПОИСКА: \"" + queryDisplay + "\"";
            RenderUtils.drawGradientText(context, this.textRenderer, headerTitle, contentLeft + 8, startY + 2,
                    0xFFF5A65B, 0xFF6A67CE, false, alpha);

            String countText = "Найдено модулей: " + matchCount;
            int countW = this.textRenderer.getWidth(countText);
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(countText),
                    contentRight - countW - 14, startY + 2, RenderUtils.multiplyAlpha(0xFF7D8296, alpha));

            RenderUtils.drawGradientLineH(context, contentLeft + 6, startY + 18, contentRight - contentLeft - 14, 1,
                    RenderUtils.multiplyAlpha(0x55F5A65B, alpha), RenderUtils.multiplyAlpha(0x556A67CE, alpha));
        }

        // 2. Если ничего не найдено
        if (matchCount == 0) {
            int emptyY = contentTop + (contentBottom - contentTop) / 2 - 20;
            String noMatch = "Модули по запросу \"" + searchQuery + "\" не найдены";
            String tip = "Попробуйте: pvp, туман, hud, fps, cps, щит, урон, glint, броня";
            RenderUtils.drawCenteredTextWithShadow(context, this.textRenderer, Text.literal(noMatch),
                    contentLeft + (contentRight - contentLeft) / 2, emptyY, RenderUtils.multiplyAlpha(0xFFF5A65B, alpha));
            RenderUtils.drawCenteredTextWithShadow(context, this.textRenderer, Text.literal(tip),
                    contentLeft + (contentRight - contentLeft) / 2, emptyY + 16, RenderUtils.multiplyAlpha(0xFF7D8296, alpha));
            return;
        }

        // 3. Найденные карточки с тегом категории
        for (SettingRow r : settingRows) {
            if (!matchesSearch(r)) continue;
            if (r.y + r.height < contentTop || r.y > contentBottom) continue;

            boolean hovered = mouseX >= r.x && mouseX <= r.x + r.width && mouseY >= r.y && mouseY <= r.y + r.height;

            int rowBg = hovered ? RenderUtils.multiplyAlpha(0x331C2030, alpha) : RenderUtils.multiplyAlpha(0xFF101118, alpha);
            int rowBorder = hovered ? RenderUtils.multiplyAlpha(0xFF6A67CE, alpha * 0.85f) : RenderUtils.multiplyAlpha(0xFF1C1E2B, alpha);

            if (hovered) {
                RenderUtils.drawSoftGlow(context, r.x, r.y, r.width, r.height, 6, 2, RenderUtils.multiplyAlpha(0x336A67CE, alpha));
            }
            RenderUtils.drawCyberCard(context, r.x, r.y, r.width, r.height, 6, rowBg, rowBorder, 0);

            // Бейдж вкладки (например: [БОЙ & PVP])
            String tabTag = "[" + r.tab.title.toUpperCase() + "]";
            int tagW = this.textRenderer.getWidth(tabTag);
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(tabTag),
                    r.x + 12, r.y + 8, RenderUtils.multiplyAlpha(0xFF6A67CE, alpha));

            // Заголовок
            RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(r.title).formatted(Formatting.BOLD),
                    r.x + 18 + tagW, r.y + 8, RenderUtils.multiplyAlpha(0xFFEAECEF, alpha));

            // Описание
            if (r.description != null && !r.description.isEmpty()) {
                RenderUtils.drawTextWithShadow(context, this.textRenderer, Text.literal(r.description).formatted(Formatting.GRAY),
                        r.x + 12, r.y + 23, RenderUtils.multiplyAlpha(0xFF7D8296, alpha));
            }

            // Правая интерактивная часть
            int ctrlY = r.y + (r.height - 18) / 2;
            renderSettingControl(context, r, ctrlY, alpha);
        }
    }

    private void renderSettingControl(DrawContext context, SettingRow r, int ctrlY, float alpha) {
        int ctrlX = r.x + r.width - 44;

        switch (r.type) {
            case TOGGLE -> {
                boolean act = r.toggleGetter != null && r.toggleGetter.get();
                float prog = toggleAnimMap.getOrDefault(r.key, act ? 1f : 0f);
                prog = AnimationUtil.deltaLerp(prog, act ? 1f : 0f, 14.0f, screenDeltaSec);
                toggleAnimMap.put(r.key, prog);

                RenderUtils.drawToggleSwitch(context, ctrlX, ctrlY, 34, 18, act, prog);
            }
            case SLIDER -> {
                float val = r.sliderGetter != null ? r.sliderGetter.get() : 0f;
                float targetNorm = (val - r.sliderMin) / (r.sliderMax - r.sliderMin);
                float smoothNorm = sliderAnimMap.getOrDefault(r.key, targetNorm);
                smoothNorm = AnimationUtil.deltaLerp(smoothNorm, targetNorm, 16.0f, screenDeltaSec);
                sliderAnimMap.put(r.key, smoothNorm);

                int sliderW = 110;
                int sliderX = r.x + r.width - sliderW - 54;
                int sliderY = r.y + (r.height - 14) / 2;

                RenderUtils.drawSlider(context, sliderX, sliderY, sliderW, 14, smoothNorm,
                        EclipseTheme.TRACK_BG,
                        EclipseTheme.ACCENT_GOLD,
                        EclipseTheme.ACCENT_INDIGO,
                        EclipseTheme.ACCENT_GOLD);

                if (Float.compare(r.lastSliderVal, val) != 0 || r.cachedSliderText == null) {
                    r.lastSliderVal = val;
                    int scaled = Math.round(val * 100.0f);
                    int abs = Math.abs(scaled);
                    String str = (scaled < 0 ? "-" : "") + (abs / 100) + "."
                            + ((abs % 100) < 10 ? "0" : "") + (abs % 100)
                            + (r.sliderSuffix != null ? r.sliderSuffix : "");
                    r.cachedSliderText = Text.literal(str);
                }
                RenderUtils.drawTextWithShadow(context, this.textRenderer, r.cachedSliderText,
                        r.x + r.width - 48, r.y + (r.height - 8) / 2, RenderUtils.multiplyAlpha(0xFFEAECEF, alpha));
            }
            case COLOR -> {
                int col = r.colorGetter != null ? r.colorGetter.get() : 0xFFFFFFFF;
                boolean rain = r.rainbowGetter != null && r.rainbowGetter.get();
                int swatchW = 38;
                int swatchH = 18;
                int swatchX = r.x + r.width - swatchW - 10;

                if (rain) {
                    RenderUtils.drawHueBar(context, swatchX, ctrlY, swatchW, swatchH);
                } else {
                    RenderUtils.drawRoundedRect(context, swatchX, ctrlY, swatchW, swatchH, 4, col | 0xFF000000);
                    RenderUtils.drawRoundedOutline(context, swatchX, ctrlY, swatchW, swatchH, 4, 0x55FFFFFF);
                }
            }
            case SEGMENTED -> {
                if (r.options != null && r.options.length > 0) {
                    int sel = r.optionGetter != null ? r.optionGetter.get() : 0;
                    int segW = 38;
                    int totalW = r.options.length * (segW + 3);
                    int startSegX = r.x + r.width - totalW - 10;

                    for (int s = 0; s < r.options.length; s++) {
                        int sx = startSegX + s * (segW + 3);
                        boolean isSel = (s == sel);
                        int sBg = isSel ? RenderUtils.multiplyAlpha(0xFF161824, alpha) : RenderUtils.multiplyAlpha(0x22101118, alpha);
                        int sBorder = isSel ? RenderUtils.multiplyAlpha(0xFF6A67CE, alpha) : RenderUtils.multiplyAlpha(0xFF232533, alpha);

                        RenderUtils.drawPill(context, sx, ctrlY, segW, 18, sBg, sBorder);
                        RenderUtils.drawCenteredTextWithShadow(context, this.textRenderer, Text.literal(r.options[s]),
                                 sx + segW / 2, ctrlY + 5, isSel ? 0xFFFFFFFF : 0xFF7D8296);
                    }
                }
            }
            case BUTTON -> {
                int btnW = 68;
                int btnX = r.x + r.width - btnW - 10;
                RenderUtils.drawPill(context, btnX, ctrlY, btnW, 18,
                        RenderUtils.multiplyAlpha(0xFF161824, alpha), RenderUtils.multiplyAlpha(0xFF6A67CE, alpha));
                RenderUtils.drawCenteredTextWithShadow(context, this.textRenderer, Text.literal(r.buttonText != null ? r.buttonText : "ОТКРЫТЬ").formatted(Formatting.BOLD),
                        btnX + btnW / 2, ctrlY + 5, 0xFFEAECEF);
            }
            default -> {}
        }
    }

    private void renderScrollbar(DrawContext context, float alpha) {
        if (!hasScrollbar()) return;

        double maxScroll = getMaxScroll();
        int trackH = contentBottom - contentTop;
        int trackX = contentRight - 4;
        int trackY = contentTop;

        // Дорожка скролла
        RenderUtils.drawRoundedRect(context, trackX, trackY, 4, trackH, 2, RenderUtils.multiplyAlpha(0xFF101118, alpha));

        // Бегунок
        int thumbH = Math.max(22, (int) ((double) trackH / (trackH + maxScroll) * trackH));
        int thumbY = trackY + (int) ((getScroll() / maxScroll) * (trackH - thumbH));

        RenderUtils.drawRoundedRect(context, trackX, thumbY, 4, thumbH, 2, RenderUtils.multiplyAlpha(0x886A67CE, alpha));
    }

    private void renderToast(DrawContext context) {
        if (toastText == null) return;
        long remaining = toastExpireTime - System.currentTimeMillis();
        if (remaining <= 0) {
            toastText = null;
            return;
        }

        long elapsed = TOAST_DURATION_MS - remaining;
        float enterProgress = MathHelper.clamp(elapsed / 260.0f, 0.0f, 1.0f);
        float exitProgress = MathHelper.clamp(remaining / 240.0f, 0.0f, 1.0f);
        float toastAlpha = Math.min(enterProgress, exitProgress);

        float easedSlide = AnimationUtil.easeOutBack(enterProgress);
        int toastW = this.textRenderer.getWidth(toastText) + 32;
        int toastH = 22;
        int toastX = (this.width - toastW) / 2;
        int targetY = dialogTop + dialogHeight - toastH - 12;
        int startToastY = targetY + 14;
        int toastY = Math.round(startToastY + (targetY - startToastY) * easedSlide);

        RenderUtils.drawSoftGlow(context, toastX, toastY, toastW, toastH, 6, 4, RenderUtils.multiplyAlpha(toastColor, toastAlpha * 0.45f));
        RenderUtils.drawRoundedRect(context, toastX, toastY, toastW, toastH, 6, RenderUtils.multiplyAlpha(0xF00D0E15, toastAlpha));
        RenderUtils.drawRoundedOutline(context, toastX, toastY, toastW, toastH, 6, RenderUtils.multiplyAlpha(toastColor, toastAlpha * 0.85f));

        RenderUtils.drawCenteredTextWithShadow(context, this.textRenderer, toastText, toastX + toastW / 2, toastY + 7,
                RenderUtils.multiplyAlpha(0xFFFFFFFF, toastAlpha));
    }

    public void showToast(String message, int color) {
        this.toastText = Text.literal(message);
        this.toastColor = color;
        this.toastExpireTime = System.currentTimeMillis() + TOAST_DURATION_MS;
    }

    // =========================================================================
    //  ИНПУТЫ И МЫШЬ (Mouse & Keyboard Handling)
    // =========================================================================
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        try {
            if (colorPicker.isVisible()) {
                return true;
            }
            if (hasScrollbar()) {
                setScroll(getScroll() - verticalAmount * 24.0);
                if (isSearching()) {
                    repositionSettingRows();
                } else if (currentTab == ScreenTab.PREFIXES) {
                    repositionPrefixCards();
                } else {
                    repositionSettingRows();
                }
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isMouseOver(net.minecraft.client.gui.widget.ClickableWidget widget, double mouseX, double mouseY) {
        if (widget == null || !widget.visible) return false;
        return mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth()
                && mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight();
    }

    private boolean wasMouseDown = false;
    private long lastClickTime = 0L;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    private void handleHardwareMouse(int mouseX, int mouseY) {
        if (this.client == null || this.client.getWindow() == null) return;
        long window = this.client.getWindow().getHandle();
        if (window == 0) return;
        boolean isDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;

        Click click = new Click(mouseX, mouseY, new MouseInput(0, 0));
        if (isDown && !wasMouseDown) {
            long now = System.currentTimeMillis();
            if (now - lastClickTime > 120) {
                lastClickTime = now;
                dispatchClick(click, false, mouseX, mouseY, 0);
            }
        } else if (!isDown && wasMouseDown) {
            mouseReleased(click);
        } else if (isDown && wasMouseDown) {
            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;
            if (Math.abs(dx) > 0.01 || Math.abs(dy) > 0.01) {
                mouseDragged(click, dx, dy);
            }
        }

        wasMouseDown = isDown;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        lastClickTime = System.currentTimeMillis();
        return dispatchClick(click, bl, click.x(), click.y(), click.button());
    }

    private boolean dispatchClick(Click click, boolean bl, double mouseX, double mouseY, int button) {
        try {
            // 1. Модальная палитра выбора цвета (если открыта поверх всех слоев)
            if (colorPicker != null && colorPicker.isVisible()) {
                if (colorPicker.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                colorPicker.close();
                return true;
            }

            // 2. Кнопка закрытия [✕] в правом верхнем углу шапки
            if (closeButton != null && closeButton.visible && isMouseOver(closeButton, mouseX, mouseY)) {
                closeButton.onPress();
                return true;
            }

            // 3. Кнопка мастера ротации в шапке (• НА ПАУЗЕ / • РОТАЦИЯ: ВКЛ)
            if (masterStatusButton != null && masterStatusButton.visible && isMouseOver(masterStatusButton, mouseX, mouseY)) {
                masterStatusButton.onPress();
                return true;
            }

            // 4. БОКОВЫЕ ВКЛАДКИ (Sidebar Tabs) — проверяем в самом начале (сразу после шапки),
            // чтобы ни один виджет, поле ввода или модуль из правой части окна не мог заблокировать переключение!
            int tabX = dialogLeft + 8;
            int tabW = sidebarWidth - 16;
            int tabH = 28;
            ScreenTab[] allTabs = ScreenTab.values();
            for (int i = 0; i < allTabs.length; i++) {
                int tabY = dialogTop + 48 + i * 33;
                if (mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
                    ScreenTab clickedTab = allTabs[i];
                    if (i < sidebarTabButtons.size() && sidebarTabButtons.get(i) != null) {
                        try {
                            sidebarTabButtons.get(i).playDownSound(MinecraftClient.getInstance().getSoundManager());
                        } catch (Throwable ignored) {}
                    }
                    switchTab(clickedTab);
                    return true;
                }
            }

            // 5. Кнопка "✦ HUD Редактор" внизу сайдбара
            int hudBtnX = dialogLeft + 8;
            int hudBtnW = sidebarWidth - 16;
            int hudBtnH = 24;
            int hudBtnY = dialogTop + dialogHeight - 34;
            if (mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH) {
                if (hudEditBtn != null) {
                    hudEditBtn.onPress();
                } else if (this.client != null) {
                    this.client.setScreen(new HudEditorScreen(this));
                }
                return true;
            }

            // 6. Поле живого поиска в шапке
            if (searchField != null) {
                boolean overSearch = isMouseOver(searchField, mouseX, mouseY);
                searchField.setFocused(overSearch);
                if (overSearch) {
                    return searchField.mouseClicked(click, bl);
                }
            }

            // 7. Скроллбар
            if (hasScrollbar()) {
                int trackX = contentRight - 4;
                if (mouseX >= trackX - 6 && mouseX <= trackX + 10 && mouseY >= contentTop && mouseY <= contentBottom) {
                    isDraggingScrollbar = true;
                    dragStartY = mouseY;
                    dragStartScroll = getScroll();
                    return true;
                }
            }

            // 8. Виджеты вкладки "Префиксы" (при активной вкладке PREFIXES и без поиска)
            if (!isSearching() && currentTab == ScreenTab.PREFIXES) {
                if (mouseY >= contentTop && mouseY <= contentBottom && mouseX >= contentLeft && mouseX <= contentRight) {
                    for (PrefixCard card : prefixCards) {
                        if (card.field != null && isMouseOver(card.field, mouseX, mouseY)) {
                            card.field.setFocused(true);
                            return card.field.mouseClicked(click, bl);
                        } else if (card.field != null) {
                            card.field.setFocused(false);
                        }
                        if (card.upButton != null && isMouseOver(card.upButton, mouseX, mouseY)) {
                            card.upButton.onPress();
                            return true;
                        }
                        if (card.downButton != null && isMouseOver(card.downButton, mouseX, mouseY)) {
                            card.downButton.onPress();
                            return true;
                        }
                        if (card.testButton != null && isMouseOver(card.testButton, mouseX, mouseY)) {
                            card.testButton.onPress();
                            return true;
                        }
                        if (card.deleteButton != null && isMouseOver(card.deleteButton, mouseX, mouseY)) {
                            card.deleteButton.onPress();
                            return true;
                        }
                    }

                    if (addPrefixButton != null && isMouseOver(addPrefixButton, mouseX, mouseY)) {
                        addPrefixButton.onPress();
                        return true;
                    }
                    if (cooldownField != null && isMouseOver(cooldownField, mouseX, mouseY)) {
                        cooldownField.setFocused(true);
                        return cooldownField.mouseClicked(click, bl);
                    } else if (cooldownField != null) {
                        cooldownField.setFocused(false);
                    }
                    if (cooldownDecrBtn != null && isMouseOver(cooldownDecrBtn, mouseX, mouseY)) {
                        cooldownDecrBtn.onPress();
                        return true;
                    }
                    if (cooldownIncrBtn != null && isMouseOver(cooldownIncrBtn, mouseX, mouseY)) {
                        cooldownIncrBtn.onPress();
                        return true;
                    }
                    if (templateField != null && isMouseOver(templateField, mouseX, mouseY)) {
                        templateField.setFocused(true);
                        return templateField.mouseClicked(click, bl);
                    } else if (templateField != null) {
                        templateField.setFocused(false);
                    }
                    if (savePrefixesBtn != null && isMouseOver(savePrefixesBtn, mouseX, mouseY)) {
                        savePrefixesBtn.onPress();
                        return true;
                    }
                }
            }

            // 9. Интерактивные строки настроек (в режиме поиска или во вкладках настроек)
            if (isSearching() || currentTab != ScreenTab.PREFIXES) {
                if (mouseY >= contentTop && mouseY <= contentBottom && mouseX >= contentLeft && mouseX <= contentRight) {
                    for (SettingRow r : settingRows) {
                        if (!matchesSearch(r)) continue;
                        if (!isSearching() && r.tab != currentTab) continue;

                        if (mouseX >= r.x && mouseX <= r.x + r.width && mouseY >= r.y && mouseY <= r.y + r.height) {
                            handleSettingRowClick(r, mouseX, mouseY);
                            return true;
                        }
                    }
                }
            }

            // 10. Стандартная диспетчеризация виджетов Minecraft
            return super.mouseClicked(click, bl);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void handleSettingRowClick(SettingRow r, double mouseX, double mouseY) {
        if (r.type == RowType.HEADER) return;
        int ctrlY = r.y + (r.height - 18) / 2;

        switch (r.type) {
            case TOGGLE -> {
                if (r.toggleSetter != null && r.toggleGetter != null) {
                    boolean newVal = !r.toggleGetter.get();
                    r.toggleSetter.accept(newVal);
                    ru.eclipse.client.module.ModuleManager.getInstance().syncConfigsToModules();
                    visualConfig.save();
                    showToast(r.title + ": " + (newVal ? "ВКЛ" : "ВЫКЛ"), newVal ? 0xFF10B981 : 0xFFF59E0B);
                }
            }
            case SLIDER -> {
                int sliderW = 110;
                int sliderX = r.x + r.width - sliderW - 54;
                if (mouseX >= sliderX - 6 && mouseX <= sliderX + sliderW + 6) {
                    activeDraggingSlider = r;
                    updateSliderFromMouse(r, mouseX);
                }
            }
            case COLOR -> {
                int swatchW = 38;
                int swatchX = r.x + r.width - swatchW - 10;
                if (mouseX >= swatchX && mouseX <= swatchX + swatchW) {
                    int curCol = r.colorGetter != null ? r.colorGetter.get() : 0xFFFF0055;
                    boolean rain = r.rainbowGetter != null && r.rainbowGetter.get();
                    colorPicker.open(r.title, curCol, rain, 1.0f, true, (int) mouseX, (int) mouseY,
                            (newCol, newRainbow, newAlpha) -> {
                                if (r.colorSetter != null) r.colorSetter.accept(newCol);
                                if (r.rainbowSetter != null) r.rainbowSetter.accept(newRainbow);
                                ru.eclipse.client.module.ModuleManager.getInstance().syncConfigsToModules();
                                visualConfig.save();
                            });
                }
            }
            case SEGMENTED -> {
                if (r.options != null && r.options.length > 0) {
                    int segW = 38;
                    int totalW = r.options.length * (segW + 3);
                    int startSegX = r.x + r.width - totalW - 10;
                    for (int s = 0; s < r.options.length; s++) {
                        int sx = startSegX + s * (segW + 3);
                        if (mouseX >= sx && mouseX <= sx + segW) {
                            if (r.optionSetter != null) {
                                r.optionSetter.accept(s);
                                ru.eclipse.client.module.ModuleManager.getInstance().syncConfigsToModules();
                                visualConfig.save();
                                showToast(r.title + " → " + r.options[s], 0xFF6A67CE);
                            }
                            break;
                        }
                    }
                }
            }
            case BUTTON -> {
                if (r.buttonAction != null) {
                    r.buttonAction.run();
                }
            }
        }
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        try {
            double mouseX = click.x();
            double mouseY = click.y();
            int button = click.button();
            if (colorPicker != null && colorPicker.isVisible()) {
                if (colorPicker.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
            if (isDraggingScrollbar) {
                double maxScroll = getMaxScroll();
                int trackH = contentBottom - contentTop;
                double scrollDelta = (deltaY / (double) trackH) * maxScroll;
                setScroll(getScroll() + scrollDelta);
                if (isSearching()) {
                    repositionSettingRows();
                } else if (currentTab == ScreenTab.PREFIXES) {
                    repositionPrefixCards();
                } else {
                    repositionSettingRows();
                }
                return true;
            }
            if (activeDraggingSlider != null) {
                updateSliderFromMouse(activeDraggingSlider, mouseX);
                return true;
            }
            return super.mouseDragged(click, deltaX, deltaY);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void updateSliderFromMouse(SettingRow r, double mouseX) {
        try {
            int sliderW = 110;
            int sliderX = r.x + r.width - sliderW - 54;
            float norm = (float) MathHelper.clamp((mouseX - sliderX) / (double) sliderW, 0.0, 1.0);
            float rawVal = r.sliderMin + norm * (r.sliderMax - r.sliderMin);
            float steppedVal = Math.round(rawVal / r.sliderStep) * r.sliderStep;
            steppedVal = MathHelper.clamp(steppedVal, r.sliderMin, r.sliderMax);
            if (r.sliderSetter != null) {
                r.sliderSetter.accept(steppedVal);
                ru.eclipse.client.module.ModuleManager.getInstance().syncConfigsToModules();
                pendingSliderSave = true;
            }
        } catch (Throwable ignored) {}
    }

    private boolean pendingSliderSave = false;

    @Override
    public boolean mouseReleased(Click click) {
        try {
            isDraggingScrollbar = false;
            activeDraggingSlider = null;
            if (pendingSliderSave) {
                pendingSliderSave = false;
                if (visualConfig != null) visualConfig.save();
            }
            if (colorPicker != null && colorPicker.isVisible()) {
                colorPicker.mouseReleased(click.x(), click.y(), click.button());
            }
            return super.mouseReleased(click);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        try {
            int keyCode = input.key();
            if (colorPicker.isVisible()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    colorPicker.close();
                    return true;
                }
            }
            if (searchField != null && searchField.isFocused()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    if (isSearching()) {
                        searchField.setText("");
                        searchQuery = "";
                        setScroll(0);
                        if (currentTab == ScreenTab.PREFIXES) {
                            initTabContent();
                        } else {
                            repositionSettingRows();
                        }
                    }
                    searchField.setFocused(false);
                    return true;
                }
                return searchField.keyPressed(input);
            }
            if (cooldownField != null && cooldownField.isFocused()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    cooldownField.setFocused(false);
                    return true;
                }
                return cooldownField.keyPressed(input);
            }
            if (templateField != null && templateField.isFocused()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    templateField.setFocused(false);
                    return true;
                }
                return templateField.keyPressed(input);
            }
            for (PrefixCard card : prefixCards) {
                if (card.field != null && card.field.isFocused()) {
                    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                        card.field.setFocused(false);
                        return true;
                    }
                    return card.field.keyPressed(input);
                }
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                startClosing();
                return true;
            }
            return super.keyPressed(input);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean charTyped(CharInput input) {
        try {
            if (searchField != null && searchField.isFocused()) {
                return searchField.charTyped(input);
            }
            if (cooldownField != null && cooldownField.isFocused()) {
                return cooldownField.charTyped(input);
            }
            if (templateField != null && templateField.isFocused()) {
                return templateField.charTyped(input);
            }
            for (PrefixCard card : prefixCards) {
                if (card.field != null && card.field.isFocused()) {
                    return card.field.charTyped(input);
                }
            }
            return super.charTyped(input);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void startClosing() {
        if (!isClosing) {
            isClosing = true;
            closeStartTime = System.currentTimeMillis();
            try {
                pendingSliderSave = false;
                collectCurrentPrefixFields();
                ru.eclipse.client.module.ModuleManager.getInstance().syncConfigsToModules();
                if (config != null) config.save();
                if (visualConfig != null) visualConfig.save();
                ru.eclipse.client.core.EclipseStateSync.getInstance().flushPendingSave();
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void close() {
        try {
            pendingSliderSave = false;
            collectCurrentPrefixFields();
            ru.eclipse.client.module.ModuleManager.getInstance().syncConfigsToModules();
            if (config != null) config.save();
            if (visualConfig != null) visualConfig.save();
            ru.eclipse.client.core.EclipseStateSync.getInstance().flushPendingSave();
        } catch (Throwable ignored) {}
        super.close();
    }

    private float getOpenProgress() {
        if (isClosing) {
            float elapsed = (System.currentTimeMillis() - closeStartTime) / CLOSE_DURATION_MS;
            return MathHelper.clamp(1.0f - elapsed, 0.0f, 1.0f);
        } else {
            float elapsed = (System.currentTimeMillis() - openStartTime) / OPEN_DURATION_MS;
            return MathHelper.clamp(elapsed, 0.0f, 1.0f);
        }
    }

    private boolean hasScrollbar() {
        return getMaxScroll() > 0;
    }

    private double getScroll() {
        if (isSearching()) {
            return searchScroll;
        }
        return tabScrollMap.getOrDefault(currentTab, 0.0);
    }

    private void setScroll(double val) {
        double clamped = MathHelper.clamp(val, 0.0, getMaxScroll());
        if (isSearching()) {
            searchScroll = clamped;
        } else {
            tabScrollMap.put(currentTab, clamped);
        }
    }

    private double getMaxScroll() {
        int viewportH = contentBottom - contentTop;
        int totalContentH;

        if (isSearching()) {
            int totalH = 36;
            for (SettingRow r : settingRows) {
                if (matchesSearch(r)) {
                    totalH += 52;
                }
            }
            totalContentH = totalH;
        } else if (currentTab == ScreenTab.PREFIXES) {
            totalContentH = prefixCards.size() * 60 + 160;
        } else {
            int totalH = 20;
            for (SettingRow r : settingRows) {
                if (r.tab == currentTab) {
                    totalH += (r.type == RowType.HEADER ? 32 : 52);
                }
            }
            totalContentH = totalH;
        }

        return Math.max(0, totalContentH - viewportH);
    }

    /**
     * Вспомогательный метод для рендера градиентного текста (Wave-анимация затмения).
     */
    public static void drawGradientText(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, String text, int x, int y, int colorStart, int colorEnd, boolean animated) {
        RenderUtils.drawGradientText(context, textRenderer, text, x, y, colorStart, colorEnd, animated, 1.0f);
    }

    public static void drawGradientText(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, String text, int x, int y, int colorStart, int colorEnd, boolean animated, float alpha) {
        RenderUtils.drawGradientText(context, textRenderer, text, x, y, colorStart, colorEnd, animated, alpha);
    }
}
