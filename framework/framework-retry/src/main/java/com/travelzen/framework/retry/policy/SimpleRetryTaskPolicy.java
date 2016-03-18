package com.travelzen.framework.retry.policy;

import java.util.Date;

import com.travelzen.framework.retry.dao.po.RetryTask;
import com.travelzen.framework.retry.dict.RetryTaskState;


public class SimpleRetryTaskPolicy implements IRetryTaskPolicy{

	@Override
	public boolean canRetry(RetryTask task) {
		if(!isAlive(task))
			return false;
		IRetryTaskBackOffPolicy backOffPolicy =  RetryTaskBackOffPolicyFactory.getPolicy(task.getBackoffPolicy());
		if(backOffPolicy == null)
			return false;
		return backOffPolicy.isTimeUp(task.getEndTime(), task.getCurrentAttempts() + 1, task);
	}

	@Override
	public boolean isAlive(RetryTask task) {
		Date now = new Date();
		if(task.getTaskDeadline() == null && task.getMaxAttempts() == 0)
			return false;
		if(task.getTaskDeadline() != null && now.compareTo(task.getTaskDeadline()) > 0)
			return false;
		if(task.getMaxAttempts() > 0 && task.getCurrentAttempts() >= task.getMaxAttempts()) 
			return false;
		if(RetryTaskState.success.name().equals(task.getState()))
			return false;
		return true;
	}

}
