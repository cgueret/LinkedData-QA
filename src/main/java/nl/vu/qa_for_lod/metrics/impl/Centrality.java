/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.graph.Direction;
import nl.vu.qa_for_lod.graph.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Centrality implements Metric {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdeal(nl.vu.qa_for_lod.metrics
	 * .Distribution)
	 */
	public double getDistanceToIdeal(Distribution inputDistribution) {

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "Centrality";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.graph.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
		double in = 0;
		for (Resource node : graph.getNeighbours(resource, Direction.IN, null))
			in += Math.max(1, graph.getNeighbours(node, Direction.IN, null).size());

		double out = 0;
		for (Resource node : graph.getNeighbours(resource, Direction.OUT, null))
			out += Math.max(1, graph.getNeighbours(node, Direction.OUT, null).size());

		return (in > 0 ? out / in : 0);
	}

}
