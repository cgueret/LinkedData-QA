/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Map.Entry;

import org.apache.commons.math.distribution.NormalDistributionImpl;

import com.hp.hpl.jena.rdf.model.Resource;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Distribution.Axis;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * Metric measuring the popularity of the nodes. Assuming the presence of highly
 * popular resources and a community effect around them, the observed
 * distribution should be a (mixture of) Gaussian
 * 
 * Target distribution: Gaussian
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
// http://stackoverflow.com/questions/153724/how-to-round-a-number-to-n-decimal-places-in-java
public class Popularity implements Metric {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdealDistribution(nl.vu.
	 * qa_for_lod.data.Distribution)
	 */
	public double getDistanceToIdealDistribution(Distribution distribution) {
		// Normalise the distribution to get probabilities instead of node count
		distribution.normalize();

		// Create the ideal distribution
		double sd = Math.max(distribution.standardDeviation(Axis.Y), 0.000001);
		double mean = distribution.mean(Axis.Y);
		NormalDistributionImpl t = new NormalDistributionImpl(mean, sd);

		// Measure the distance
		double d = 0;
		for (Entry<Double, Double> point : distribution.entrySet())
			d += Math.abs(t.density(point.getKey().doubleValue()) - point.getValue());

		return d;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "Popularity";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
		return graph.getPopularity(resource);
	}
}
