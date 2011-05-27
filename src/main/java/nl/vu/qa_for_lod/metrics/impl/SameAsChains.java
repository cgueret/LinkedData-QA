/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.graph.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * Look at the sameAs chains starting from a node. Every such a chain should be
 * closed on its starting point, in that case its length is the result.
 * Sub-chains of a chain are considered in the result computed
 * 
 * Target distribution: Linear (?) - every chain of length n+1 implies a chain
 * of length n
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class SameAsChains implements Metric {
	private final static String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "SameAs chains";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
		// Return the level of anomalies

		double ano = 0;
		List<Resource> neighbours = new ArrayList<Resource>(graph.getNeighbours(resource, SAME_AS));
		if (neighbours.size() > 1) {
			for (Resource startingPoint : neighbours) {
				Set<Resource> explored = new HashSet<Resource>();
				explored.add(resource);
				Set<Resource> targets = new HashSet<Resource>(neighbours);
				targets.remove(startingPoint);
				LinkedList<Resource> queue = new LinkedList<Resource>();
				queue.add(startingPoint);
				boolean found = false;
				while (!found && queue.size() != 0) {
					Resource visiting = queue.poll();
					explored.add(visiting);
					for (Resource next : graph.getNeighbours(visiting, SAME_AS)) {
						found = targets.contains(next);
						if (!explored.contains(next))
							queue.add(next);
					}
				}
				if (!found)
					ano++;
			}
		}

		return ano;
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
		// We should have no incomplete sameAs chains
		Distribution result = new Distribution();
		double total = 0;
		for (Double key : inputDistribution.keySet())
			total += inputDistribution.get(key);

		result.set(0, total);
		return result;
	}

}
