/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.Graph;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdealDistribution(nl.vu.
	 * qa_for_lod.metrics.Distribution)
	 */
	public double getDistanceToIdealDistribution(Distribution distribution) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#isApplicableFor(nl.vu.qa_for_lod.Graph,
	 * java.util.Collection)
	 */
	public boolean isApplicableFor(Graph graph, Collection<Resource> resources) {
		// TODO Auto-generated method stub
		return false;
	}

}
