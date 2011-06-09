/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.graph.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * Look at the consistency of the description of nodes across the network. There
 * should be a consistent usage of properties for nodes having the same type.
 * 
 * The metric is to be computed on a set of node sharing the same type (!)
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class DescriptionConsistency implements Metric {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdeal(nl.vu.qa_for_lod.metrics
	 * .Distribution)
	 */
	public double getDistanceToIdeal(Distribution inputDistribution) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
		// TODO Auto-generated method stub
		return 0;
	}
}
