package nl.vu.qa_for_lod.crawler;


public class HostConfig {
	private long requestRate = 1000;

	// Request rate in ms
	public long getRequestRate() {
		return requestRate;
	}
	
	public void setRequestRate(int requestRate) {
		this.requestRate = requestRate;
	}
}
