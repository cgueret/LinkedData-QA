package nl.vu.qa_for_lod.graph.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import nl.vu.qa_for_lod.crawler.HostState;
import nl.vu.qa_for_lod.graph.Direction;

import org.aksw.commons.sparql.api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreEx;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreH2;
import org.aksw.commons.sparql.api.cache.extra.CacheEx;
import org.aksw.commons.sparql.api.cache.extra.CacheExImpl;
import org.aksw.commons.sparql.api.core.QueryExecutionFactory;
import org.aksw.commons.sparql.api.dereference.Dereferencer;
import org.aksw.commons.sparql.api.dereference.QueryExecutionFactoryDereference;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

/*
 *
 	private final Direction direction;
	private final List<QueryExecutionFactory<? extends QueryExecution>> factories;
	public Direction getDirection() {
		return direction;
	}


	public List<QueryExecutionFactory<? extends QueryExecution>> getFactories() {
		return factories;
	}
		this.direction = direction;
		this.factories = factories;

 */

/**
 * A class for retrieving the data of a single resource.
 * 
 * @author Claus Stadler <cstadler@informatik.uni-leipzig.de>
 *
 */
public class DataAcquisitionTask2
	implements Callable<Model>
{
	// Logger
	final static Logger logger = LoggerFactory.getLogger(DataAcquisitionTask.class);

	private DataAcquisitionConfig config;
	
	private Resource resource;
	private Model model;
	
	// Note: Should be a synchronized LRU Map 
	private Map<Resource, Model> cache;
	
	public DataAcquisitionTask2(DataAcquisitionConfig config, Resource resource, Model model, Map<Resource, Model> cache) {
		this.config = config;
		this.resource = resource;
		this.model = model;
		this.cache = cache;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	public Model call() throws Exception {

		if(cache != null) {
			Model cached = cache.get(resource);
			if(cached != null) {
				logger.info("LRU-Cache: " + cached.size() + " triples for " + resource);
				model.add(cached);
				return model;
			}
		}
		
		for(QueryExecutionFactory<?> factory : config.getFactories()) {
			
			queryEndPoint(resource, model, factory);

			if(!model.isEmpty()) {
				
				logger.info(factory.getId() + ": " + model.size() + " triples for " + resource);
				
				break;
			}
		}

		if(model.isEmpty()) {
			logger.info("No data fetched for " + resource);
		}
		
		// Only cache small models in-memory
		// FIXME Make this configurabel
		if(cache != null) {
			if(model.size() < 100) {
				Model copy = ModelFactory.createDefaultModel();
				copy.add(model);
				cache.put(resource, copy);
			}
		}
		
		return model;
	}

	
	private Model doConstruct(Model result, Direction direction, Node node, Query query, QueryExecutionFactory<?> factory)
	{
		// Execute and add the results
		QueryExecution execution = factory.createQueryExecution(query);
		try {
			execution.execConstruct(result);
		}
		catch(QueryExceptionHTTP e) {
			System.out.println("Code = " + e.getResponseCode());
			logger.warn("Error fetching data for " + resource + " from " + factory.getId(), e);
		}
		
		return result;
	}

	
	private Model doDescribe(Model result, Direction direction, Node node, QueryExecutionFactory<?> factory) {
		Query query = CannedQueryUtils.describe(node);
		QueryExecution execution = factory.createQueryExecution(query);
		execution.execDescribe(result);
		DescribeDataProvider.filterInPlace(result, ResourceFactory.createResource(node.getURI()), direction);
		
		return result;
	}
	
	private Model doConstructWithDescribeFallback(Model result, Direction direction, Node node, Query query, QueryExecutionFactory<?> factory)
	{
		try {
			return doConstruct(result, direction, node, query, factory);
		}  catch(NotImplementedException e) { // FIXME Eventually use a different exception - or add a getCapabilities method to the factory

			doDescribe(result, direction, node, factory);
			
		}
		catch(RuntimeException e) {
			if(e.getCause() instanceof NotImplementedException) {
				doDescribe(result, direction, node, factory);
			}
		}
		
		//DescribeDataProvider.filterInPlace(result, resource, direction);
		
		return result;
	}
	
	/**
	 * @param statements
	 * @param string
	 * @return
	 */
	private Model queryEndPoint(Resource resource, Model model, QueryExecutionFactory<?> factory) {
		// Half the value of maxResultSetSize for both directions
		// Integer resultSetSize = null;
		// if(maxResultSetSize != null) {
		// if(direction.equals(Direction.BOTH)) {
		// resultSetSize = maxResultSetSize / 2;
		// } else {
		// resultSetSize = maxResultSetSize;
		// }
		// }
		//QueryExecutionFactory factory = EndpointManager.getDefaultInstance().get(endPoint);

		Node node = resource.asNode();

		Direction direction = config.getDirection();
		
		// Outgoing triples
		if (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH)) {
			Query query = CannedQueryUtils.outgoing(node);
			
			doConstructWithDescribeFallback(model, Direction.OUT, node, query, factory);
		}
		

		// Incoming triples
		if (direction.equals(Direction.IN) || direction.equals(Direction.BOTH)) {
			Query query = CannedQueryUtils.incoming(node);

			doConstructWithDescribeFallback(model, Direction.IN, node, query, factory);
		}

		return model;
	}


	public static void doDelay(long sleepTime)
	{
		if(sleepTime > 0) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				DataAcquisitionTask.logger.debug("Sleep interrupted", e);
			}
		}
	}


	public static long getDelay(long requestRate, long lastRequestTime)
	{
		long requestDelta = System.currentTimeMillis() - lastRequestTime;
		long sleepTime = requestRate - requestDelta;
	
		return sleepTime;
	}


	/**
	 * Some arbitrary criteria on when to reject a host.
	 * 
	 * @param state
	 * @return
	 */
	public static boolean isAcceptedHost(HostState state) {
		
		
		// Host must have something to do with RDF
		// This should rule out wikipedia after some requests
		if(state.getEmptyResultCount() > 100) {
			double ratio = state.getEmptyResultCount() / state.getRequestCount();
			
			if(ratio > 0.5) {
				DataAcquisitionTask.logger.warn("Rejected host " + state.getName() + " due to too many empty results: ");
				DataAcquisitionTask.logger.warn("" + state);
				return false;
			}
		}
		
		// Host must not have too many non-200 counts
		int errorCount = 0;
		synchronized(state.getHttpStatusCounts()) {
			for(Entry<Integer, Integer> entry : state.getHttpStatusCounts().entrySet()) {
				if(entry.getKey().equals(200)) {
					continue;
				}
				
				errorCount += entry.getValue();
			}
		}
	
		if(state.getRequestCount() > 100 && (errorCount / (float)state.getRequestCount()) > 0.2) {
			DataAcquisitionTask.logger.warn("Rejected host " + state.getName() + " due to too many errors: ");
			DataAcquisitionTask.logger.warn("" + state);
			return false;
		}
		
		return true;
	}


	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
	    //PropertyConfigurator.configure("log4j.properties");

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    // print logback's internal status
	    StatusPrinter.print(lc);
	    	    
		List<QueryExecutionFactory<?>> factories = new ArrayList<QueryExecutionFactory<?>>();
		
		Dereferencer dereferencer = DereferencerAny23.create("LATC QA tool <cstadler@informatik.uni-leipzig.de>");
		
		QueryExecutionFactory<?> factory = new QueryExecutionFactoryDereference(dereferencer);
		
		// Create a cache using a database called 'cache'
		CacheCoreEx cacheBackend = CacheCoreH2.create("cache", 15000000, true);
		CacheEx cacheFrontend = new CacheExImpl(cacheBackend);
		
		// The following caching query execution factory associates all cache entries
		// with 'the-internet'
		factory = new QueryExecutionFactoryCacheEx(factory, "http://the-internet.org", cacheFrontend);
		
		
		factories.add(factory);

		
		DataAcquisitionConfig config = new DataAcquisitionConfig(factories, Direction.BOTH);
		DataAcquisitionTaskFactoryImpl daFactory = new DataAcquisitionTaskFactoryImpl(config);
		
		DataAcquisitionTask2 me = daFactory.createTask(ResourceFactory.createResource("http://www.w3.org/2002/07/owl#Thing"), ModelFactory.createDefaultModel());

		Model result = me.call();
		result.write(System.out, "N-TRIPLES", null);
	}
}

