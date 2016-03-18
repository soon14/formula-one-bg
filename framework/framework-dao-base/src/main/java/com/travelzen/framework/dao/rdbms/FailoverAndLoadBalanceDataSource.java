package com.travelzen.framework.dao.rdbms;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.MDC;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
/**
 * 该数据源代理后面可以管理一组主数据源和一组从数据源，当主数据源全部失效后，才会使用从数据源。 
 * 
 * 每组数据源都可以指定自己的使用策略：random或sequence，random 表示随机选择一个数据源，sequence表示对这一组数据源按照编号进行字典序排序，选取第一个可用的数据源。
 * @author shuiren
 *
 */
public class FailoverAndLoadBalanceDataSource extends AbstractRoutingDataSource{
	public static enum DataSourceSelectStrategy{
		random, sequence
	}
	//配置的全部数据源
	private Map<Object, Object> targetDataSources;
	//目前可用的主数据源集群
	private List<String> availableMasterCluster = new ArrayList<String>();
	//目前可用的从数据源集群
	private List<String> availableSlaveCluster = new ArrayList<String>();
	//主数据源默认使用sequence方式选取
	private DataSourceSelectStrategy masterDataSourceSelectStrategy = DataSourceSelectStrategy.sequence;
	//从数据源默认使用random方式选取
	private DataSourceSelectStrategy slaveDataSourceSelectStrategy = DataSourceSelectStrategy.random;
	//检查数据源可用性的时间间隔
	private int checkIntervalsec = 3000;
	
	private static volatile boolean stop;
	
	private  Thread checkThread;
	
	private String name;
	
	class CheckTask implements   Runnable  {
		
		@Override
		public void run() {
			
			String rpid = RandomStringUtils.randomNumeric(10);
			MDC.put("rpid", String.format("[rpid=%s]", rpid));

			while (!stop) {

				try {
					
					List<String> availableMasterCluster = new ArrayList<String>();
					
					List<String> availableSlaveCluster = new ArrayList<String>();
					
					for (Object key : targetDataSources.keySet()) {
						
						if(Thread.interrupted())
			 				throw new InterruptedException();
						
						DataSource ds = (DataSource)(targetDataSources.get(key));
						
						try {
							String keyStr = (String)key;
 							Connection connection = ds.getConnection();			
							connection.close();
							if(keyStr.startsWith("master"))
 								availableMasterCluster.add(keyStr);
 							else if(keyStr.startsWith("slave"))
 								availableSlaveCluster.add(keyStr);
						}catch (Throwable thr) {
							logger.error("检查数据源可用性时出现异常", thr);
							logger.error("失败的数据源:" + ds);
						}
					}
					//更新可用集群列表
					if(clusterChanged(FailoverAndLoadBalanceDataSource.this.availableMasterCluster, FailoverAndLoadBalanceDataSource.this.availableSlaveCluster, availableMasterCluster, availableSlaveCluster)){
						FailoverAndLoadBalanceDataSource.this.availableMasterCluster = availableMasterCluster;
						FailoverAndLoadBalanceDataSource.this.availableSlaveCluster = availableSlaveCluster;
						printAvailableDataSources();
					}
					
				} catch (Exception e) {
					logger.error("", e);
					logger.error("检查数据源可用性时出现异常", e);
				}
				try {
					Thread.sleep(checkIntervalsec);
				} catch (InterruptedException e) {
					logger.error("数据源检测线程被中断", e);
					throw new RuntimeException(e);
				}

			}

		}

		private boolean clusterChanged(List<String> oldMasterCluster, List<String> oldSlaveCluster, List<String> newMasterCluster,
				List<String> newSlaveCluster) {
			boolean changed = false;
			if(!CollectionUtils.isEqualCollection(newMasterCluster, oldMasterCluster)){
				changed = changed || true;
				logger.info(String.format("主数据源发生了变化， 新的数据源=%s, 旧的数据源=%s", newMasterCluster, oldMasterCluster));
			}
			
			if(!CollectionUtils.isEqualCollection(newSlaveCluster, oldSlaveCluster)){
				changed = changed || true;
				logger.info(String.format("备用数据源发生了变化， 新的数据源=%s, 旧的数据源=%s", newSlaveCluster, oldSlaveCluster));
			}
				
			return changed;
		}

	};
	
	private void printAvailableDataSources(){
		logger.info("当前可用的主数据源：" + FailoverAndLoadBalanceDataSource.this.availableMasterCluster);
		logger.info("当前可用的备用数据源：" + FailoverAndLoadBalanceDataSource.this.availableSlaveCluster);
		for(Object key : targetDataSources.keySet()){			
			logger.info(String.format("数据源配置, key=%s, detail=%s", key, targetDataSources.get(key)));
		}
	}
	
	@Override
	protected Object determineCurrentLookupKey() {
		List<String> availableMasterCluster = this.availableMasterCluster;
		List<String> availableSlaveCluster = this.availableSlaveCluster;
		if(availableMasterCluster.size() > 0)
			return selectDataSource(availableMasterCluster, this.masterDataSourceSelectStrategy);
		if(availableSlaveCluster.size() > 0)
			return selectDataSource(availableSlaveCluster, this.slaveDataSourceSelectStrategy);
		return null;
	}
	
	private String selectDataSource(List<String> cluster, DataSourceSelectStrategy strategy){
		if(strategy == DataSourceSelectStrategy.sequence){
			//选取编号最小的数据源
			Collections.sort(cluster);
			return cluster.get(0);
		}else{
			//随机选取数据源
			return cluster.get(new Random().nextInt(cluster.size()));
		}
	}
	
	public  void stop(){
		stop = true;
		if(checkThread != null)
			checkThread.interrupt();
	}
	
	@Override
	public void afterPropertiesSet() {
		super.setTargetDataSources(targetDataSources);
		super.afterPropertiesSet();
		String threadName = "check-datasource-thread-" + name;
		checkThread = new Thread(new CheckTask(), threadName);
		checkThread.start();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			logger.error("", e);
		}
	}
	public Map<Object, Object> getTargetDataSources() {
		return targetDataSources;
	}

	public void setTargetDataSources(Map<Object, Object> targetDataSources) {
		this.targetDataSources = targetDataSources;
	}

	public int getCheckIntervalsec() {
		return checkIntervalsec;
	}

	public void setCheckIntervalsec(int checkIntervalsec) {
		this.checkIntervalsec = checkIntervalsec;
	}
	public DataSourceSelectStrategy getMasterDataSourceSelectStrategy() {
		return masterDataSourceSelectStrategy;
	}

	public void setMasterDataSourceSelectStrategy(
			DataSourceSelectStrategy masterDataSourceSelectStrategy) {
		this.masterDataSourceSelectStrategy = masterDataSourceSelectStrategy;
	}

	public DataSourceSelectStrategy getSlaveDataSourceSelectStrategy() {
		return slaveDataSourceSelectStrategy;
	}

	public void setSlaveDataSourceSelectStrategy(
			DataSourceSelectStrategy slaveDataSourceSelectStrategy) {
		this.slaveDataSourceSelectStrategy = slaveDataSourceSelectStrategy;
	}

	
	public String getName() {
		return name;
	}

	
	public void setName(String name) {
		this.name = name;
	}
}
