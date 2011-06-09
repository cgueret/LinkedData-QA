/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.graph.Direction;
import nl.vu.qa_for_lod.graph.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Distribution.DistributionAxis;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Centrality implements Metric {
	static final Logger logger = LoggerFactory.getLogger(Centrality.class);
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdeal(nl.vu.qa_for_lod.metrics
	 * .Distribution)
	 */
	public double getDistanceToIdeal(Distribution inputDistribution) {
		// Get the highest centrality result found
		double max = inputDistribution.max(DistributionAxis.X);

		// Compute the centrality index
		double index = 0;
		double size = 0;
		for (Entry<Double, Double> entry : inputDistribution.entrySet()) {
			size += entry.getValue();
			index += entry.getValue() * (max - entry.getKey());
		}

		return (size > 1 ? index / (size - 1) : 0);
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
