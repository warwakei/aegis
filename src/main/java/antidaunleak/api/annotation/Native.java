package antidaunleak.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface Native {
    Type type() default Type.STANDARD;

    enum Type {
        STANDARD,
        VMProtectBeginMutation,
        VMProtectBeginVirtualization,
        VMProtectBeginUltra
    }
}
