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

import nl.vu.qa_for_lod.data.Results;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public abstract class Metric {
	protected final Results resultsBefore = new Results();
	protected final Results resultsAfter = new Results();

	/**
	 * List the top suspicious nodes
	 * 
	 * @param number
	 *            The number of nodes to return
	 * @return
	 */
	public List<String> getSuspiciousNodes(int number) {
		// Compare
		Map<String, Double> diffs = new HashMap<String, Double>();
		for (Entry<String, Map<String, Double>> entry : statistics.entrySet()) {
			Double difference = Math.abs(entry.getValue().get(attribute + "_before")
					- entry.getValue().get(attribute + "_after"));
			diffs.put(entry.getKey(), difference);
		}

		// Print the top 5
		List<String> output = new ArrayList<String>();
		Set<Double> keys = new TreeSet<Double>();
		keys.addAll(diffs.values());
		for (Double key : keys)
			for (Entry<String, Double> entry : diffs.entrySet())
				if (entry.getValue().equals(key))
					output.add(entry.getValue() + " " + entry.getKey());
		for (String out : output.subList(output.size() - 10, output.size()))
			System.out.println(out);

	}
}
