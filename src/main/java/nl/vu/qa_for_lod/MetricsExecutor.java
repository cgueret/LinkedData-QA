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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.impl.FileDataProvider;
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
	private JFrame frame;
	private final DataProvider extraTriples;
	private final Map<Metric, MetricData> metricsData = new HashMap<Metric, MetricData>();
	private final List<Resource> resourceQueue = new ArrayList<Resource>();
	private final DataProvider dataFetcher;

	/**
	 * @param dataFetcher
	 *            the data provider used to get the description of the resources
	 * @param extraTriples
	 *            the data provider serving the extra set of triples for the
	 *            comparative analysis
	 */
	public MetricsExecutor(DataProvider dataFetcher, FileDataProvider extraTriples) {
		this.dataFetcher = dataFetcher;
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
	public void processQueue(boolean withGUI) throws Exception {
		logger.info("Start processing " + resourceQueue.size() + " resources");

		// Create an executor service
		ExecutorService executorService = Executors.newFixedThreadPool(6);

		// Init the GUI if needed
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
		List<Future<?>> futures = new ArrayList<Future<?>>();
		for (Resource resource : resourceQueue) {
			MetricsTask task = new MetricsTask(this, resource, dataFetcher, extraTriples);
			Future<?> future = executorService.submit(task);
			futures.add(future);
		}

		// Wait for all the tasks to be completed
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
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

		logger.info("Done!");



		// Hide the progress bar
		if (withGUI) {
			frame.setVisible(false);
		}

		// Shutdown the executor
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
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