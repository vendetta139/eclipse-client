package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.BooleanSetting;
import ru.eclipse.client.module.setting.NumberSetting;

public class ViewModelModule extends Module {
    private final NumberSetting scaleX = new NumberSetting("Масштаб X", "Ширина предмета в руке", 1.0, 0.3, 2.0, 0.05);
    private final NumberSetting scaleY = new NumberSetting("Масштаб Y", "Высота предмета в руке", 1.0, 0.3, 2.0, 0.05);
    private final NumberSetting scaleZ = new NumberSetting("Масштаб Z", "Толщина предмета в руке", 1.0, 0.3, 2.0, 0.05);

    private final NumberSetting posX = new NumberSetting("Позиция X", "Сдвиг по горизонтали", 0.0, -1.0, 1.0, 0.05);
    private final NumberSetting posY = new NumberSetting("Позиция Y", "Сдвиг по вертикали", 0.0, -1.0, 1.0, 0.05);
    private final NumberSetting posZ = new NumberSetting("Позиция Z", "Сдвиг вперед/назад", 0.0, -1.0, 1.0, 0.05);

    private final BooleanSetting blockHit = new BooleanSetting("BlockHit", "Анимация блока мечом при удержании ПКМ", false);
    private final BooleanSetting lowShield = new BooleanSetting("Заниженный щит", "Опускает щит во второй руке вниз", false);

    public ViewModelModule() {
        super("ViewModel", "Позиция, масштаб и анимация предметов в руках от 1-го лица", Category.ANIMATIONS, 0, false);
        addSetting(scaleX);
        addSetting(scaleY);
        addSetting(scaleZ);
        addSetting(posX);
        addSetting(posY);
        addSetting(posZ);
        addSetting(blockHit);
        addSetting(lowShield);
    }

    public NumberSetting getScaleX() {
        return scaleX;
    }

    public NumberSetting getScaleY() {
        return scaleY;
    }

    public NumberSetting getScaleZ() {
        return scaleZ;
    }

    public NumberSetting getPosX() {
        return posX;
    }

    public NumberSetting getPosY() {
        return posY;
    }

    public NumberSetting getPosZ() {
        return posZ;
    }

    public BooleanSetting getBlockHit() {
        return blockHit;
    }

    public BooleanSetting getLowShield() {
        return lowShield;
    }
}
