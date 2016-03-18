package com.travelzen.framework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

/**
 *   use classs in   framework-spring  instead
 * @author liang.wang
 *
 */
@Deprecated

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestArray {
	Class<?> type() default ArrayList.class; // 实例化集合的类型，默认为ArrayList
}
