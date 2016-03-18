package com.travelzen.framework.web.format.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *   use classs in   framework-spring  instead
 * @author liang.wang
 *
 */
@Deprecated

@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface DateFormat {
	String value() default "yyyy-MM-dd";
}
