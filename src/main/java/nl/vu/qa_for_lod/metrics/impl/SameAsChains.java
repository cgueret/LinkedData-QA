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
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
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
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getIdealDistribution(nl.vu.qa_for_lod
	 * .metrics.Distribution)
	 */
	public Distribution getIdealDistribution(Distribution distribution) {
		return null;
	}

}
