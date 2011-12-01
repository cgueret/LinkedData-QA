package nl.vu.qa_for_lod.crawler;



import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.vu.qa_for_lod.graph.EndPoint;

import org.aksw.commons.sparql.api.cache.core.QueryExecutionFactoryCache;
import org.aksw.commons.sparql.api.cache.extra.CacheCore;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreH2;
import org.aksw.commons.sparql.api.cache.extra.CacheImpl;
import org.aksw.commons.sparql.api.core.QueryExecutionFactory;
import org.aksw.commons.sparql.api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.commons.sparql.api.http.QueryExecutionFactoryHttp;
import org.aksw.commons.sparql.api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.commons.util.strings.StringUtils;



public class EndpointManager {
	public Map<EndPoint, QueryExecutionFactory> endpointToFactory = new HashMap<EndPoint, QueryExecutionFactory>();
	
	
	private static EndpointManager defaultInstance;
	
	public synchronized static EndpointManager getDefaultInstance()
	{
		if(defaultInstance == null) {
			defaultInstance = new EndpointManager(); 
		}
		
		return defaultInstance;
	}
	
	public EndpointManager() {
		
	}
	
	public QueryExecutionFactory createDefaultFactory(EndPoint endpoint)
		throws Exception
	{
		// NOTE: There is no need for our cache, as there is already Christoph's cache.
		
		QueryExecutionFactory f = new QueryExecutionFactoryHttp(endpoint.getURI(), Collections.singletonList(endpoint.getGraph()));
		f = new QueryExecutionFactoryDelay(f, 1000);
		f = new QueryExecutionFactoryPaginated(f, 500);
		//CacheCore core = CacheCoreH2.create("cache/sparql/" + StringUtils.urlEncode(endpoint.getURI()) + "/" + StringUtils.urlEncode(endpoint.getGraph()));
		//CacheImpl cache = new CacheImpl(core);
		//f = new QueryExecutionFactoryCache(f, cache);
		//f = 

		return f;
	}
	
	public synchronized QueryExecutionFactory get(EndPoint endpoint)
	{	
		QueryExecutionFactory factory = endpointToFactory.get(endpoint);
		if(factory == null) {
			try {
				factory = createDefaultFactory(endpoint);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		
			endpointToFactory.put(endpoint, factory);
		}
		
		return factory;
	}
}
