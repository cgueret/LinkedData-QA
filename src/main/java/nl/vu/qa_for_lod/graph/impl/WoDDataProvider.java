/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.util.HashSet;
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
public class WoDDataProvider implements DataProvider {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(WoDDataProvider.class);
	// Stuff for the concurrent execution of data queries
	final ExecutorService executor = Executors.newFixedThreadPool(2);
	final Map<Resource, DataAcquisitionTask> queue = new ConcurrentHashMap<Resource, DataAcquisitionTask>();
	final Lock queueLock = new ReentrantLock();
	final Condition queueNotFull = queueLock.newCondition();
	final static int MAX_QUEUED_TASK = 100;
	final static Resource THIS = ResourceFactory.createResource("http://example.org/this");
	final static Property HAS_BLACK_LISTED = ResourceFactory.createProperty("http://example.org/blacklisted");
	final Lock modelLock = new ReentrantLock();
	final Model model;

	/**
	 * @param cacheDir
	 * 
	 */
	public WoDDataProvider(String cacheDir) {
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
			Set<Statement> stmts = model.listStatements(resource, (Property) null, (RDFNode) null).toSet();
			boolean blackListed = model.contains(THIS, HAS_BLACK_LISTED, resource);
			if (stmts.size() != 0 || blackListed)
				return stmts;
		} finally {
			modelLock.unlock();
		}

		// Not blacklisted and still no info, try to get the content
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
				futureTask = new DataAcquisitionTask(resource, model);
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
				for (Statement stmt : futureTask.getStatements())
					model.add(stmt);
				model.commit();
				modelLock.unlock();
				return futureTask.getStatements();
			} else {
				// Black list the resource
				modelLock.lock();
				model.add(THIS, HAS_BLACK_LISTED, resource);
				logger.warn("Added " + resource.getURI() + " to the black list");
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
