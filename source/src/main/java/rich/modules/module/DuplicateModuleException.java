package rich.modules.module;

public class DuplicateModuleException extends RuntimeException {
    public DuplicateModuleException(String moduleName) {
        super("Duplicate module registration: " + moduleName);
    }
}