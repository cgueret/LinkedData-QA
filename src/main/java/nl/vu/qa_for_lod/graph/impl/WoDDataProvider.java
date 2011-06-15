/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.tdb.TDBFactory;

import nl.vu.qa_for_lod.graph.DataProvider;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO Add implementation to query SPARQL end points before de-referencing
public class WoDDataProvider implements DataProvider {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(WoDDataProvider.class);
	
	// Stuff for the concurrent execution of data queries
	final ExecutorService executor = Executors.newFixedThreadPool(2);
	final Map<Resource, DataAcquisitionTask> queue = new HashMap<Resource, DataAcquisitionTask>();
	final Lock queueLock = new ReentrantLock();
	final Condition queueNotFull = queueLock.newCondition();
	final static int MAX_QUEUED_TASK = 100;
	
	// Jena-based caching
	final static Resource CACHE = ResourceFactory.createResource("http://example.org/cache");
	final static Property CONTAINS = ResourceFactory.createProperty("http://example.org/contains");
	final static Property FAILED_ON = ResourceFactory.createProperty("http://example.org/failed");
	final Lock modelLock = new ReentrantLock();
	final Model model;

	// List of end points
	final List<String> endPoints = new ArrayList<String>();
	
	/**
	 * @param endPointURI
	 */
	public void addEndPoint(String endPointURI) {
		endPoints.add(endPointURI);
	}

	/**
	 * @param cacheDir
	 * 
	 */
	public WoDDataProvider(String cacheDir) {
		logger.info("Initialise cache in " + cacheDir);
		model = TDBFactory.createModel(cacheDir);
		// model.removeAll(THIS, HAS_BLACK_LISTED, (RDFNode)null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.graph.DataProvider#close()
	 */
	public void close() {
		modelLock.lock();
		model.close();
		modelLock.unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.DataProvider#get(com.hp.hpl.jena.rdf.model.Resource
	 * )
	 */
	public Set<Statement> get(Resource resource) {
		// Try to get the content from the cache
		modelLock.lock();
		try {
			if (model.contains(CACHE, CONTAINS, resource))
				return model.listStatements(resource, (Property) null, (RDFNode) null).toSet();
		} finally {
			modelLock.unlock();
		}

		// Not in the cache, try to get the content
		DataAcquisitionTask futureTask = null;
		queueLock.lock();
		try {
			// Get the running task for this query
			futureTask = queue.get(resource);

			// If it is non existing and the queue is full, wait and ask again
			if (futureTask == null && queue.size() == MAX_QUEUED_TASK) {
				while (queue.size() == MAX_QUEUED_TASK)
					queueNotFull.await();
				futureTask = queue.get(resource);
			}

			// If there is none, create one
			if (futureTask == null) {
				// logger.info("Download data for " + resource);
				futureTask = new DataAcquisitionTask(endPoints, resource, model);
				queue.put(resource, futureTask);
				executor.submit(futureTask);
			} else {
				// logger.info("Already getting data for " + resource);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			queueLock.unlock();
		}

		// Wait for completion
		try {
			Boolean isSuccessful = futureTask.call();
			if (isSuccessful) {
				// Save the data
				modelLock.lock();
				model.add(CACHE, CONTAINS, resource);
				for (Statement stmt : futureTask.getStatements())
					model.add(stmt);
				// model.commit();
				modelLock.unlock();
				return futureTask.getStatements();
			} else {
				// Black list the resource
				modelLock.lock();
				logger.warn("Failed getting data from " + resource.getURI());
				model.add(CACHE, CONTAINS, resource);
				model.add(CACHE, FAILED_ON, resource);
				modelLock.unlock();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			queueLock.lock();
			queue.remove(resource);
			if (queue.size() != MAX_QUEUED_TASK)
				queueNotFull.signal();
			queueLock.unlock();
		}

		// At worse, return an empty set
		return new HashSet<Statement>();
	}
}
