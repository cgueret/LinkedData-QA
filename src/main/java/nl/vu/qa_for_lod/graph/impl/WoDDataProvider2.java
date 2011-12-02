package nl.vu.qa_for_lod.graph.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nl.vu.qa_for_lod.graph.DataAcquisitionTaskFactory;
import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.Direction;

import org.aksw.commons.jena.ModelSetView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


public class WoDDataProvider2 implements DataProvider {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(WoDDataProvider2.class);

	// Stuff for the concurrent execution of data queries
	final static int MAX_QUEUED_TASK = 100;
	final ExecutorService executor = Executors.newFixedThreadPool(5);
	final Map<Resource, Future<Model>> queue = new HashMap<Resource, Future<Model>>();
	final Lock queueLock = new ReentrantLock();
	final Condition queueNotFull = queueLock.newCondition();

	private DataAcquisitionTaskFactory taskFactory;
	
	/**
	 * @param taskFactory
	 * 
	 */
	public WoDDataProvider2(DataAcquisitionTaskFactory taskFactory) {
		this.taskFactory = taskFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.graph.DataProvider#close()
	 */
	public void close() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.DataProvider#get(com.hp.hpl.jena.rdf.model.Resource
	 * )
	 */
	public Set<Statement> get(Resource resource, Direction direction) {
		Model model = _get(resource, direction);
		return new ModelSetView(model);
	}
	
	public Model _get(Resource resource, Direction direction) {
		
		queueLock.lock();
		Model result = ModelFactory.createDefaultModel();
		try {
			// Get the running task for this query
			Future<Model> futureTask = queue.get(resource);

			// If it is non existing and the queue is full, wait and ask again
			if (futureTask == null && queue.size() == MAX_QUEUED_TASK) {
				while (queue.size() == MAX_QUEUED_TASK)
					queueNotFull.await();
				futureTask = queue.get(resource);
			}

			// If there is none, create one
			if (futureTask == null) {
				//logger.info("Download data for " + resource);
				Callable<Model> dataTask = taskFactory.createTask(resource, result);
				queue.put(resource, executor.submit(dataTask));
			} 
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			queueLock.unlock();
		}

		// Get the future task
		queueLock.lock();
		Future<Model> futureTask = null;
		try {
			futureTask = queue.get(resource);
		} finally {
			queueLock.unlock();
		}

		// Wait for completion
		try {
			return futureTask.get();
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
		return result;
	}
}
