package antidaunleak.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * аннотация игнорит метод от обфускации
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface MainClass {
}
