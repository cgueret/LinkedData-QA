/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.Map.Entry;

import org.apache.commons.math.distribution.NormalDistributionImpl;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Distribution.Axis;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.Results;

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
		double sd = distribution.standardDeviation(Axis.Y);
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
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResults(nl.vu.qa_for_lod.Graph)
	 */
	public Results getResults(Graph graph) {
		return graph.getNodesPopularity();
	}
}
