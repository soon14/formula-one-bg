package com.travelzen.framework.dao.rdbms.dataSource;

/**
 * @author yujunfeng
 * @function 获得和设置上下文环境的类，主要负责改变上下文数据源的名称
 * @className DataSourceContextHolder.java
 */

public class DataSourceContextHolder {

	@SuppressWarnings("rawtypes")
	private static final ThreadLocal contextHolder = new ThreadLocal();

	@SuppressWarnings("unchecked")
	public static void setDataSourceType(DataSourceType dataSourceType) {
		contextHolder.set(dataSourceType);
	}

	public static DataSourceType getDataSourceType() {
		return (DataSourceType) contextHolder.get();
	}

	public static void clearDataSourceType() {
		contextHolder.remove();
	}

}
