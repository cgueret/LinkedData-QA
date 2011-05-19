/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.Results;

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
public class Popularity implements Metric {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdealDistribution(nl.vu.
	 * qa_for_lod.data.Distribution)
	 */
	public double getDistanceToIdealDistribution(Distribution distribution) {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "popularity";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResults(nl.vu.qa_for_lod.Graph)
	 */
	public Results getResults(Graph graph) {
		return graph.getNodesPopularity();
	}

}
