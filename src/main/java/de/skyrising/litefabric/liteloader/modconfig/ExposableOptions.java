package de.skyrising.litefabric.liteloader.modconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposableOptions {
    ConfigStrategy strategy() default ConfigStrategy.Unversioned;
    String filename() default "";
    boolean aggressive() default false;
}
