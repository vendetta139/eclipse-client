package ru.example.donorprefix.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * CPS Counter (клики в секунду):
 * Отслеживает реальную скорость кликов ЛКМ и ПКМ.
 */
public class CPSCounter {

    private static final List<Long> LEFT_CLICKS = new ArrayList<>();
    private static final List<Long> RIGHT_CLICKS = new ArrayList<>();

    public static void onLeftClick() {
        long now = System.currentTimeMillis();
        synchronized (LEFT_CLICKS) {
            LEFT_CLICKS.add(now);
        }
    }

    public static void onRightClick() {
        long now = System.currentTimeMillis();
        synchronized (RIGHT_CLICKS) {
            RIGHT_CLICKS.add(now);
        }
    }

    public static int getLeftCps() {
        return getCps(LEFT_CLICKS);
    }

    public static int getRightCps() {
        return getCps(RIGHT_CLICKS);
    }

    private static int getCps(List<Long> list) {
        long cutoff = System.currentTimeMillis() - 1000;
        synchronized (list) {
            list.removeIf(time -> time < cutoff);
            return list.size();
        }
    }
}
