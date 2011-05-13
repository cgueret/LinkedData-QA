/**
 * 
 */
package nl.vu.qa_for_lod.data;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Distribution extends HashMap<Integer, Map<String, Integer>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8746268674977907936L;

	public Distribution() {
		// Initialise the keys
		for (int key = 0; key < 101; key++)
			this.put(key, new HashMap<String, Integer>());
	}

	/**
	 * @param distribs
	 * 
	 */
	public void write(String fileName) {
		// Get the keys for the rows and columns
		Set<Integer> keys = new TreeSet<Integer>();
		keys.addAll(this.keySet());
		Set<String> names = new TreeSet<String>();
		names.addAll(this.get(keys.toArray()[0]).keySet());

		try {
			Writer file = new FileWriter(fileName);
			
			// Print the headers
			StringBuffer header = new StringBuffer();
			header.append("# Percent");
			for (String name : names)
				header.append(" ").append(name);
			file.write(header.append("\n").toString());

			// Print the content of the table
			for (Integer key : keys) {
				StringBuffer row = new StringBuffer();
				row.append(key);
				for (String name : names)
					row.append(" ").append(this.get(key).get(name));
				file.write(row.append("\n").toString());
			}
			
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param results
	 * @param key
	 * @param distributions
	 */
	public void saveResults(Map<String, Double> results, String name) {
		// Initialise results
		for (Entry<Integer, Map<String, Integer>> entry : this.entrySet())
			entry.getValue().put(name, 0);

		// Find the highest value
		Double max = Double.MIN_VALUE;
		for (Entry<String, Double> result : results.entrySet())
			if (result.getValue() > max)
				max = result.getValue();

		// Fill the distribution table
		for (Entry<String, Double> result : results.entrySet()) {
			int key = (int) (100 * (result.getValue() / max));
			Map<String, Integer> row = this.get(key);
			row.put(name, row.get(name) + 1);
		}
	}
}
