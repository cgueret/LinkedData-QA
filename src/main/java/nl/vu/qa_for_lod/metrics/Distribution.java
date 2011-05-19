/**
 * 
 */
package nl.vu.qa_for_lod.metrics;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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
		// TODO Auto-generated method stub
		return data.entrySet();
	}

	/**
	 * @param x
	 * @param state
	 */
	public void increaseCounter(double x) {
		Double key = Double.valueOf(df.format(x));
		Double counter = (data.containsKey(key) ? data.get(key) : Double.valueOf(1));
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
	 * @return
	 */
	public int size() {
		return data.size();
	}

	/**
	 * 
	 */
	public void writeToFile(String fileName) {
		/*
		 * Set<Double> keys = new TreeSet<Double>(); keys.addAll(data.keySet());
		 * 
		 * try { Writer file = new FileWriter(fileName);
		 * 
		 * for (Double key : keys) { StringBuffer buffer = new StringBuffer();
		 * buffer.append(key).append(" "); for (MetricState state :
		 * MetricState.values()) { if (data.get(key).containsKey(state))
		 * buffer.append(data.get(key).get(state)).append(" "); else
		 * buffer.append(0).append(" "); }
		 * file.write(buffer.append("\n").toString()); }
		 * 
		 * file.close(); } catch (IOException e) { e.printStackTrace(); }
		 */
	}
}
