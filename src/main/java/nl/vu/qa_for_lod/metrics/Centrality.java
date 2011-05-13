/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.data.Results;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 *
 */
public class Centrality extends Metric {

	/* (non-Javadoc)
	 * @see nl.vu.qa_for_lod.metrics.Metric#go(nl.vu.qa_for_lod.data.Results)
	 */
	@Override
	protected void go(Graph graph, Results results) {
		graph.getNodesCentrality(results);
	}

	/* (non-Javadoc)
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	@Override
	public String getName() {
		return "centrality";
	}

}
