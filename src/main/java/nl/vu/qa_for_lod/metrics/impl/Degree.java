/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.graph.Graph;
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
	static final Logger logger = LoggerFactory.getLogger(Degree.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdeal(nl.vu.qa_for_lod.metrics
	 * .Distribution)
	 */
	public double getDistanceToIdeal(Distribution inputDistribution) {
		// Try to fix a linear equation in log/log
		PolynomialFitter poly = new PolynomialFitter(1);
		for (Entry<Double, Double> point : inputDistribution.entrySet())
			if (point.getKey() != 0 && point.getValue() != 0)
				poly.addPoint(Math.log(point.getKey()), Math.log(point.getValue()));
		Polynomial p = poly.getBestFit();

		// Use that log/log equation to generate the non-log/log ideal
		// distribution
		Distribution idealDistribution = new Distribution();
		for (Double key : inputDistribution.keySet())
			idealDistribution.set(key, (key.equals(0.0d) ? 0d : Math.exp(p.getY(Math.log(key)))));

		// Return the distance between the two curves
		return inputDistribution.distanceTo(idealDistribution);
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

}
