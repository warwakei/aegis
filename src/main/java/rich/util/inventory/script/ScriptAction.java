package rich.util.inventory.script;

/**
 * действие для скрипта
 *
 * @see Script
 */
@FunctionalInterface
public interface ScriptAction {

    /**
     * делаем экшен тут
     */
    void perform();
}
