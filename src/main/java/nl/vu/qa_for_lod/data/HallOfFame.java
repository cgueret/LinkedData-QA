/**
 * 
 */
package nl.vu.qa_for_lod.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class HallOfFame {
	private final Map<String, Double> scores = new HashMap<String, Double>();
	private final int size;

	/**
	 * @param i
	 */
	public HallOfFame(int size) {
		this.size = size;
	}

	/**
	 * @param suspiciousNodes
	 */
	public void insert(List<String> suspiciousNodes) {
		for (int index = 0; index < suspiciousNodes.size(); index++) {
			String key = suspiciousNodes.get(index);
			if (!scores.containsKey(key))
				scores.put(key, new Double(0));
			scores.put(key, scores.get(key) + size - index);
		}
	}

	/**
	 * @return
	 */
	public int getSize() {
		return size;
	}

	/**
	 * 
	 */
	public void print() {
		for (Entry<String, Double> entry : scores.entrySet())
			System.out.println(entry.getValue() + " " + entry.getKey());
	}

}
