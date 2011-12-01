package nl.vu.qa_for_lod.crawler;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.openjena.atlas.lib.MapUtils;

/**
 * Keeps track of the state of hosts:
 * .) How often the host was unavailable
 * .) 
 * 
 * 
 * @author Claus Stadler
 */
public class HostState
{
	public HostState()
	{
	}

	public HostState(String name)
	{
		this.name = name;
	}

	private String name;
	
	// When the host was most recently requested
	private long lastRequestTime = 0;

	// How often the host was requested
	private int requestCount = 0;
	
	// How often which status code was received
	private Map<Integer, Integer> httpStatusCounts = Collections.synchronizedMap(new TreeMap<Integer, Integer>());

	// How often a read took too long
	private int readTimeOutCount = 0;
	
	// How often no (interesting) data was received
	private int emptyResultCount = 0;

	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public long getLastRequestTime() {
		return lastRequestTime;
	}

	public synchronized void setLastRequestTime(long lastRequestTime) {
		this.lastRequestTime = lastRequestTime;
	}

	public int getRequestCount() {
		return requestCount;
	}

	public int getReadTimeOutCount() {
		return readTimeOutCount;
	}
	
	public void setRequestCount(int requestCount) {
		this.requestCount = requestCount;
	}

	// Creates a copy of the map
	public synchronized NavigableMap<Integer, Integer> getHttpStatusCounts() {
		return new TreeMap<Integer, Integer>(httpStatusCounts);
	}

	public void setHttpStatusCounts(NavigableMap<Integer, Integer> httpStatusCounts) {
		this.httpStatusCounts = httpStatusCounts;
	}

	public int getEmptyResultCount() {
		return emptyResultCount;
	}

	public void setEmptyResultCount(int emptyResultCount) {
		this.emptyResultCount = emptyResultCount;
	}
	
	public synchronized void incrementRequestCount() {
		requestCount += 1;
	}
	
	public synchronized void incrementEmptyResultCount() {
		emptyResultCount += 1;
	}
	
	public synchronized void incrementHttpStatusCount(int httpStatusCode) {
		MapUtils.increment(httpStatusCounts, httpStatusCode);
	}
	
	public synchronized void incrementReadTimeOutCount() 
	{
		readTimeOutCount += 1;
	}

	@Override
	public String toString() {
		return "HostState [name=" + name + ", lastRequestTime="
				+ lastRequestTime + ", requestCount=" + requestCount
				+ ", httpStatusCounts=" + httpStatusCounts
				+ ", readTimeOutCount=" + readTimeOutCount
				+ ", emptyResultCount=" + emptyResultCount + "]";
	}
	
	
}


