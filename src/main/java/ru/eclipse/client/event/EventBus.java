package ru.eclipse.client.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ✦ EventBus — Высокоскоростная шина событий клиента ECLIPSE.
 */
public final class EventBus {

    private static final Map<Class<? extends Event>, List<ListenerInvocation>> LISTENERS = new ConcurrentHashMap<>();

    private static class ListenerInvocation implements Comparable<ListenerInvocation> {
        final Object target;
        final Method method;
        final int priority;

        ListenerInvocation(Object target, Method method, int priority) {
            this.target = target;
            this.method = method;
            this.priority = priority;
            this.method.setAccessible(true);
        }

        void invoke(Event event) {
            try {
                method.invoke(target, event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        @Override
        public int compareTo(ListenerInvocation other) {
            // Более высокий приоритет выполняется раньше
            return Integer.compare(other.priority, this.priority);
        }
    }

    private EventBus() {}

    @SuppressWarnings("unchecked")
    public static void register(Object listener) {
        if (listener == null) return;
        Class<?> clazz = listener.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventTarget.class) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(paramType)) {
                    Class<? extends Event> eventClass = (Class<? extends Event>) paramType;
                    EventTarget target = method.getAnnotation(EventTarget.class);

                    LISTENERS.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                            .add(new ListenerInvocation(listener, method, target.priority()));
                    
                    // Сортировка по приоритету
                    LISTENERS.get(eventClass).sort(null);
                }
            }
        }
    }

    public static void unregister(Object listener) {
        if (listener == null) return;
        for (List<ListenerInvocation> list : LISTENERS.values()) {
            list.removeIf(inv -> inv.target == listener);
        }
    }

    public static <T extends Event> T post(T event) {
        if (event == null) return null;
        List<ListenerInvocation> list = LISTENERS.get(event.getClass());
        if (list != null && !list.isEmpty()) {
            for (ListenerInvocation inv : list) {
                inv.invoke(event);
            }
        }
        return event;
    }
}
