/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.HashSet;
import java.util.Set;

import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.Graph;
import nl.vu.qa_for_lod.graph.impl.JenaGraph;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.report.MetricState;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class MetricsTask implements Runnable {
	private final Resource resource;
	private final DataProvider dataFetcher;
	private final MetricsExecutor metricsExecutor;
	private final DataProvider extraTriples;

	/**
	 * @param resource
	 */
	public MetricsTask(MetricsExecutor metricsExecutor, Resource resource, DataProvider dataFetcher, DataProvider extraTriples) {
		this.resource = resource;
		this.dataFetcher = dataFetcher;
		this.metricsExecutor = metricsExecutor;
		this.extraTriples = extraTriples;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// Create a graph
		Graph graph = new JenaGraph();

		// Set used to track the expansion of the graph
		Set<Resource> dereferencedResources = new HashSet<Resource>();

		try {
			// Add the resource and expand it
			for (Statement statement : dataFetcher.get(resource)) {
				// Add the statement
				graph.addStatement(statement);

				// Expand the other end of the statement
				Resource other = (statement.getSubject().equals(resource) ? statement.getObject().asResource()
						: statement.getSubject());
				if (!dereferencedResources.contains(other)) {
					for (Statement otherStatement : dataFetcher.get(other))
						graph.addStatement(otherStatement);
					dereferencedResources.add(other);
				}
			}
			dereferencedResources.add(resource);

			// Execute the metrics
			for (Metric metric : metricsExecutor.getMetrics())
				metricsExecutor.getMetricData(metric).setResult(MetricState.BEFORE, resource,
						metric.getResult(graph, resource));

			// Add the statements from the extraLinks file
			for (Statement statement : extraTriples.get(resource)) {
				// Add the statement
				graph.addStatement(statement);

				// Expand the other end of the statement
				Resource other = (statement.getSubject().equals(resource) ? statement.getObject().asResource()
						: statement.getSubject());
				if (!dereferencedResources.contains(other)) {
					for (Statement otherStatement : dataFetcher.get(other))
						graph.addStatement(otherStatement);
					dereferencedResources.add(other);
				}
			}

			// Re-execute the metrics
			for (Metric metric : metricsExecutor.getMetrics())
				metricsExecutor.getMetricData(metric).setResult(MetricState.AFTER, resource,
						metric.getResult(graph, resource));
		} catch (NotFoundException e) {
			// Just skip resources that don't work
		} finally {
			metricsExecutor.incrementBar();
		}
	}

}
