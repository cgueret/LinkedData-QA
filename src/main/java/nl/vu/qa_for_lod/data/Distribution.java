/**
 * 
 */
package nl.vu.qa_for_lod.data;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import nl.vu.qa_for_lod.metrics.Metric.MetricState;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Distribution {
	private final static DecimalFormat df = new DecimalFormat("###.######");
	private final Map<Double, Map<MetricState, Double>> data = new HashMap<Double, Map<MetricState, Double>>();

	/**
	 * @param x
	 * @param state
	 */
	public void increaseCounter(double x, MetricState state) {
		// Round the number
		Double key = Double.valueOf(df.format(x));
		
		// Get the requested column
		Map<MetricState, Double> column = null;
		if (data.containsKey(key)) {
			column = data.get(key); 
		} else {
			column = new HashMap<MetricState, Double>();
			data.put(key, column);
		}

		// Get the current value of the column
		double counter = (column.containsKey(state) ? column.get(state) : 0);

		// Save the new value
		column.put(state, counter + 1);
	}

	/**
	 * 
	 */
	public void writeToFile(String fileName) {
		Set<Double> keys = new TreeSet<Double>();
		keys.addAll(data.keySet());

		try {
			Writer file = new FileWriter(fileName);

			for (Double key : keys) {
				StringBuffer buffer = new StringBuffer();
				buffer.append(key).append(" ");
				for (MetricState state : MetricState.values()) {
					if (data.get(key).containsKey(state))
						buffer.append(data.get(key).get(state)).append(" ");
					else
						buffer.append(0).append(" ");
				}
				file.write(buffer.append("\n").toString());
			}

			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
