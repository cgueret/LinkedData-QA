/**
 * 
 */
package nl.vu.qa_for_lod;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.BarChartPlot;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Plots;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.NotFoundException;

import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Distribution.Axis;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricData;
import nl.vu.qa_for_lod.report.HallOfFame;
import nl.vu.qa_for_lod.report.MetricState;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://wiki.gephi.org/index.php/Toolkit_portal
public class MetricsExecutor {
	private final static DecimalFormat df = new DecimalFormat("###.##");
	protected static Logger logger = LoggerFactory.getLogger(MetricsExecutor.class);
	private final Graph graph = new Graph();
	private final Map<Metric, MetricData> metricsData = new HashMap<Metric, MetricData>();
	private final List<Resource> resourceQueue = new ArrayList<Resource>();
	private final ExtraLinks seedFile;

	/**
	 * @param seedFile
	 */
	public MetricsExecutor(ExtraLinks seedFile) {
		this.seedFile = seedFile;
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
	 * @return
	 */
	private Collection<Metric> applicableMetrics() {
		List<Metric> tmp = new ArrayList<Metric>();
		for (Metric m : metricsData.keySet())
			if (m.isApplicableFor(graph, resourceQueue))
				tmp.add(m);
		return tmp;
	}

	/**
	 * 
	 */
	public void printReport() {
		// Get the list of metrics to execute
		Set<Metric> metrics = metricsData.keySet();

		System.out.println("\n");
		System.out.println("Metric statuses");
		System.out.println("---------------");
		for (Entry<Metric, MetricData> entry : metricsData.entrySet()) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(entry.getKey().getName()).append(" => ");
			buffer.append(entry.getValue().isGreen() ? "green " : "red ").append("(");
			buffer.append("new distance = ");
			buffer.append(df.format(100 - entry.getValue().getRatioDistanceChange()));
			buffer.append(" % improvement)");
			System.out.println(buffer.toString());
		}
		System.out.println("");

		// Compute a ranking of the most suspicious nodes
		System.out.println("Top suspicious nodes");
		System.out.println("--------------------");
		HallOfFame haf = new HallOfFame(10);
		for (Metric metric : metrics)
			haf.insert(metricsData.get(metric).getSuspiciousNodes(haf.getSize(), seedFile.getResources()));
		haf.print();

		// Save the distribution table
		/*
		 * Distributions distributions = new Distributions(); for (Metric metric
		 * : metrics) {
		 * distributions.insert(metric.getDistribution(MetricState.BEFORE),
		 * metric.getName() + "_1");
		 * distributions.insert(metric.getDistribution(MetricState.AFTER),
		 * metric.getName() + "_2"); }
		 * distributions.write("/tmp/distributions.dat");
		 * 
		 * for (Metric metric : metrics)
		 * metric.getDistribution().writeToFile("/tmp/distribution-" +
		 * metric.getName() + ".dat");
		 */

	}

	/**
	 * @param state
	 */
	public void processMetric(Graph graph, Resource resource, Metric metric, MetricState state) {
		// Execute the metric
		// logger.info("Execute metric \"" + metric.getName() + "\"");
		double v = metric.getResult(graph, resource);

		// Save the result
		MetricData data = metricsData.get(metric);
		data.setResult(state, resource, v);
	}

	/**
	 * @throws Exception
	 * 
	 */
	// TODO Parallelise this
	public void processQueue() throws Exception {
		logger.info("Process resources queue");

		// Clear all previous results
		for (Metric metric : metricsData.keySet())
			metricsData.get(metric).clear();

		// Execute the metrics
		for (Resource resource : resourceQueue) {
			try {
				// Create the initial graph and run the metrics
				graph.clear();
				graph.loadFromResource(resource);
				for (Metric metric : applicableMetrics())
					processMetric(graph, resource, metric, MetricState.BEFORE);

				// Create the graph with the new links and re-run the metrics
				graph.clear();
				graph.addStatements(seedFile.getStatements(resource));
				graph.loadFromResource(resource);
				for (Metric metric : applicableMetrics())
					processMetric(graph, resource, metric, MetricState.AFTER);
			} catch (NotFoundException e) {
				// Just skip resources that don't work
			}
		}

		// Do the post processing
		for (Metric metric : applicableMetrics()) {
			MetricData data = metricsData.get(metric);
			for (MetricState state : MetricState.values()) {
				// Get the distributions
				Distribution observedDistribution = data.getDistribution(state);
				Distribution idealDistribution = metric.getIdealDistribution(observedDistribution);

				// Ask the metric the distance to the ideal value
				data.setDistanceToIdeal(state, observedDistribution.distanceTo(idealDistribution));

				if (state.equals(MetricState.AFTER)) {
					// Plot the distributions
					// observedDistribution.normalize();
					// idealDistribution.normalize();
					Set<Double> keys = new TreeSet<Double>();
					keys.addAll(observedDistribution.keySet());
					keys.addAll(idealDistribution.keySet());
					List<BarChartPlot> l = new ArrayList<BarChartPlot>();
					for (Double key : keys) {
						Data d = Data.newData(observedDistribution.get(key), idealDistribution.get(key));
						BarChartPlot plot = Plots.newBarChartPlot(d, Color.BLUE, key.toString());
						l.add(plot);
					}
					BarChart chart = GCharts.newBarChart(l);
					AxisLabels count = AxisLabelsFactory.newNumericRangeAxisLabels(0,
							Math.max(observedDistribution.max(Axis.Y), idealDistribution.max(Axis.Y)));
					count.setAxisStyle(AxisStyle.newAxisStyle(Color.BLACK, 13, AxisTextAlignment.CENTER));
					chart.addYAxisLabels(count);
					chart.setSize(500, 200);
					chart.setSpaceBetweenGroupsOfBars(30);
					System.out.println(chart.toURLString());
				}
			}
		}
	}
}
