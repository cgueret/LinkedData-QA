/**
 * 
 */
package nl.vu.qa_for_lod.report;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import nl.vu.qa_for_lod.MetricsExecutor;
import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Distribution.DistributionAxis;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricData;
import nl.vu.qa_for_lod.metrics.MetricState;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.BarChartPlot;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.DataEncoding;
import com.googlecode.charts4j.DataUtil;
import com.googlecode.charts4j.GChart;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.Plots;
import com.googlecode.charts4j.Shape;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


class NumberComparator implements Comparator<Number> {
	@Override
	public int compare(Number a, Number b) {
		return Double.compare(a.doubleValue(), b.doubleValue());
	}
}

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 *
 * 
 * Actually, this XML format is quite convenient to use with XSLT for 
 * further processing, although maybe it should actually
 * be XML rather than HTML - so that the HTML is generated
 * with an XSLT stylesheet. 
 * So my point is, that RDF or JSon are not so great for processing with Java -
 * RDF somewhat sucks because of the need to assign URIs, and JSON because
 * of the weak query + transformation language support ~Claus
 *  
 * 
 */
public class HTMLReport {
	static Logger logger = LoggerFactory.getLogger(HTMLReport.class);
	private static final int HAF_SIZE = 100;

	public static HTMLReport createReport(String datasetName,
			MetricsExecutor executor, DataProvider extraLinks) {
		return createReport(datasetName, executor, extraLinks,
				new HashMap<String, Model>());
	}

	/**
	 * @param datasetName
	 * @param executor
	 * @param extraLinks
	 * @return
	 */
	public static HTMLReport createReport(String datasetName,
			MetricsExecutor executor, DataProvider extraLinks,
			Map<String, Model> coloredModels) {
		HTMLReport report = new HTMLReport(datasetName);
		report.appendMetricStatuses(executor);
		report.appendDistributions(executor);
		report.appendHallOfFame(executor, coloredModels);
		report.close();
		return report;
	}

	private final StringBuffer buffer = new StringBuffer();

	private final DecimalFormat df = new DecimalFormat("###.##");

	/**
	 * @param datasetName
	 */
	public HTMLReport(String datasetName) {
		buffer.append("<!DOCTYPE HTML><html><head><title>Execution report for ");
		buffer.append(datasetName).append("</title>");
		buffer.append("<style type=\"text/css\">");
		buffer.append("body {color: #000000;font-family: Helvetica,Arial,sans-serif;font-size: small;}");
		buffer.append("h1 {background-color: #E5ECF9;border-top: 1px solid #3366CC;font-size: 130%;font-weight: bold;margin: 2em 0 0 -10px;padding: 1px 3px;}");
		buffer.append("td {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 6px 12px;text-align: left;vertical-align: top;}");
		buffer.append("img {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 20px 20px;}");
		buffer.append("</style></head>");
		buffer.append("<body>");
	}

	/**
	 * @param executor
	 */
	private void appendDistributions(MetricsExecutor executor) {
		buffer.append("<h1>Metric distributions</h1>");
		for (Metric metric : executor.getMetrics()) {
			MetricData data = executor.getMetricData(metric);

			// Get the distributions
			Distribution observedDistributionBefore = data
					.getDistribution(MetricState.BEFORE);
			Distribution observedDistributionAfter = data
					.getDistribution(MetricState.AFTER);
			// Distribution idealDistribution =
			// metric.getIdealDistribution(observedDistributionAfter);

			// Normalise the distributions
			// observedDistributionBefore.normalize();
			// observedDistributionAfter.normalize();
			// idealDistribution.normalize();

			// Generate the chart
			try {
				GChart chart = getChart(metric.getName(),
						observedDistributionBefore, observedDistributionAfter);
				buffer.append(
						"<img style=\"float:left;padding:5px;margin:5px\" src=\"")
						.append(chart.toURLForHTML()).append("\"/>");
			} catch (IllegalArgumentException e) {

			}
		}
		buffer.append("<div style=\"clear:both;\"></div>");
	}

	/**
	 * @param executor
	 * @param extraLinks
	 * 
	 */
	private void appendHallOfFame(MetricsExecutor executor,
			Map<String, Model> coloredModels) {

		// Invert the coloredModels map: Map resource to color
		HashMultimap<Resource, String> resourceToColor = HashMultimap.create();

		for (Entry<String, Model> entry : coloredModels.entrySet()) {
			String color = entry.getKey();

			StmtIterator it = entry.getValue().listStatements();
			while (it.hasNext()) {
				Statement stmt = it.next();

				resourceToColor.put(stmt.getSubject(), color);
				resourceToColor.put(stmt.getPredicate(), color);

				if (stmt.getObject().isResource()) {
					resourceToColor.put((Resource) stmt.getObject(), color);
				}
			}

			it.close();
		}

		// Count the number of slots (resources) for each metric
		int maxSlotCount = 0;
		Set<Metric> metrics = executor.getMetrics();
		for (Metric metric : metrics) {
			Map<Resource, Double> changedNodes = executor.getMetricData(metric)
					.getNodeChanges();

			maxSlotCount = Math.max(maxSlotCount, changedNodes.size());
		}

		int slotCount = Math.min(HAF_SIZE, maxSlotCount);

		// Insert the HTML code
		buffer.append("<h1>Outliers - top ").append(slotCount)
				.append(" out of max ").append(maxSlotCount)
				.append(" affected resources per metric</h1>");
		buffer.append("<table><tr>");
		buffer.append("<th>Metric</th>");
		for (int i = 0; i < slotCount; i++)
			buffer.append("<th>Resource (change)</th>");
		buffer.append("</tr>");
		for (Metric metric : metrics) {
			buffer.append("<tr>");
			buffer.append("<td>").append(metric.getName()).append("</td>");
			Map<Resource, Double> changedNodes = executor.getMetricData(metric)
					.getNodeChanges();
			Set<Entry<Resource, Double>> entries = changedNodes.entrySet();
			int count = 0;
			for (Entry<Resource, Double> entry : entries) {

				Resource resource = entry.getKey();
				Set<String> colors = resourceToColor.get(resource);

				String color = "";
				if (!colors.isEmpty()) {
					if (colors.size() > 1) {
						logger.warn("Multiple colors for " + resource + ": "
								+ colors + " - Picking first one.");
					}

					color = " style='background-color:"
							+ colors.iterator().next() + ";'";
				}

				String resourceXml = StringEscapeUtils.escapeXml(entry.getKey().toString());
				
				if (count++ < slotCount) {
					buffer.append("<td" + color + ">").append(resourceXml)
							.append(" (").append(entry.getValue())
							.append(")</td>");
				}
			}
			buffer.append("</tr>");
		}
		buffer.append("</table>");
	}

	/**
	 * @param executor
	 * 
	 */
	private void appendMetricStatuses(MetricsExecutor executor) {
		buffer.append("<h1>Metric statuses</h1>");
		buffer.append("<table><tr>");
		buffer.append("<th>Metric name</th>");
		buffer.append("<th>Status</th>");
		buffer.append("<th>Distance change</th>");
		buffer.append("</tr>");
		for (Metric metric : executor.getMetrics()) {
			MetricData data = executor.getMetricData(metric);
			buffer.append("<tr>");
			buffer.append("<td>").append(metric.getName()).append("</td>");
			buffer.append("<td>").append(data.isGreen() ? "green " : "red ")
					.append("</td>");
			buffer.append("<td>")
					.append(df.format(data.getRatioDistanceChange() - 100))
					.append(" %</td>");
			buffer.append("</tr>");
		}
		buffer.append("</table>");
	}

	/**
	 * 
	 */
	private void close() {
		buffer.append("</body>");
		buffer.append("</html>");
	}

	public static <T> T minElement(Collection<T> collection,
			Comparator<? super T> comparator, boolean invert) {
		// FIXME Check for sorted collections

		T result = null;
		for (T item : collection) {
			if (result == null) {
				result = item;
				continue;
			}

			boolean tmp = comparator.compare(result, item) > 0;
			if (invert) {
				tmp = !tmp;
			}

			if (tmp) {
				result = item;
			}
		}

		return result;
	}

	public static GChart createSimpleBarChart(String title,
			List<Number> data, Integer min, Integer max) {

		if (min == null && !data.isEmpty()) {
			min = minElement(data, new NumberComparator(), false).intValue();
		}

		if (max == null && !data.isEmpty()) {
			max = minElement(data, new NumberComparator(), true).intValue();
		}

		BarChartPlot plot = Plots.newBarChartPlot(DataUtil.scaleWithinRange(min, max, data), Color.BLUE);

		AxisStyle axisStyle = AxisStyle.newAxisStyle(Color.BLACK, 13,
				AxisTextAlignment.CENTER);

		AxisLabels occurrences = AxisLabelsFactory.newNumericRangeAxisLabels(
				min, max);
		occurrences.setAxisStyle(axisStyle);

		int levels = 2;
		List<List<String>> labels = new ArrayList<List<String>>();

		for (int i = 0; i < levels; ++i) {
			List<String> list = new ArrayList<String>();
			labels.add(list);
			for (int j = 0; j < data.size(); ++j) {
				list.add("");
			}
		}

		for (int i = 0; i < data.size() - 1; ++i) {
			int level = i % levels;

			labels.get(level).set(i, i * 10 + "-" + ((i + 1) * 10 - 1));
		}

		labels.get((data.size() - 1) % levels).set(data.size() - 1, ">= 100");

		BarChart result = GCharts.newBarChart(plot);
		result.setDataEncoding(DataEncoding.EXTENDED);
		result.addYAxisLabels(occurrences);

		for (List<String> label : labels) {
			AxisLabels buckets = AxisLabelsFactory.newAxisLabels(label);
			buckets.setAxisStyle(axisStyle);
			result.addXAxisLabels(buckets);
		}

		result.setTitle(title);
		result.setSize(512, 256);

		return result;
	}

	/**
	 * @param name
	 * @param observed
	 * @param ideal
	 */
	private GChart getChart(String name, Distribution observedBefore,
			Distribution observedAfter) throws IllegalArgumentException {
		// Get the list of keys
		TreeSet<Double> keys = new TreeSet<Double>();
		keys.addAll(observedBefore.keySet());
		keys.addAll(observedAfter.keySet());

		// Get the list of values for every key
		List<Double> observedDataBefore = new ArrayList<Double>();
		List<Double> observedDataAfter = new ArrayList<Double>();
		for (Double key : keys) {
			observedDataBefore.add(observedBefore.get(key));
			observedDataAfter.add(observedAfter.get(key));
		}

		// Create the two lines
		Line d1 = Plots.newLine(DataUtil.scale(observedDataBefore), Color.BLUE,
				"before");
		Line d2 = Plots.newLine(DataUtil.scale(observedDataAfter), Color.GREEN,
				"after");
		d1.addShapeMarkers(Shape.CIRCLE, Color.BLUE, 4);
		d2.addShapeMarkers(Shape.CIRCLE, Color.GREEN, 4);

		LineChart chart = GCharts.newLineChart(d1, d2);
		AxisStyle axisStyle = AxisStyle.newAxisStyle(Color.BLACK, 13,
				AxisTextAlignment.CENTER);
		AxisLabels count = AxisLabelsFactory.newNumericRangeAxisLabels(
				0,
				Math.max(observedBefore.max(DistributionAxis.Y),
						observedAfter.max(DistributionAxis.Y)));
		count.setAxisStyle(axisStyle);
		chart.addYAxisLabels(count);
		AxisLabels values = AxisLabelsFactory.newNumericRangeAxisLabels(
				keys.first(), keys.last());
		values.setAxisStyle(axisStyle);
		chart.addXAxisLabels(values);
		chart.setSize(350, 200);
		chart.setTitle(name, Color.BLACK, 16);

		return chart;

	}

	/**
	 * @param uri
	 * @param fileName
	 * @throws IOException
	 */
	protected void saveURIToFile(String uri, String fileName)
			throws IOException {
		URL url = new URL(uri);
		URLConnection urlConnection = url.openConnection();
		InputStream in = new BufferedInputStream(urlConnection.getInputStream());
		byte[] datad = new byte[urlConnection.getContentLength()];
		int bytesRead = 0;
		int offset = 0;
		while (offset < urlConnection.getContentLength()) {
			bytesRead = in.read(datad, offset, datad.length - offset);
			if (bytesRead == -1)
				break;
			offset += bytesRead;
		}
		in.close();

		FileOutputStream out = new FileOutputStream(fileName);
		out.write(datad);
		out.flush();
		out.close();
	}

	/**
	 * @param fileName
	 * @throws IOException
	 */
	public void writeTo(String fileName) throws IOException {
		Writer writer = new FileWriter(fileName);
		writer.write(buffer.toString());
		writer.close();
	}

}
