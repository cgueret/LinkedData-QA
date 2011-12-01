package nl.vu.qa_for_lod.graph.impl;

import java.util.Collections;
import java.util.Map;

import nl.vu.qa_for_lod.graph.DataAcquisitionTaskFactory;

import org.apache.commons.collections15.map.LRUMap;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class DataAcquisitionTaskFactoryImpl
	implements DataAcquisitionTaskFactory
{
	private DataAcquisitionConfig config;
	
	private Map<Resource, Model> cache = Collections.synchronizedMap(new LRUMap<Resource, Model>(1000));
	
	public DataAcquisitionTaskFactoryImpl(DataAcquisitionConfig config) {
		this.config = config;
	}
	
	/*
	public DataAcquisitionTask2 createTask(Resource resource) {
		return new DataAcquisitionTask2(config, resource, ModelFactory.createDefaultModel());
	}*/

	@Override
	public DataAcquisitionTask2 createTask(Resource resource, Model model) {
		return new DataAcquisitionTask2(config, resource, model, cache);
	}
}