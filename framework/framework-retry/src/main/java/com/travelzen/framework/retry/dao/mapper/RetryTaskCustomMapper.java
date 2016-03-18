package com.travelzen.framework.retry.dao.mapper;

import com.travelzen.framework.retry.dao.po.RetryTask;
import java.util.List;

public interface RetryTaskCustomMapper {

    List<RetryTask> getOldestRetryTask(Integer top);

}