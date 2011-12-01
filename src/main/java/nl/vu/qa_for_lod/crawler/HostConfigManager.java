package nl.vu.qa_for_lod.crawler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HostConfigManager {
	private HostConfig defaultConfig;
	private Map<String, HostConfig> map = Collections.synchronizedMap(new HashMap<String, HostConfig>());
	
	private static HostConfigManager defaultInstance;
	
	public synchronized static HostConfigManager getDefaultInstance()
	{
		if(defaultInstance == null) {
			defaultInstance = new HostConfigManager(); 
		}
		
		return defaultInstance;
	}
	
	public HostConfigManager() {
		this.defaultConfig = new HostConfig();
	}
	
	public synchronized HostConfig getConfig(String hostName) {
		HostConfig result = map.get(hostName);
		if(result == null) {
			result = defaultConfig;
		}
		
		return result;
	}
}
