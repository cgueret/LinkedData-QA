/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Collection;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * Compute the local clustering coefficient of the nodes
 * 
 * Target distribution: flat line at 1
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class ClusteringCoefficient implements Metric {

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
		Set<Resource> neighbours = graph.getNeighbours(resource);

		double c = 0;
		for (Resource neighbourA : neighbours)
			for (Resource neighbourB : neighbours)
				if (!neighbourA.equals(neighbourB) && graph.containsEdge(neighbourA, neighbourB))
					c++;
		c = c / (neighbours.size() * (neighbours.size() - 1.0d));

		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#isApplicableFor(nl.vu.qa_for_lod.Graph,
	 * java.util.Collection)
	 */
	public boolean isApplicableFor(Graph graph, Collection<Resource> resources) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getIdealDistribution(nl.vu.qa_for_lod
	 * .metrics.Distribution)
	 */
	public Distribution getIdealDistribution(Distribution inputDistribution) {
		// We want 0 for all the keys in the input distribution and the number
		// of nodes for a clustering coefficient of 1
		Distribution result = new Distribution();
		double total = 0;
		for (Double key : inputDistribution.keySet()) 
			total += inputDistribution.get(key);
		
		result.set(1, total);

		return result;
	}

}
