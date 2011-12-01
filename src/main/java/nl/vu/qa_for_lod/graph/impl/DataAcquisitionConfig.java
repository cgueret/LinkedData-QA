package nl.vu.qa_for_lod.graph.impl;

import java.util.List;

import nl.vu.qa_for_lod.graph.Direction;

import org.aksw.commons.sparql.api.core.QueryExecutionFactory;

import com.hp.hpl.jena.query.QueryExecution;


/**
 * Having a separate config class for just two fields is somewhat overengineered,
 * but before some refactoring there were alot more fields here
 * 
 * @author Claus Stadler <cstadler@informatik.uni-leipzig.de>
 *
 */
public class DataAcquisitionConfig
{
	private final List<QueryExecutionFactory<? extends QueryExecution>> factories;
 	private final Direction direction;

 	public DataAcquisitionConfig(
			List<QueryExecutionFactory<? extends QueryExecution>> factories,
			Direction direction) {
		super();
		this.factories = factories;
		this.direction = direction;
	}

	public List<QueryExecutionFactory<? extends QueryExecution>> getFactories() {
		return factories;
	}

	public Direction getDirection() {
		return direction;
	}
}