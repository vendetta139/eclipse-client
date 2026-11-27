package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.BooleanSetting;
import ru.eclipse.client.module.setting.NumberSetting;

public class AutoPrefixModule extends Module {
    private final NumberSetting interval = new NumberSetting("Интервал", "Пауза между сменами в секундах", 30.0, 5.0, 300.0, 5.0, "s");
    private final BooleanSetting random = new BooleanSetting("Случайный порядок", "Берёт префиксы вразброс вместо списка", false);

    public AutoPrefixModule() {
        super("AutoPrefix", "Меняет донат-префикс на сервере по таймеру", Category.PREFIXES, 0, true);
        addSetting(interval);
        addSetting(random);
    }

    public NumberSetting getInterval() {
        return interval;
    }

    public BooleanSetting getRandom() {
        return random;
    }
}
