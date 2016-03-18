package com.travelzen.framework.core.exception;

/**
 * 抛出此异常的方法可以重试
 * @author renshui
 *
 */
public class CanRetryException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public static CanRetryException instance(String retMsg) {
		return new CanRetryException(retMsg, null);
	}

	public CanRetryException(String retMsg, Throwable thr) {
		super(retMsg, thr);
	}

}
