/**
 * 
 */
package nl.vu.qa_for_lod.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class HallOfFame {
	private final Map<Resource, Double> scores = new HashMap<Resource, Double>();
	private final int size;

	/**
	 * @param size
	 */
	public HallOfFame(int size) {
		this.size = size;
	}

	/**
	 * @return
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param suspiciousNodes
	 */
	public void insert(List<Resource> suspiciousNodes) {
		for (int index = 0; index < suspiciousNodes.size(); index++) {
			Resource key = suspiciousNodes.get(index);
			if (!scores.containsKey(key))
				scores.put(key, new Double(0));
			scores.put(key, scores.get(key) + size - index);
		}
	}

	/**
	 * 
	 */
	public void print() {
		TreeSet<Double> keys = new TreeSet<Double>();
		keys.addAll(scores.values());

		for (Double key : keys.descendingSet())
			for (Entry<Resource, Double> entry : scores.entrySet())
				if (entry.getValue().equals(key))
					System.out.println(entry.getValue() + " " + entry.getKey());
	}

}
