/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import java.util.Map.Entry;
import java.util.TreeSet;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.data.Results;

/**
 * Metric computing the centrality of the nodes in the network. The global goal
 * is to have a distribution as flat as possible to limit the potential impact
 * of an individual node failure.
 * 
 * Target distribution: flat
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
public class Centrality extends Metric {
	double centralityIndexBefore = 0;

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#go(nl.vu.qa_for_lod.data.Results)
	 */
	@Override
	protected void go(Graph graph, Results results, MetricState state) {
		// Compute the centralities
		Results centralities = graph.getNodesCentrality();
		results.putAll(centralities);

		// Compute the centrality index
		double c = 0;
		Double max = (new TreeSet<Double>(centralities.values())).last();
		for (Entry<String, Double> centrality : centralities.entrySet())
			c += max - centrality.getValue();
		c = c / (centralities.size() - 1);

		// We want to minimize the centrality index
		if (state.equals(MetricState.BEFORE)) {
			centralityIndexBefore = c;
		} else {
			isGreen = (c < centralityIndexBefore);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	@Override
	public String getName() {
		return "centrality";
	}

}
