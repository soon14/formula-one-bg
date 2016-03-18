package com.travelzen.framework.retry.dict;


public enum RetryTaskType {
	AUDIT_RESULT("审核结果通知"),
    CREDIT_LIMIT_INFO("额度生成通知"), 
    CREDIT_LIMIT_EFFECT("额度生效通知"),
    CREDIT_TRADE_RESULT("授信交易通知");
	private String desc;

	private RetryTaskType(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

}
