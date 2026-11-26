package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.VisualConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD-виджет Armor HUD:
 * Отображает 4 элемента надетой брони (Шлем, Нагрудник, Поножи, Ботинки) с полосами и цифрами прочности.
 * Размеры элемента строго охватывают все 4 слота (56 x 80).
 */
public class ArmorHudElement extends HudElement {

    public static final int HUD_WIDTH = 56;
    public static final int ITEM_HEIGHT = 18;
    public static final int TOTAL_HEIGHT = 4 * ITEM_HEIGHT + 8; // 80

    public ArmorHudElement() {
        super("armorhud", "Armor HUD", HUD_WIDTH, TOTAL_HEIGHT);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig cfg = VisualConfig.getInstance();

        // 1. Отрисовка подложки СТРОГО в локальных координатах (0, 0 .. width, height)
        RenderUtils.drawRoundedRect(context, 0, 0, this.width, this.height, 8, cfg.themeBgColor);
        RenderUtils.drawRoundedOutline(context, 0, 0, this.width, this.height, 8, cfg.themeOutlineColor);

        // 2. Получение экипированных предметов
        List<ItemStack> items = new ArrayList<>();
        if (client.player != null) {
            EquipmentSlot[] slots = {
                    EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.FEET
            };
            boolean hasAny = false;
            for (EquipmentSlot slot : slots) {
                ItemStack stack = client.player.getEquippedStack(slot);
                items.add(stack);
                if (stack != null && !stack.isEmpty()) {
                    hasAny = true;
                }
            }
            // Если игрок голый — в редакторе показываем превью
            if (!hasAny) {
                items.clear();
                items.add(new ItemStack(Items.NETHERITE_HELMET));
                items.add(new ItemStack(Items.NETHERITE_CHESTPLATE));
                items.add(new ItemStack(Items.NETHERITE_LEGGINGS));
                items.add(new ItemStack(Items.NETHERITE_BOOTS));
            }
        } else {
            // Превью-слоты
            items.add(new ItemStack(Items.NETHERITE_HELMET));
            items.add(new ItemStack(Items.NETHERITE_CHESTPLATE));
            items.add(new ItemStack(Items.NETHERITE_LEGGINGS));
            items.add(new ItemStack(Items.NETHERITE_BOOTS));
        }

        // 3. Отрисовка всех 4 слотов строго внутри высоты элемента
        int curY = 4;
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) {
                context.drawItem(stack, 3, curY);

                if (stack.isDamageable()) {
                    int maxDmg = stack.getMaxDamage();
                    int curDmg = maxDmg - stack.getDamage();
                    float ratio = MathHelper.clamp((float) curDmg / (float) maxDmg, 0f, 1f);

                    int barW = 26;
                    int barH = 3;
                    int barX = 24;
                    int barY = curY + 11;

                    int barColor = ratio > 0.5f
                            ? RenderUtils.lerpColor(0xFFEAB308, 0xFF10B981, (ratio - 0.5f) * 2f)
                            : RenderUtils.lerpColor(0xFFEF4444, 0xFFEAB308, ratio * 2f);

                    context.fill(barX, barY, barX + barW, barY + barH, 0x66000000);
                    context.fill(barX, barY, barX + (int) (barW * ratio), barY + barH, barColor);

                    String dmgStr = String.valueOf(curDmg);
                    RenderUtils.drawTextWithShadow(context, client.textRenderer, dmgStr, barX, curY + 2, cfg.themeTextColor);
                } else {
                    int count = stack.getCount();
                    if (count > 1) {
                        RenderUtils.drawTextWithShadow(context, client.textRenderer, "x" + count, 24, curY + 5, cfg.themeSubtextColor);
                    } else {
                        RenderUtils.drawTextWithShadow(context, client.textRenderer, "100%", 24, curY + 5, cfg.themeTextColor);
                    }
                }
            }
            curY += ITEM_HEIGHT;
        }
    }

    @Override
    public void loadConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        this.x = cfg.armorHudX;
        this.y = cfg.armorHudY;
        this.scale = cfg.armorHudScale > 0.2f ? cfg.armorHudScale : 1.0f;
        this.enabled = cfg.armorHudEnabled;
    }

    @Override
    public void saveConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        cfg.armorHudX = this.x;
        cfg.armorHudY = this.y;
        cfg.armorHudScale = this.scale;
        cfg.armorHudEnabled = this.enabled;
    }

    @Override
    public void resetDefaultPosition(int screenWidth, int screenHeight) {
        this.x = screenWidth / 2.0f + 100.0f;
        this.y = Math.max(14, screenHeight - this.height - 10.0f);
        this.scale = 1.0f;
    }
}
