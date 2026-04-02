package antidaunleak.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * VMProtectBeginVirtualization - Кушает фпс так намана
 * VMProtectBeginMutation - Ваще не кушаэ но плохо защищает
 * VMProtectBeginUltra - Золотой стандарт
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Native {

    Type type() default Type.STANDARD;

    enum Type {
        STANDARD,
        VMProtectBeginVirtualization,
        VMProtectBeginMutation,
        VMProtectBeginUltra,
    }
}