/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import java.util.Collection;
import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.Graph;

/**
 * Generic metric to be inherited by every metric implemented in the framework
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
public interface Metric {
	/**
	 * Generate the ideal distribution for the metric
	 * 
	 * @param inputDistribution
	 *            the observed distribution to take in account when creating the
	 *            ideal distribution
	 * @return
	 */
	public abstract Distribution getIdealDistribution(Distribution inputDistribution);

	/**
	 * Get the name of the metric
	 * 
	 * @return a String with the metric name
	 */
	public abstract String getName();

	/**
	 * Get the metric score for the resource
	 * 
	 * @param graph
	 * @param resource
	 * @return
	 */
	public abstract double getResult(Graph graph, Resource resource);

	/**
	 * Not every metric may be meaningful over a set of resources
	 * 
	 * @param graph
	 * @param resources
	 * @return True if the metric is applicable to a particular set of resources
	 */
	public abstract boolean isApplicableFor(Graph graph, Collection<Resource> resources);
}

/*
 * http://stackoverflow.com/questions/507602/how-to-initialise-a-static-map-in-java
 * static { Map<MetricState, Double> map = new HashMap<MetricState, Double>();
 * map.put(MetricState.AFTER, 0.0); map.put(MetricState.BEFORE, 0.0);
 * DISTANCE_TO_POWER_LAW = Collections.unmodifiableMap(map); }
 */
