/**
 * 
 */
package nl.vu.qa_for_lod.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Statistics extends HashMap<String, Map<String, Double>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2612141969470753015L;

	/**
	 * @param seedResources
	 */
	public Statistics(Set<Resource> seedResources) {
		// Init hash map
		for (Resource resource : seedResources)
			this.put(resource.getURI(), new HashMap<String, Double>());
	}

	/**
	 * @param results
	 * @param stats
	 * @param key
	 */
	public void saveResults(Map<String, Double> results, String key) {
		for (Entry<String, Double> result : results.entrySet())
			if (this.keySet().contains(result.getKey()))
				this.get(result.getKey()).put(key, result.getValue());
	}
}
