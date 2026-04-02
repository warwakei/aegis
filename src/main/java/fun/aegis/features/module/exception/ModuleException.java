package fun.aegis.features.module.exception;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ModuleException extends RuntimeException {
    String message, moduleName;
}
