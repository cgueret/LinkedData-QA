package nl.vu.qa_for_lod.graph;

import nl.vu.qa_for_lod.graph.impl.DataAcquisitionTask2;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public interface DataAcquisitionTaskFactory {
	/**
	 * 
	 * 
	 * @param resource Resource for which to acquire data
	 * @param model Model to write the data to
	 * @return
	 */
	DataAcquisitionTask2 createTask(Resource resource, Model model);
	
	//void close();
}
