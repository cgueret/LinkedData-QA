/**
 * 
 */
package nl.vu.qa_for_lod.report;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vu.qa_for_lod.metrics.Distribution;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class DistributionsTable extends HashMap<Double, Map<String, Double>> {
	static Logger logger = LoggerFactory.getLogger(DistributionsTable.class);
	private static final long serialVersionUID = -8746268674977907936L;
	private final Set<String> labels = new LinkedHashSet<String>();

	/**
	 * Append a distribution into the structure and save it under some name
	 * 
	 * @param distribution
	 * @param name
	 */
	public void insert(Distribution distribution, String name) {
		// Add the name to the list of column labels
		labels.add(name);

		// Iterate over the values
		for (Entry<Double, Double> result : distribution.entrySet()) {
			// Get the row
			Map<String, Double> row = get(result.getKey());

			// If the row is not existing, create it
			if (row == null) {
				row = new HashMap<String, Double>();
				this.put(result.getKey(), row);
			}

			// Save the value for that row*column
			row.put(name, result.getValue());
		}

		// Fill the NULLs with zero
		for (Double row : this.keySet())
			for (String column : labels)
				if (this.get(row).get(column) == null)
					this.get(row).put(column, 0d);
	}

	/**
	 * @param distribs
	 * 
	 */
	public void write(String fileName) {
		// Get the keys for the rows
		Set<Double> rowLabels = new TreeSet<Double>();
		rowLabels.addAll(this.keySet());

		try {
			Writer file = new FileWriter(fileName);

			// Print the headers
			StringBuffer header = new StringBuffer();
			header.append("# Value ");
			for (String label : labels)
				header.append(" ").append(label);
			file.write(header.append("\n").toString());

			// Print the content of the table
			for (Double rowLabel : rowLabels) {
				StringBuffer rowStr = new StringBuffer();
				rowStr.append(rowLabel);
				for (String label : labels)
					rowStr.append(" ").append(this.get(rowLabel).get(label));
				file.write(rowStr.append("\n").toString());
			}

			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
