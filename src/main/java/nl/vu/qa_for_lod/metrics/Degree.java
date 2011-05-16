/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import java.util.HashMap;
import java.util.Map;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.data.Results;

/**
 * Metric computing the degree distribution in the network. Having a power-law
 * distribution would imply a better scalability of the network along with the
 * presence of hubs.
 * 
 * Target distribution: power-law
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 */
public class Degree extends Metric {
	private static final Map<MetricState, Double> distanceToPowerLaw = new HashMap<MetricState, Double>(2, 1.0f);

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#go(nl.vu.qa_for_lod.Graph,
	 * nl.vu.qa_for_lod.data.Results)
	 */
	@Override
	protected void go(Graph graph, Results results, MetricState state) {
		// Save node degrees
		graph.getNodesDegree(results);

		// Compute distance to 2.5 (power law as a factor between 2 and 3)
		double d = graph.getDegreeDistributionPowerLawFactor();
		distanceToPowerLaw.put(state, Math.abs(d - 2.5));

		// If the status is after, update isGreen accordingly
		if (state.equals(MetricState.AFTER)) {
			isGreen = (distanceToPowerLaw.get(MetricState.AFTER) < distanceToPowerLaw.get(MetricState.BEFORE));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	@Override
	public String getName() {
		return "degree";
	}

}
