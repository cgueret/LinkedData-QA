/**
 * 
 */
package nl.vu.qa_for_lod.metrics.impl;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.openrdf.model.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import nl.vu.qa_for_lod.graph.Direction;
import nl.vu.qa_for_lod.graph.Graph;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;

/**
 * Aggregate the set of links that define a node accross all its different
 * SameAs nodes
 * 
 * Target distribution: Average equal to the number of distinct outgoing links
 * in the net (upper bound)
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Richness implements Metric {
	static Logger logger = LoggerFactory.getLogger(Richness.class);
	private final static Property SAME_AS = ResourceFactory.createProperty(OWL.SAMEAS.stringValue());

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.metrics.Metric#getDistanceToIdeal(nl.vu.qa_for_lod.metrics
	 * .Distribution)
	 */
	public double getDistanceToIdeal(Distribution inputDistribution) {
		double distance = 0;
		for (Entry<Double, Double> entry : inputDistribution.entrySet()) {
			double nb = entry.getValue();
			double val = entry.getKey();
			distance += nb * (1 / (1 + val));
		}
		return distance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getName()
	 */
	public String getName() {
		return "Description richness";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.metrics.Metric#getResult(nl.vu.qa_for_lod.Graph,
	 * com.hp.hpl.jena.rdf.model.Resource)
	 */
	public double getResult(Graph graph, Resource resource) {
		// Get the current description, remove the sameAs from it
		Set<Resource> description = graph.getNeighbours(resource, Direction.OUT, null);
		Set<Resource> sameAsResources = graph.getNeighbours(resource, Direction.OUT, SAME_AS);
		description.removeAll(sameAsResources);

		// Get the enriched description
		Set<Resource> enrich = new HashSet<Resource>();
		for (Resource sameAsResource : sameAsResources)
			enrich.addAll(graph.getNeighbours(sameAsResource, Direction.OUT, null));
		enrich.remove(resource);

		// Compute the set difference
		enrich.removeAll(description);

		return enrich.size();
	}

}
