package ru.example.donorprefix.visual;

/**
 * CPS Counter (клики в секунду) на примитивном кольцевом буфере:
 * без упаковки Long и без аллокаций на каждый клик/кадр.
 */
public class CPSCounter {

    private static final int CAPACITY = 512;
    private static final int MASK = CAPACITY - 1;

    private static final RingBuffer LEFT_CLICKS = new RingBuffer();
    private static final RingBuffer RIGHT_CLICKS = new RingBuffer();

    private static final class RingBuffer {
        private final long[] timestamps = new long[CAPACITY];
        private int head = 0;
        private int tail = 0;
        private int size = 0;

        synchronized void push(long now) {
            if (size == CAPACITY) {
                head = (head + 1) & MASK;
                size--;
            }
            timestamps[tail] = now;
            tail = (tail + 1) & MASK;
            size++;
        }

        synchronized int countSince(long cutoff) {
            while (size > 0 && timestamps[head] < cutoff) {
                head = (head + 1) & MASK;
                size--;
            }
            return size;
        }
    }

    public static void onLeftClick() {
        LEFT_CLICKS.push(System.currentTimeMillis());
    }

    public static void onRightClick() {
        RIGHT_CLICKS.push(System.currentTimeMillis());
    }

    public static int getLeftCps() {
        return LEFT_CLICKS.countSince(System.currentTimeMillis() - 1000L);
    }

    public static int getRightCps() {
        return RIGHT_CLICKS.countSince(System.currentTimeMillis() - 1000L);
    }
}
