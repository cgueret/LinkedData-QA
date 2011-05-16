/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.data.Distribution;
import nl.vu.qa_for_lod.data.Results;

/**
 * Generic metric to be inherited by every metric implemented in the framework
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
public abstract class Metric {
	public final static Integer DISTRIBUTIONS_BINS = 1000;

	public static enum MetricState {
		BEFORE, AFTER
	}

	protected final Results resultsBefore = new Results();
	protected final Results resultsAfter = new Results();
	private final Distribution distribution = new Distribution();
	protected boolean isGreen = true;

	/**
	 * @param state
	 */
	public void processGraph(Graph graph, MetricState state) {
		// Get the requested
		Results results = resultsBefore;
		if (state.equals(MetricState.AFTER))
			results = resultsAfter;

		results.clear();
		this.go(graph, results, state);

		// Save the distribution
		for (Entry<String, Double> r : results.entrySet())
			distribution.increaseCounter(r.getValue(), state);
	}

	/**
	 * List the top suspicious nodes
	 * 
	 * @param number
	 *            The number of nodes to return
	 * @param resources
	 * @return an ordered list of the top suspicious nodes according to the
	 *         metric
	 */
	public List<String> getSuspiciousNodes(int number, Set<Resource> resources) {
		// Get the list of nodes for which we have before and after results
		Set<String> keys = new TreeSet<String>(resultsBefore.keySet());
		keys.retainAll(resultsAfter.keySet());

		// If we want to filter, get only the relevant keys
		if (resources != null) {
			Set<String> tmp = new TreeSet<String>();
			for (Resource r : resources)
				tmp.add(r.toString());
			keys.retainAll(tmp);
		}

		// Compare
		Map<String, Double> diffs = new HashMap<String, Double>();
		for (String key : keys)
			diffs.put(key, Math.abs(resultsBefore.get(key) - resultsAfter.get(key)));

		// Get the ordered list of nodes
		List<String> output = new ArrayList<String>();
		Set<Double> scores = new TreeSet<Double>();
		scores.addAll(diffs.values());
		for (Double score : scores)
			for (Entry<String, Double> entry : diffs.entrySet())
				if (entry.getValue().equals(score))
					output.add(entry.getKey());

		// Return the top "number"
		return output.subList(output.size() - number, output.size());
	}

	/**
	 * @param state
	 * @return
	 */
	public Map<Integer, Integer> getDistribution(MetricState state) {
		// Use the requested result set
		Results results = resultsBefore;
		if (state.equals(MetricState.AFTER))
			results = resultsAfter;

		// Initialise the results table
		Map<Integer, Integer> output = new HashMap<Integer, Integer>();
		for (int key = 0; key < DISTRIBUTIONS_BINS + 1; key++)
			output.put(new Integer(key), new Integer(0));

		// Find the highest value
		Double max = Double.MIN_VALUE;
		for (Entry<String, Double> result : results.entrySet())
			if (result.getValue() > max)
				max = result.getValue();

		// Fill the distribution table
		for (Entry<String, Double> result : results.entrySet()) {
			Integer key = Integer.valueOf((int) (DISTRIBUTIONS_BINS * (result.getValue() / max)));
			output.put(key, output.get(key) + 1);
		}

		return output;
	}

	/**
	 * @return
	 */
	public boolean isGreen() {
		return isGreen;
	}

	/**
	 * @param results
	 * @param state
	 */
	protected abstract void go(Graph graph, Results results, MetricState state);

	/**
	 * @return
	 */
	public abstract String getName();

	/**
	 * @return the distribution
	 */
	public Distribution getDistribution() {
		return distribution;
	}
}

/*
 * http://stackoverflow.com/questions/507602/how-to-initialise-a-static-map-in-java
 * static { Map<MetricState, Double> map = new HashMap<MetricState, Double>();
 * map.put(MetricState.AFTER, 0.0); map.put(MetricState.BEFORE, 0.0);
 * DISTANCE_TO_POWER_LAW = Collections.unmodifiableMap(map); }
 */
