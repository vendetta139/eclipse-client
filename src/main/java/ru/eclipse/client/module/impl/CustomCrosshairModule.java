package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.BooleanSetting;
import ru.eclipse.client.module.setting.ColorSetting;
import ru.eclipse.client.module.setting.ModeSetting;
import ru.eclipse.client.module.setting.NumberSetting;

public class CustomCrosshairModule extends Module {
    private final ModeSetting style = new ModeSetting("Стиль", "Форма прицела", "Dot", "Dot", "Cross", "Circle", "T-Shape");
    private final NumberSetting size = new NumberSetting("Размер", "Размер прицела в пикселях", 4.0, 1.0, 12.0, 1.0);
    private final NumberSetting gap = new NumberSetting("Зазор", "Отступ от центра прицела", 2.0, 0.0, 8.0, 1.0);
    private final NumberSetting thickness = new NumberSetting("Толщина", "Толщина линий прицела", 1.0, 1.0, 4.0, 1.0);
    private final BooleanSetting centerDot = new BooleanSetting("Точка в центре", "Ставит точку в центре прицела", false);
    private final ColorSetting color = new ColorSetting("Цвет", "Цвет прицела", 0xFFFFFFFF);

    public CustomCrosshairModule() {
        super("CustomCrosshair", "Заменяет прицел на точку, круг или крестик", Category.COMBAT_PVP, 0, false);
        addSetting(style);
        addSetting(size);
        addSetting(gap);
        addSetting(thickness);
        addSetting(centerDot);
        addSetting(color);
    }

    public ModeSetting getStyle() {
        return style;
    }

    public NumberSetting getSize() {
        return size;
    }

    public NumberSetting getGap() {
        return gap;
    }

    public NumberSetting getThickness() {
        return thickness;
    }

    public BooleanSetting getCenterDot() {
        return centerDot;
    }

    public ColorSetting getColor() {
        return color;
    }
}
