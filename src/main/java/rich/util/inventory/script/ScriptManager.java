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
     * Returns an Optional containing the Script associated with the specified name.
     * If the Script does not exist, it will be created and added to the map.
     *
     * @param name the name of the script
     * @return an Optional containing the Script, or an empty Optional if the name is null or empty
     */
    public Optional<Script> getScript(String name) {
        return isNullOrEmpty(name) ? Optional.empty() : Optional.of(scripts.computeIfAbsent(name, x -> new Script()));
    }

    /**
     * Adds a new Script to the manager.
     *
     * @param name   the name of the script
     * @param script the script instance to add
     * @return the previous Script associated with the specified name, or null if there was no mapping
     */
    public Script addScript(String name, Script script) {
        if (isNullOrEmpty(name) || script == null) {
            throw new IllegalArgumentException("Script name or instance cannot be null or empty");
        }
        return scripts.put(name, script);
    }

    /**
     * Checks if a script with the specified name exists.
     *
     * @param name the name of the script
     * @return true if the script exists, false otherwise
     */
    public boolean containsScript(String name) {
        return !isNullOrEmpty(name) && scripts.containsKey(name);
    }

    /**
     * Checks if the Script associated with the specified name is finished.
     *
     * @param name the name of the script
     * @return true if the script is finished, false otherwise
     */
    public boolean finished(String name) {
        return !isNullOrEmpty(name) && getScript(name).isPresent() && getScript(name).get().isFinished();
    }

    /**
     * Removes the Script associated with the specified name.
     *
     * @param name the name of the script to remove, ignored if null or empty
     */
    public void removeScript(String name) {
        if (!isNullOrEmpty(name)) {
            scripts.remove(name);
        }
    }

    /**
     * Cleanup the Script associated with the specified name.
     *
     * @param name the name of the script to clean up
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
     * Cleanup all scripts from the manager.
     */
    public void cleanupAll() {
        scripts.forEach((k, v) -> v.cleanup());
    }

    /**
     * Removes all scripts from the manager.
     */
    public void clearAll() {
        scripts.clear();
    }

    /**
     * Updates the Script associated with the specified name.
     *
     * @param name the name of the script to update
     */
    public void updateScript(String name) {
        updateScript(name, () -> true);
    }

    /**
     * Updates the Script associated with the specified name.
     *
     * @param name      the name of the script to update
     * @param condition the condition that must be true for the script to be updated
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
     * Updates all scripts managed by this manager.
     */
    public void updateAll() {
        scripts.values().forEach(Script::update);
    }

    /**
     * Returns an unmodifiable set of all script names managed by this manager.
     *
     * @return an unmodifiable set of script names
     */
    public Set<String> getAllScriptNames() {
        return Collections.unmodifiableSet(scripts.keySet());
    }

    /**
     * Returns an unmodifiable map of all scripts managed by this manager.
     *
     * @return an unmodifiable map of scripts
     */
    public Map<String, Script> getAllScripts() {
        return Collections.unmodifiableMap(scripts);
    }

    /**
     * Checks if a string is null or empty.
     *
     * @param str the string to check
     * @return true if the string is null or empty, false otherwise
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
