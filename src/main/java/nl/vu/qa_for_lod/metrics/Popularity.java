/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.data.Results;

/**
 * Metric measuring the popularity of the nodes. Assuming the presence of highly
 * popular resources and a community effect around them, the observed
 * distribution should be a (mixture of) Gaussian
 * 
 * Target distribution: Gaussian
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
// http://stackoverflow.com/questions/153724/how-to-round-a-number-to-n-decimal-places-in-java
public class Popularity extends Metric {
	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#go(nl.vu.qa_for_lod.data.Results)
	 */
	@Override
	protected void go(Graph graph, Results results, MetricState state) {
		graph.getNodesPopularity(results);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	@Override
	public String getName() {
		return "popularity";
	}

}
