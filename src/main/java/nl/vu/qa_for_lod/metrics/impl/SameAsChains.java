/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	static Logger logger = LoggerFactory.getLogger(SameAsChains.class);
	private final static String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "Open sameAs chains";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
		// Search for the paths
		Set<List<Resource>> paths = new HashSet<List<Resource>>();
		List<Resource> path = new ArrayList<Resource>();
		path.add(resource);
		getPaths(graph, paths, path);

		// Count the open ones
		double open = 0;
		if (paths.size() > 0)
			for (List<Resource> p : paths)
				if (p.size() > 2 && !p.get(p.size() - 1).equals(resource))
					open++;

		return open;
	}

	/**
	 * Recursive search of paths
	 * 
	 * @param graph
	 *            the graph to use for the input
	 * @param currentPath
	 *            the current path explored
	 * @param paths
	 *            a set of paths currently found
	 */
	private void getPaths(Graph graph, Set<List<Resource>> paths, List<Resource> currentPath) {
		Resource currentNode = currentPath.get(currentPath.size() - 1);

		Set<Resource> nextNodes = graph.getNeighbours(currentNode, SAME_AS);
		if (nextNodes.isEmpty()) {
			List<Resource> newPath = new ArrayList<Resource>();
			newPath.addAll(currentPath);
			paths.add(newPath);
		} else {
			for (Resource nextNode : nextNodes) {
				if (!currentPath.contains(nextNode)) {
					List<Resource> newPath = new ArrayList<Resource>();
					newPath.addAll(currentPath);
					newPath.add(nextNode);
					getPaths(graph, paths, newPath);
				} else {
					List<Resource> newPath = new ArrayList<Resource>();
					newPath.addAll(currentPath);
					newPath.add(nextNode);
					paths.add(newPath);
				}
			}
		}
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
