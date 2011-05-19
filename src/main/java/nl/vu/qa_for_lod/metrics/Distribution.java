/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Distribution {
	public static enum Axis {
		X, Y
	}

	private final static DecimalFormat df = new DecimalFormat("######.######");

	private final Map<Double, Double> data = new HashMap<Double, Double>();

	/**
	 * @return
	 */
	public Set<Entry<Double, Double>> entrySet() {
		return data.entrySet();
	}

	/**
	 * Divide all the Y values by their sum to get probabilities
	 */
	public void normalize() {
		// Count
		Double total = 0.0;
		for (Double v : data.values())
			total += v;

		// Rescale
		if (total != 0)
			for (Entry<Double, Double> entry : data.entrySet())
				data.put(entry.getKey(), entry.getValue() / total);
	}

	/**
	 * @param x
	 * @param state
	 */
	public void increaseCounter(double x) {
		Double key = Double.valueOf(df.format(x));
		Double counter = (data.containsKey(key) ? data.get(key) + 1 : Double.valueOf(1));
		data.put(key, counter);
	}

	/**
	 * @param axis
	 * @return
	 */
	public Double max(Axis axis) {
		TreeSet<Double> values = new TreeSet<Double>(axis.equals(Axis.X) ? data.keySet() : data.values());
		return values.last();
	}

	/**
	 * @param axis
	 * @return
	 */
	public Double min(Axis axis) {
		TreeSet<Double> values = new TreeSet<Double>(axis.equals(Axis.X) ? data.keySet() : data.values());
		return values.first();
	}

	/**
	 * @param axis
	 * @return
	 */
	public double standardDeviation(Axis axis) {
		StandardDeviation sd = new StandardDeviation();
		return sd.evaluate(getValues(axis));
	}

	/**
	 * @param axis
	 * @return
	 */
	public double mean(Axis axis) {
		Mean mean = new Mean();
		return mean.evaluate(getValues(axis));
	}

	/**
	 * @param axis
	 * @return
	 */
	private double[] getValues(Axis axis) {
		Collection<Double> values = (axis.equals(Axis.X) ? data.keySet() : data.values());
		double[] numbers = new double[values.size()];
		int i = 0;
		for (Double v : values)
			numbers[i++] = v;
		return numbers;
	}

	/**
	 * @return
	 */
	public int size() {
		return data.size();
	}

	/**
	 * Write the distribution to a file
	 * 
	 * @throws IOException
	 */
	public void writeToFile(String fileName) throws IOException {
		Set<Double> keys = new TreeSet<Double>();
		keys.addAll(data.keySet());

		Writer file = new FileWriter(fileName);
		for (Double key : keys) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(df.format(key)).append(" ").append(df.format(data.get(key)));
			file.write(buffer.append("\n").toString());
		}
		file.close();
	}

}
