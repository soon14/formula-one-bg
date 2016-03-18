package com.travelzen.framework.retry.policy;

import com.travelzen.framework.retry.dao.po.RetryTask;

public interface IRetryTaskPolicy {
	/**
	 * 是否可以重试
	 * @param task
	 * @return
	 */
	public boolean canRetry(RetryTask task);
	
	/**
	 * 是否还可以执行
	 * @param task
	 * @return
	 */
	public boolean isAlive(RetryTask task);
}
