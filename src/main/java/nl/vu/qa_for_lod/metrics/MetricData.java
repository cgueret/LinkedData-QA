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

import nl.vu.qa_for_lod.report.MetricState;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class MetricData {
	/** Distance to ideal value */
	private Map<MetricState, Double> distanceMap = new HashMap<MetricState, Double>();

	/** Distribution of the results */
	private Map<MetricState, Distribution> distributionMap = new HashMap<MetricState, Distribution>();

	/** Raw results mapping every node to a value */
	private Map<MetricState, Results> resultsMap = new HashMap<MetricState, Results>();

	/**
	 * 
	 */
	public void clear() {
		resultsMap.clear();
		distributionMap.clear();
		distanceMap.clear();
	}

	/**
	 * @return
	 */
	public Distribution getDistribution(MetricState state) {
		if (distributionMap.get(state) == null) {
			Distribution distribution = new Distribution();
			for (Entry<String, Double> r : resultsMap.get(state).entrySet())
				distribution.increaseCounter(r.getValue());
			distributionMap.put(state, distribution);
		}
		return distributionMap.get(state);
	}

	/**
	 * @return
	 */
	public Results getResults(MetricState state) {
		return resultsMap.get(state);
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
		// Get the results
		Results resultsBefore = getResults(MetricState.BEFORE);
		Results resultsAfter = getResults(MetricState.AFTER);

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
			diffs.put(key, Math.abs(resultsAfter.get(key) - resultsBefore.get(key)));

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
	 * @return
	 */
	public boolean isGreen() {
		return distanceMap.get(MetricState.AFTER) <= distanceMap.get(MetricState.BEFORE);
	}

	/**
	 * @param state
	 * @param distanceToIdealDistribution
	 */
	public void setDistanceToIdeal(MetricState state, double distanceToIdealDistribution) {
		distanceMap.put(state, distanceToIdealDistribution);
	}

	/**
	 * @param results
	 */
	public void setResults(MetricState state, Results results) {
		resultsMap.put(state, results);
		distributionMap.put(state, null);
		distanceMap.put(state, null);
	}
}
