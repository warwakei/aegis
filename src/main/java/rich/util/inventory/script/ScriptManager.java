package rich.util.inventory.script;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ScriptManager {
    private final Map<String, Script> scripts = new ConcurrentHashMap<>();

    /**
     * достаём скрипт по имени
     *
     * @param name имя скрипта
     * @return скрипт или пусто
     */
    public Optional<Script> getScript(String name) {
        return isNullOrEmpty(name) ? Optional.empty() : Optional.of(scripts.computeIfAbsent(name, x -> new Script()));
    }

    /**
     * суём новый скрипт
     *
     * @param name имя скрипта
     * @param script сам скрипт
     * @return старый скрипт или нулл
     */
    public Script addScript(String name, Script script) {
        if (isNullOrEmpty(name) || script == null) {
            throw new IllegalArgumentException("Script name or instance cannot be null or empty");
        }
        return scripts.put(name, script);
    }

    /**
     * есть ли такой скрипт
     *
     * @param name имя скрипта
     * @return есть или нет
     */
    public boolean containsScript(String name) {
        return !isNullOrEmpty(name) && scripts.containsKey(name);
    }

    /**
     * скрипт уже отработал
     *
     * @param name имя скрипта
     * @return готов или нет
     */
    public boolean finished(String name) {
        return !isNullOrEmpty(name) && getScript(name).isPresent() && getScript(name).get().isFinished();
    }

    /**
     * сносим скрипт нафиг
     *
     * @param name какой сносить
     */
    public void removeScript(String name) {
        if (!isNullOrEmpty(name)) {
            scripts.remove(name);
        }
    }

    /**
     * чистим один скрипт
     *
     * @param name какой чистить
     */
    public void cleanupScript(String name) {
        if (!isNullOrEmpty(name)) {
            scripts.computeIfPresent(name, (k, v) -> {
                v.cleanup();
                return v;
            });
        }
    }

    /**
     * чистим все скрипты
     */
    public void cleanupAll() {
        scripts.forEach((k, v) -> v.cleanup());
    }

    /**
     * сносим все скрипты
     */
    public void clearAll() {
        scripts.clear();
    }

    /**
     * тикаем один скрипт
     *
     * @param name какой тикать
     */
    public void updateScript(String name) {
        updateScript(name, () -> true);
    }

    /**
     * тикаем скрипт если можно
     *
     * @param name какой тикать
     * @param condition когда можно
     */
    public void updateScript(String name, Supplier<Boolean> condition) {
        if (condition.get() && !isNullOrEmpty(name)) {
            scripts.computeIfPresent(name, (k, v) -> {
                v.update();
                return v;
            });
        }
    }

    /**
     * тикаем все скрипты
     */
    public void updateAll() {
        scripts.values().forEach(Script::update);
    }

    /**
     * все имена скриптов
     *
     * @return имена скриптов
     */
    public Set<String> getAllScriptNames() {
        return Collections.unmodifiableSet(scripts.keySet());
    }

    /**
     * все скрипты скопом
     *
     * @return мапа скриптов
     */
    public Map<String, Script> getAllScripts() {
        return Collections.unmodifiableMap(scripts);
    }

    /**
     * строка пустая или нулл
     *
     * @param str чё чекаем
     * @return пусто или нет
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
