/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Collection;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.misc.PolynomialFitter;
import nl.vu.qa_for_lod.misc.PolynomialFitter.Polynomial;

/**
 * Metric computing the degree distribution in the network. Having a power-law
 * distribution would imply a better scalability of the network along with the
 * presence of hubs.
 * 
 * Target distribution: power-law
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
public class Degree implements Metric {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdealDistribution(nl.vu.
	 * qa_for_lod.data.Distribution)
	 */
	public double getDistanceToIdealDistribution(Distribution distribution) {
		// Try to fix a linear equation in log/log
		PolynomialFitter poly = new PolynomialFitter(1);
		for (Entry<Double, Double> point : distribution.entrySet())
			if (point.getKey() != 0 && point.getValue() != 0)
				poly.addPoint(Math.log(point.getKey()), Math.log(point.getValue()));
		Polynomial p = poly.getBestFit();

		// Measure the distance to that line
		double d = 0;
		for (Entry<Double, Double> point : distribution.entrySet())
			if (point.getKey() != 0 && point.getValue() != 0)
				d += Math.abs(p.getY(Math.log(point.getKey())) - Math.log(point.getValue()));

		return d;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "Degree";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
		return graph.getDegree(resource);
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

}
