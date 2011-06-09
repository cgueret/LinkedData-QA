/**
 * 
 */
package nl.vu.qa_for_lod;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.impl.Any23DataProvider;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricData;
import nl.vu.qa_for_lod.metrics.MetricState;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://wiki.gephi.org/index.php/Toolkit_portal
public class MetricsExecutor {
	class MetricSorter implements Comparator<Metric> {
		public int compare(Metric o1, Metric o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	static Logger logger = LoggerFactory.getLogger(MetricsExecutor.class);
	private JProgressBar bar;
	private final DataProvider extraTriples;
	private JFrame frame;
	private final Map<Metric, MetricData> metricsData = new HashMap<Metric, MetricData>();

	private final List<Resource> resourceQueue = new ArrayList<Resource>();

	/**
	 * @param extraTriples
	 */
	public MetricsExecutor(DataProvider extraTriples) {
		this.extraTriples = extraTriples;
	}

	/**
	 * Adds a new metric to the analysis pipeline
	 * 
	 * @param metric
	 *            the metric to be added
	 */
	public void addMetric(Metric metric) {
		metricsData.put(metric, new MetricData());
		logger.info("Registered new metric: " + metric.getName());
	}

	/**
	 * @param resource
	 */
	public void addToResourcesQueue(Resource resource) {
		resourceQueue.add(resource);
	}

	/*
	 * public void incrementBar() { synchronized (bar) { int v = bar.getValue();
	 * bar.setValue(v + 1); bar.invalidate(); } }
	 */

	/**
	 * @return
	 */
	public MetricData getMetricData(Metric metric) {
		return metricsData.get(metric);
	}

	/**
	 * @return
	 */
	public Set<Metric> getMetrics() {
		Set<Metric> sortedKeys = new TreeSet<Metric>(new MetricSorter());
		sortedKeys.addAll(metricsData.keySet());
		return sortedKeys;
	}

	/**
	 * @throws Exception
	 * 
	 */
	public void processQueue(boolean withGUI, String cacheDir) throws Exception {
		logger.info("Start processing " + resourceQueue.size() + " resources");

		// Create a data fetcher to de-reference resources
		DataProvider dataFetcher = new Any23DataProvider(cacheDir);

		if (withGUI) {
			frame = new JFrame("Progress");
			frame.setResizable(false);
			frame.setPreferredSize(new Dimension(500, 32));

			bar = new JProgressBar(0, resourceQueue.size());
			bar.setStringPainted(true);
			frame.getContentPane().add(bar);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		}

		// Do the processing
		// FIXME The parallel code was not more efficient :(
		for (Resource resource : resourceQueue) {
			MetricsTask task = new MetricsTask(this, resource, dataFetcher, extraTriples);
			task.run();
			if (withGUI) {
				bar.setValue(bar.getValue() + 1);
				bar.invalidate();
			}
		}

		// Remove all metrics that returned no result
		Set<Metric> notApplicable = new HashSet<Metric>();
		for (Metric metric : this.getMetrics()) {
			MetricData data = metricsData.get(metric);
			if (data.getDistribution(MetricState.BEFORE).equals(data.getDistribution(MetricState.AFTER)))
					notApplicable.add(metric);
		}
		for (Metric metric : notApplicable)
			metricsData.remove(metric);
		
		// Do the post processing
		logger.info("Start post-processing");
		for (Metric metric : this.getMetrics()) {
			MetricData data = metricsData.get(metric);
			for (MetricState state : MetricState.values()) {
				// Ask the metric the distance to the ideal value
				Distribution observedDistribution = data.getDistribution(state);
				double dist = metric.getDistanceToIdeal(observedDistribution);
				data.setDistanceToIdeal(state, dist);
			}
		}

		if (withGUI) {
			frame.setVisible(false);
		}

		logger.info("Done!");
	}

	public int queueSize() {
		return resourceQueue.size();
	}
}

/*
 * // Do the processing ExecutorService executor =
 * Executors.newFixedThreadPool(8); List<Future<?>> handles = new
 * ArrayList<Future<?>>(); for (Resource resource : resourceQueue) { MetricsTask
 * task = new MetricsTask(this, resource, dataFetcher, extraTriples);
 * handles.add(executor.submit(task)); } for (Future<?> handle : handles)
 * handle.get();
 * 
 * // We won't fetch data anymore dataFetcher.close();
 * 
 * // Finish the executor executor.shutdown(); executor.awaitTermination(1,
 * TimeUnit.SECONDS);
 */