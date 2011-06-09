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
 * Compute the local clustering coefficient of the nodes
 * 
 * Target is 1 on average
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class ClusteringCoefficient implements Metric {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdeal(nl.vu.qa_for_lod.metrics
	 * .Distribution)
	 */
	public double getDistanceToIdeal(Distribution inputDistribution) {
		// Get the average clustering coefficient
		double average = inputDistribution.getAverage();

		// We want to reach 1, return the distance with that value
		return 1.0d - average;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "Clustering coefficient";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	// http://en.wikipedia.org/wiki/Clustering_coefficient
	public double getResult(Graph graph, Resource resource) {
		// Get all the neighbours, independently of the binding relation
		Set<Resource> neighbours = graph.getNeighbours(resource, Direction.BOTH, null);

		double c = 0;
		if (neighbours.size() > 1) {
			for (Resource neighbourA : neighbours)
				for (Resource neighbourB : neighbours)
					if (!neighbourA.equals(neighbourB) && graph.containsEdge(neighbourA, neighbourB, true))
						c++;
			c = c / (neighbours.size() * (neighbours.size() - 1.0d));
		}

		return c;
	}

}
