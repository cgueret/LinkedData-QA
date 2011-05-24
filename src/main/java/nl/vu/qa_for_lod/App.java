package nl.vu.qa_for_lod;

import nl.vu.qa_for_lod.metrics.impl.ClusteringCoefficient;
import nl.vu.qa_for_lod.metrics.impl.Degree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO add a metric to detect when most mappings are 1-1 and some 1-M (then
// they are suspicious)
public class App {
	static Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		// Load the graph file
		ExtraLinks extraLinks = new ExtraLinks("data/links-cut.nt");
		logger.info("Number of resources  = " + extraLinks.getResources().size());
		logger.info("Number of statements = " + extraLinks.getStatements().size());

		// Run the analysis
		MetricsExecutor metrics = new MetricsExecutor(extraLinks);
		metrics.addMetric(new Degree());
		metrics.addMetric(new ClusteringCoefficient());

		// Set the list of nodes to check
		for (Resource resource : extraLinks.getResources())
			metrics.addToResourcesQueue(resource);

		// Run all the metrics
		metrics.processQueue();

		// Print the execution report
		//metrics.printReport();
	}
}
