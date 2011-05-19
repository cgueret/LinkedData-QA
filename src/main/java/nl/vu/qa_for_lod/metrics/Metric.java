/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import nl.vu.qa_for_lod.Graph;

/**
 * Generic metric to be inherited by every metric implemented in the framework
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
public interface Metric {
	/**
	 * Compute the distance between the given distribution and what would be an
	 * ideal distribution
	 * 
	 * @param distribution
	 *            the distribution to compare the ideal distribution to
	 * @return the distance. 0 means equality, lower is better
	 */
	public abstract double getDistanceToIdealDistribution(Distribution distribution);

	/**
	 * Get the name of the metric
	 * 
	 * @return a String with the metric name
	 */
	public abstract String getName();

	/**
	 * Get the metric scores for the nodes
	 * 
	 * @param graph
	 * @return
	 */
	public abstract Results getResults(Graph graph);
}

/*
 * http://stackoverflow.com/questions/507602/how-to-initialise-a-static-map-in-java
 * static { Map<MetricState, Double> map = new HashMap<MetricState, Double>();
 * map.put(MetricState.AFTER, 0.0); map.put(MetricState.BEFORE, 0.0);
 * DISTANCE_TO_POWER_LAW = Collections.unmodifiableMap(map); }
 */
