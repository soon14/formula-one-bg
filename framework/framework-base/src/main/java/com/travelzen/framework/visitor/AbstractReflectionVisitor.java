package com.travelzen.framework.visitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReflectionVisitor implements Visitor {

	/**
	 * String specifying name of method used to overload by any class extending
	 * this.
	 **/
	public static final String VISIT_METHOD_IMPL = "visitImpl";

	private static Logger LOG = LoggerFactory
			.getLogger(AbstractReflectionVisitor.class);

	private static final String ERR_CLAUSE_SUBTYPE_NOT_FOUND = "Current visitor implementation does not handle visiting "
			+ "non clause subtypes. Tried to visit object ";
	private static final String ERR_FAILED_TO_VISIT = "Failed to visit object ";
	final String ERR_FAILED_TO_FIND_VISIT_IMPL_OBJECT = "Failed to find method that exists in this class!!"
			+ "Was trying to visit object ";
	private static final String DEBUG_LOOKING_AT = "Looking for method "
			+ VISIT_METHOD_IMPL + "(";
	private static final String TRACE_KEEP_LOOKING = "keep looking";
	private static final String RB = ")";

	// private static final Map<Class<? extends Visitor>, Map<Class<? extends
	// Visitable>, Method>> CACHE = new HashMap();

	private static final Map<Class<? extends Visitor>, Map<Class<?>, Method>> CACHE = new HashMap<Class<? extends Visitor>, Map<Class<?>, Method>>();

	// private static final ReferenceMap<Class<? extends Visitor>,Map<Class<?
	// extends Visitable>,Method>> CACHE =
	// new ReferenceMap<Class<? extends Visitor>,Map<Class<? extends
	// Visitable>,Method>>(
	// ReferenceMap.Type.SOFT,
	// new ConcurrentHashMap<Class<? extends Visitor>,Reference<Map<Class<?
	// extends Visitable>,Method>>>());

	/**
	 * Creates a new instance of AbstractReflectionVisitor.
	 */
	public AbstractReflectionVisitor() {
	}

	/**
	 * Method implementing Visitor interface. Uses reflection to find the method
	 * with name VISIT_METHOD_IMPL with the closest match to the clause
	 * subclass.
	 * 
	 * We CACHE the methods used by the visitor. This is done in a soft
	 * ReferenceMap since the skins can be reloaded, and then we would have had
	 * a memory leak.
	 * 
	 * @param clause
	 *            the clause we're visiting.
	 */
	public void visit(final Object clause) {
		Map<Class<?>, Method> map = CACHE.get(getClass());

		if (null == map) {
			map = new ConcurrentHashMap<Class<?>, Method>();
			CACHE.put(getClass(), map);
		}

		Method method = map.get(clause.getClass());

		try {
			if (null == method) {
				method = this.getClass().getDeclaredMethod(
						"visit" + clause.getClass().getSimpleName(),
						clause.getClass());
				map.put(clause.getClass(), method);

			}

			method.invoke(this, new Object[] { clause });

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}

	public void visit(final Object clause, Object... others) {
		Map<Class<?>, Method> map = CACHE.get(getClass());

		if (null == map) {
			map = new ConcurrentHashMap<Class<?>, Method>();
			CACHE.put(getClass(), map);
		}

		Method method = map.get(clause.getClass());

		try {
			if (null == method) {
				Class<?>[] paramsClass = new Class[others.length + 1];
				paramsClass[0] = clause.getClass();
				int i = 1;
				for (Object o : others) {
					paramsClass[i++] = o.getClass();
				}

				method = this.getClass().getDeclaredMethod(
						"visit" + clause.getClass().getSimpleName(),
						paramsClass);
				map.put(clause.getClass(), method);

			}

			method.invoke(this, new Object[] { clause, others });

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}

	public void visit(final Visitable clause) {

		Map<Class<?>, Method> map = CACHE.get(getClass());

		if (null == map) {

			map = new ConcurrentHashMap<Class<?>, Method>();

			CACHE.put(getClass(), map);
		}

		Method method = map.get(clause.getClass());
		if (null == method) {
			method = getMethod(clause.getClass());
			method.setAccessible(true);
			map.put(clause.getClass(), method);
		}
		assert method.equals(getMethod(clause.getClass()));
		try {
			method.invoke(this, new Object[] { clause });

		} catch (IllegalArgumentException ex) {
			LOG.error(ERR_FAILED_TO_VISIT + clause, ex);

		} catch (InvocationTargetException ex) {
			LOG.error(ERR_FAILED_TO_VISIT + clause, ex);
			// IllegalArgumentException often mean an underlying exception.
			// If the underlying exception has a blank stacktrace it's most
			// likely a sun bug
			// when running hotspot compiled methods
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4966410
			// The JVM flag -XX:-OmitStackTraceInFastThrow fixes it.
			for (Throwable t = ex; t != null; t = t.getCause()) {
				LOG.error(t.getMessage(), t);
			}

		} catch (IllegalAccessException ex) {
			LOG.error(ERR_FAILED_TO_VISIT + clause, ex);
		}
	}

	/**
	 * Final fallback method. This means that the object being visited is not a
	 * Clause (or subclass of) object! This behaviour is not intendedly
	 * supported and this implementation throws an IllegalArgumentException!
	 * 
	 * @param clause
	 *            the clause we're visiting (that's not acutally a clause
	 *            subtype ;)
	 */
	protected void visitImpl(final Object clause) {
		throw new IllegalArgumentException(ERR_CLAUSE_SUBTYPE_NOT_FOUND
				+ clause.getClass().getName());
	}

	private Method getMethod(final Class<?> clauseClass) {
		Method method = null;

		LOG.trace("getMethod(" + clauseClass.getName() + ")");

		// Try the superclasses
		Class<?> currClauseClass = clauseClass;
		while (method == null && currClauseClass != Object.class) {
			LOG.trace(DEBUG_LOOKING_AT + currClauseClass.getName() + RB);

			method = getDeclaredMethod(currClauseClass);

			if (method == null) {
				currClauseClass = currClauseClass.getSuperclass();
			}
		}

		// Try the interfaces.
		// Gets alittle bit tricky because we must not only search subinterfaces
		// but search both interfaces and superinterfaces of superclasses...
		currClauseClass = clauseClass;
		while (method == null && currClauseClass != Object.class) {

			method = getMethodFromInterface(currClauseClass);
			currClauseClass = currClauseClass.getSuperclass();
		}

		// fallback to visitImpl(Object)
		if (method == null) {

			method = getDeclaredMethod(Object.class);

			if (method == null) {
				LOG.error(ERR_FAILED_TO_FIND_VISIT_IMPL_OBJECT
						+ clauseClass.getName());
			}

		}
		LOG.trace("end getMethod(" + clauseClass.getName() + ")");
		return method;
	}

	/**
	 * The interfaces in this array will already be in a suitable order.
	 * According to java reflection's getMethod contract this order will match
	 * the order listed in the implements(/extends) definition of the Clause
	 * subclass.
	 **/
	private Method getMethodFromInterface(final Class<?> clauseClass) {

		Method method = null;

		LOG.trace("getMethodFromInterface(" + clauseClass.getName() + ")");

		final Class<?>[] interfaces = clauseClass.getInterfaces();
		for (int i = 0; i < interfaces.length && method == null; i++) {

			LOG.trace(DEBUG_LOOKING_AT + interfaces[i].getName() + RB);

			method = getDeclaredMethod(interfaces[i]);

			if (method == null) {
				// [RECURSION] Look for super interfaces
				method = getMethodFromInterface(interfaces[i]);
			} else {
				// This is the most useful log statement in this file,
				// but gets called too many times per request to be promoted to
				// debug.
				LOG.trace("Found method accepting <"
						+ interfaces[i].getSimpleName() + "> in "
						+ method.getDeclaringClass().getSimpleName());
			}
		}

		LOG.trace("end getMethodFromInterface(" + clauseClass.getName() + ")");
		return method;
	}

	/**
	 * Because Class.getDeclaredMethod(..) behaves differently to getMethod(..)
	 * in that it does not look into superclasses we must manually look through
	 * the superclass heirarchy. We don't want to use getMethod(..) either
	 * because it will only return public methods, and we would like our
	 * visitImpl methods to remain private/protected.
	 **/
	private Method getDeclaredMethod(final Class<?> clauseClass) {

		for (Class<?> cls = getClass();; cls = cls.getSuperclass()) {
			if (cls != null) {
				try {
					return cls.getDeclaredMethod(VISIT_METHOD_IMPL,
							new Class[] { clauseClass });

				} catch (NoSuchMethodException e) {
					LOG.trace(TRACE_KEEP_LOOKING);
				}
			} else {
				return null;
			}
		}
	}

}
