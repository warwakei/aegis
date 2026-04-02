package rich.events.api;

import rich.events.api.events.Event;
import rich.events.api.events.EventStoppable;
import rich.events.api.types.Priority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventManager {
    private static final Map<Class<? extends Event>, List<MethodData>> REGISTRY_MAP = new HashMap<>();

    public EventManager() {}

    public static void register(Object object) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            if (isMethodBad(method)) {
                continue;
            }

            register(method, object);
        }
    }

    public static void register(Object object, Class<? extends Event> eventClass) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            if (isMethodBad(method, eventClass)) {
                continue;
            }

            register(method, object);
        }
    }

    public static void unregister(Object object) {
        for (final List<MethodData> dataList : REGISTRY_MAP.values()) {
            dataList.removeIf(data -> data.source().equals(object));
        }

        cleanMap(true);
    }

    public static void unregister(Object object, Class<? extends Event> eventClass) {
        if (REGISTRY_MAP.containsKey(eventClass)) {
            REGISTRY_MAP.get(eventClass).removeIf(data -> data.source().equals(object));
            cleanMap(true);
        }
    }

    private static void register(Method method, Object object) {
        @SuppressWarnings("unchecked")
        Class<? extends Event> indexClass = (Class<? extends Event>) method.getParameterTypes()[0];
        final MethodData data = new MethodData(object, method, method.getAnnotation(EventHandler.class).value());

        if (!data.target().canAccess(data.source())) {
            data.target().setAccessible(true);
        }

        if (REGISTRY_MAP.containsKey(indexClass)) {
            if (!REGISTRY_MAP.get(indexClass).contains(data)) {
                REGISTRY_MAP.get(indexClass).add(data);
                sortListValue(indexClass);
            }
        } else {
            REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<>());
            REGISTRY_MAP.get(indexClass).add(data);
        }
    }

    public static void removeEntry(Class<? extends Event> indexClass) {
        REGISTRY_MAP.entrySet().removeIf(entry -> entry.getKey().equals(indexClass));
    }

    public static void cleanMap(boolean onlyEmptyEntries) {
        if (onlyEmptyEntries) {
            REGISTRY_MAP.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        } else {
            REGISTRY_MAP.clear();
        }
    }

    private static void sortListValue(Class<? extends Event> indexClass) {
        List<MethodData> sortedList = new CopyOnWriteArrayList<>();

        for (final byte priority : Priority.VALUE_ARRAY) {
            for (final MethodData data : REGISTRY_MAP.get(indexClass)) {
                if (data.priority() == priority) {
                    sortedList.add(data);
                }
            }
        }

        REGISTRY_MAP.put(indexClass, sortedList);
    }

    private static boolean isMethodBad(Method method) {
        return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventHandler.class);
    }

    private static boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
        return isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
    }

    public static Event callEvent(final Event event) {
        List<MethodData> dataList = REGISTRY_MAP.get(event.getClass());

        if (dataList != null) {
            if (event instanceof EventStoppable stoppable) {
                for (final MethodData data : dataList) {
                    invoke(data, event);

                    if (stoppable.isStopped()) {
                        break;
                    }
                }
            } else {
                for (final MethodData data : dataList) {
                    try {
                        invoke(data, event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return event;
    }

    private static void invoke(MethodData data, Event argument) {
        try {
            data.target().invoke(data.source(), argument);
        } catch (IllegalAccessException e) {
            String errorMessage = "Illegal access to method. ";
            errorMessage += "Method: " + data.target().getName() + ", ";
            errorMessage += "Argument: " + argument.toString() + ", ";
            errorMessage += "Log: " + e.fillInStackTrace();
            System.out.println(errorMessage);
        } catch (IllegalArgumentException e) {
            String errorMessage = "Illegal arguments passed to method. ";
            errorMessage += "Method: " + data.target().getName() + ", ";
            errorMessage += "Argument: " + argument.toString() + ", ";
            errorMessage += "Log: " + e.getCause();
            System.out.println(errorMessage);
        } catch (InvocationTargetException e) {
            String errorMessage = "Exception occurred within invoked method. ";
            errorMessage += "Method: " + data.target().getName() + ", ";
            errorMessage += "Argument: " + argument.toString() + ", ";
            errorMessage += "Log: " + e.getCause();
            System.out.println(errorMessage);
        }
    }

    private record MethodData(Object source, Method target, byte priority) {}
}