/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.Results;

/**
 * Metric computing the degree distribution in the network. Having a power-law
 * distribution would imply a better scalability of the network along with the
 * presence of hubs.
 * 
 * Target distribution: power-law
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
public class Degree implements Metric {
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
		return "degree";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResults(nl.vu.qa_for_lod.Graph)
	 */
	public Results getResults(Graph graph) {
		return graph.getNodesDegree();
	}

}
