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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import nl.vu.qa_for_lod.graph.Direction;
import nl.vu.qa_for_lod.graph.EndPoint;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO Add implementation to query SPARQL end points before de-referencing
public class WoDDataProvider implements DataProvider {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(WoDDataProvider.class);

	// Stuff for the concurrent execution of data queries
	final static int MAX_QUEUED_TASK = 100;
	final ExecutorService executor = Executors.newFixedThreadPool(2);
	final Map<Resource, Future<Set<Statement>>> queue = new HashMap<Resource, Future<Set<Statement>>>();
	final Lock queueLock = new ReentrantLock();
	final Condition queueNotFull = queueLock.newCondition();

	// Jena-based caching
	final static Resource CACHE = ResourceFactory.createResource("http://example.org/cache");
	final static Property CONTAINS = ResourceFactory.createProperty("http://example.org/contains");
	final static Property FAILED_ON = ResourceFactory.createProperty("http://example.org/failed");
	final Lock modelLock = new ReentrantLock();
	final Model model;

	// List of end points
	final List<EndPoint> endPoints = new ArrayList<EndPoint>();

	/**
	 * @param endPoint
	 */
	public void addEndPoint(EndPoint endPoint) {
		endPoints.add(endPoint);
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
	public Set<Statement> get(Resource resource, Direction direction) {
		// Try to get the content from the cache
		modelLock.lock();
		try {
			if (model.contains(CACHE, CONTAINS, resource)) {
				Set<Statement> statements = new HashSet<Statement>();
				if (direction.equals(Direction.IN) || direction.equals(Direction.BOTH))
					for (Statement st: model.listStatements((Resource) null, (Property) null, resource).toSet())
						if (!st.getSubject().equals(CACHE))
							statements.add(st);
				if (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH))
					statements.addAll(model.listStatements(resource, (Property) null, (RDFNode) null).toSet());
				return statements;
			}
		} finally {
			modelLock.unlock();
		}

		// Not in the cache, try to get the content
		queueLock.lock();
		try {
			// Get the running task for this query
			Future<Set<Statement>> futureTask = queue.get(resource);

			// If it is non existing and the queue is full, wait and ask again
			if (futureTask == null && queue.size() == MAX_QUEUED_TASK) {
				while (queue.size() == MAX_QUEUED_TASK)
					queueNotFull.await();
				futureTask = queue.get(resource);
			}

			// If there is none, create one
			if (futureTask == null) {
				logger.info("Download data for " + resource);
				DataAcquisitionTask dataTask = new DataAcquisitionTask(endPoints, resource, direction, model);
				queue.put(resource, executor.submit(dataTask));
			} 
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			queueLock.unlock();
		}

		// Get the future task
		queueLock.lock();
		Future<Set<Statement>> futureTask = null;
		try {
			futureTask = queue.get(resource);
		} finally {
			queueLock.unlock();
		}

		// Wait for completion
		try {
			Set<Statement> statements = futureTask.get();
			modelLock.lock();
			if (statements.isEmpty()) {
				// Black list the resource
				logger.warn("Empty data for " + resource.getURI());
				model.add(CACHE, CONTAINS, resource);
				model.add(CACHE, FAILED_ON, resource);
				model.commit();
				modelLock.unlock();
			} else {
				// Save the data
				model.add(CACHE, CONTAINS, resource);
				for (Statement stmt : statements)
					model.add(stmt);
				model.commit();
				modelLock.unlock();
				return statements;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			queueLock.lock();
			queue.remove(resource);
			if (queue.size() < MAX_QUEUED_TASK)
				queueNotFull.signal();
			queueLock.unlock();
		}

		// In the worse case, return an empty set
		return new HashSet<Statement>();
	}
}
