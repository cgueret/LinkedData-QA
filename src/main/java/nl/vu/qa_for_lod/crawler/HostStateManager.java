package nl.vu.qa_for_lod.crawler;

import java.util.HashMap;
import java.util.Map;

public class HostStateManager
{
	private static HostStateManager defaultInstance = null;
	
	public static HostStateManager getDefaultInstance() {
		if(defaultInstance == null) {
			defaultInstance = new HostStateManager();
		}
		
		return defaultInstance;
	}
	
	private Map<String, HostState> map = new HashMap<String, HostState>();
	
	public synchronized Map<String, HostState> getMap() {
		return new HashMap<String, HostState>(map);
	}
	
	public synchronized HostState getOrCreate(String hostName) {
		HostState result = map.get(hostName);
		if(result == null) {
			result = new HostState(hostName);
			map.put(hostName, result);
		}
		return result;
		//return org.aksw.commons.collections.MapUtils.getOrCreate(map, hostName, HostState.class);
	}
}