/**
 * 
 */
package nl.vu.qa_for_lod;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;

import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.Graph;
import nl.vu.qa_for_lod.graph.impl.RDFDataProvider;
import nl.vu.qa_for_lod.graph.impl.JenaGraph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricData;
import nl.vu.qa_for_lod.report.MetricState;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://wiki.gephi.org/index.php/Toolkit_portal
public class MetricsExecutor {
	protected static Logger logger = LoggerFactory.getLogger(MetricsExecutor.class);
	private final Map<Metric, MetricData> metricsData = new HashMap<Metric, MetricData>();
	private final List<Resource> resourceQueue = new ArrayList<Resource>();
	private final DataProvider extraTriples;

	/**
	 * @param extraTriples
	 */
	public MetricsExecutor(DataProvider extraTriples) {
		this.extraTriples = extraTriples;
	}

	/**
	 * @param metric
	 */
	public void addMetric(Metric metric) {
		metricsData.put(metric, new MetricData());
	}

	/**
	 * @param resource
	 */
	public void addToResourcesQueue(Resource resource) {
		resourceQueue.add(resource);
	}

	/**
	 * @throws Exception
	 * 
	 */
	// TODO Parallelise this
	public void processQueue() throws Exception {
		JFrame frame = new JFrame("Progress");
		frame.setResizable(false);
		frame.setPreferredSize(new Dimension(500, 32));

		JProgressBar bar = new JProgressBar(0, resourceQueue.size());
		bar.setStringPainted(true);
		frame.getContentPane().add(bar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		logger.info("Process " + resourceQueue.size() + " resources in queue");

		// Create a new empty graph
		Graph graph = new JenaGraph();

		// Create a data fetcher to de-reference resources
		RDFDataProvider dataFetcher = new RDFDataProvider();

		// Set used to track the expansion of the graph
		Set<Resource> dereferencedResources = new HashSet<Resource>();

		// Get executable metrics
		List<Metric> executableMetrics = new ArrayList<Metric>();
		for (Metric metric : metricsData.keySet()) {
			if (metric.isApplicableFor(graph, resourceQueue)) {
				executableMetrics.add(metric);
				metricsData.get(metric).clear();
			}
		}

		int index = 0;
		for (Resource resource : resourceQueue) {
			bar.setValue(index);
			bar.setString(resource.toString());
			try {
				// Clear the current graph and expansion tracker
				graph.clear();
				dereferencedResources.clear();

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
				for (Metric metric : executableMetrics)
					metricsData.get(metric).setResult(MetricState.BEFORE, resource, metric.getResult(graph, resource));

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
				for (Metric metric : executableMetrics)
					metricsData.get(metric).setResult(MetricState.AFTER, resource, metric.getResult(graph, resource));
			} catch (NotFoundException e) {
				// Just skip resources that don't work
			}
			index++;
		}

		// Do the post processing
		for (Metric metric : executableMetrics) {
			MetricData data = metricsData.get(metric);
			for (MetricState state : MetricState.values()) {
				// Get the distributions
				Distribution observedDistribution = data.getDistribution(state);
				Distribution idealDistribution = metric.getIdealDistribution(observedDistribution);

				// Ask the metric the distance to the ideal value
				data.setDistanceToIdeal(state, observedDistribution.distanceTo(idealDistribution));
			}
		}

		dataFetcher.close();

		frame.setVisible(false);
	}

	/**
	 * @return
	 */
	public Set<Entry<Metric, MetricData>> metricsData() {
		return metricsData.entrySet();
	}
}
