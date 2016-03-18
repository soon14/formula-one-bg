package com.travelzen.framework.retry.handler;

public interface IRetryTaskHandlerFactory {
	
	/**
	 * 根据任务类型获取任务处理器
	 * @param taskType
	 * @return
	 */
	public IRetryTaskHandler getHandler(String taskType);
}
