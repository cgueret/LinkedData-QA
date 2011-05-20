/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * Metric computing the centrality of the nodes in the network. The global goal
 * is to have a distribution as flat as possible to limit the potential impact
 * of an individual node failure.
 * 
 * Target distribution: flat
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
public class Centrality implements Metric {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdealDistribution(nl.vu.
	 * qa_for_lod.data.Distribution)
	 */
	public double getDistanceToIdealDistribution(Distribution distribution) {
		// Compute the centrality index as the distance to the ideal
		double c = 0;
		
		Double max = distribution.max(Distribution.Axis.Y);
		for (Entry<Double, Double> centrality : distribution.entrySet())
			c += max - centrality.getValue();
		if (distribution.size() > 1)
			c = c / (distribution.size() - 1);

		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "Centrality";
	}

	/* (non-Javadoc)
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph, com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
		return graph.getCentrality(resource);
	}

}
