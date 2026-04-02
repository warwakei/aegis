package fun.aegis.utils.client.managers.event;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import fun.aegis.utils.client.managers.event.events.Event;
import fun.aegis.utils.client.managers.event.events.EventStoppable;
import fun.aegis.utils.client.managers.event.types.Priority;
import fun.aegis.common.logger.implement.ConsoleLogger;
import fun.aegis.common.logger.implement.MinecraftLogger;
import fun.aegis.features.module.exception.ModuleException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventManager {
    private static final Map<Class<? extends Event>, List<MethodData>> REGISTRY_MAP = new ConcurrentHashMap<>();
    public EventManager() {}

    public void register(Object object) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            if (isMethodBad(method)) {
                continue;
            }

            register(method, object);
        }
    }

    public void register(Object object, Class<? extends Event> eventClass) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            if (isMethodBad(method, eventClass)) {
                continue;
            }

            register(method, object);
        }

    }

    public void unregister(Object object) {
        for (final List<MethodData> dataList : REGISTRY_MAP.values()) {
            dataList.removeIf(data -> data.source().equals(object));
        }

        cleanMap(true);
    }

    public void unregister(Object object, Class<? extends Event> eventClass) {
        if (REGISTRY_MAP.containsKey(eventClass)) {
            REGISTRY_MAP.get(eventClass).removeIf(data -> data.source().equals(object));
            cleanMap(true);
        }
    }

    private void register(Method method, Object object) {
        Class<? extends Event> indexClass = (Class<? extends Event>) method.getParameterTypes()[0];
        final MethodData data = new MethodData(object, method, method.getAnnotation(EventHandler.class).value());

        if (!data.target().isAccessible()) {
            data.target().setAccessible(true);
        }

        if (REGISTRY_MAP.containsKey(indexClass)) {
            if (!REGISTRY_MAP.get(indexClass).contains(data)) {
                REGISTRY_MAP.get(indexClass).add(data);
                sortListValue(indexClass);
            }
        } else {
            REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<MethodData>() {
                private static final long serialVersionUID = 666L;
                {
                    add(data);
                }
            });
        }
    }

    public void removeEntry(Class<? extends Event> indexClass) {
        Iterator<Map.Entry<Class<? extends Event>, List<MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

        while (mapIterator.hasNext()) {
            if (mapIterator.next().getKey().equals(indexClass)) {
                mapIterator.remove();
                break;
            }
        }
    }
    public static void cleanMap(boolean onlyEmptyEntries) {
        Iterator<Map.Entry<Class<? extends Event>, List<MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

        while (mapIterator.hasNext()) {
            if (!onlyEmptyEntries || mapIterator.next().getValue().isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private static void sortListValue(Class<? extends Event> indexClass) {
        List<MethodData> sortedList = new CopyOnWriteArrayList<MethodData>();

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
            ConsoleLogger consoleLogger = new ConsoleLogger();
            String errorMessage = "Illegal access to method. ";
            errorMessage += "Method: " + data.target().getName() + ", ";
            errorMessage += "Argument: " + argument.toString() + ", ";
            errorMessage += "Log: " + e.fillInStackTrace();
            consoleLogger.log(errorMessage);
        } catch (IllegalArgumentException e) {
            ConsoleLogger consoleLogger = new ConsoleLogger();
            String errorMessage = "Illegal arguments passed to method. ";
            errorMessage += "Method: " + data.target().getName() + ", ";
            errorMessage += "Argument: " + argument.toString() + ", ";
            errorMessage += "Log: " + e.getCause();
            consoleLogger.log(errorMessage);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            ConsoleLogger consoleLogger = new ConsoleLogger();
            MinecraftLogger minecraftLogger = new MinecraftLogger();
            if (cause instanceof ModuleException moduleException) {
                minecraftLogger.minecraftLog(Text.literal("[" + moduleException.getModuleName() + "] " + Formatting.RED + moduleException.getMessage()));
            } else {
                String errorMessage = "Exception occurred within invoked method. ";
                errorMessage += "Method: " + data.target().getName() + ", ";
                errorMessage += "Argument: " + argument.toString() + ", ";
                errorMessage += "Log: " + e.getCause();
                consoleLogger.log(errorMessage);
            }
        }
    }

    private record MethodData(Object source, Method target, byte priority) {}
}
