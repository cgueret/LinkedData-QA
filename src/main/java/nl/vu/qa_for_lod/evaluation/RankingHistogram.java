package nl.vu.qa_for_lod.evaluation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.vu.qa_for_lod.report.HTMLReport;

import org.aksw.commons.collections.Descender;
import org.aksw.commons.collections.FileDescender;
import org.aksw.commons.util.XPathUtils;
import org.mindswap.pellet.utils.PatternFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.googlecode.charts4j.GChart;




public class RankingHistogram
{
	
	public static void find(File dir, String name, List<File> result) {
		if(dir == null) {
			return;
		}
		
		File[] files = dir.listFiles();
		if(files == null) {
			return;
		}
		
		for(File file : files) {
			if(file.isDirectory()) {
				find(file, name, result);
			} else  if(file.getName().equals(name)) {
				result.add(file);
			}
		}
	}
	
	public static void main(String[] args)
		throws Exception
	{
		FileFilter fileFilter = new PatternFilter(".*");		
		Descender<File> descender = new FileDescender(fileFilter);
		
		File baseDir = new File("/home/raven/Documents/LinkQA/Evaluation/ESWC2012/repos/rankingRepo/");
		List<File> files = new ArrayList<File>();
		
		find(baseDir, "report.html", files);
		
		
		Map<String, List<Number>> histograms = new HashMap<String, List<Number>>();
		
		
		for(File file : files) {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        factory.setValidating(false);
	        factory.setNamespaceAware(false);
	        factory.setXIncludeAware(false);
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document xmlDoc = builder.parse(file);
	
	        /*DomDocument document = new DomDocument();
	        document.createTextNode()
	        //This is used to remove white spaces instead of document->preserveWhiteSpace = false used in php
	        document.normalize();*/
	        xmlDoc.normalize();
			
			NodeList nodes = XPathUtils.evalToNodes(xmlDoc, "//table[2]//tr");
			
			for(int i = 0; i < nodes.getLength(); ++i) {
				Node tr = nodes.item(i);
				NodeList tds = XPathUtils.evalToNodes(tr, "td");

				List<Number> histogram = null;
				//int[] histogram = new int[11];
				//Arrays.fill(histogram, 0);

				
				int remainingItems = 10;
				for(int j = 0; j < tds.getLength(); ++j) {
					Node td = tds.item(j);

					if(j == 0) {
						
						String metricName = td.getTextContent().trim();
						
						histogram = histograms.get(metricName);
						
						if(histogram == null) {
							histogram = new ArrayList<Number>();
							
							for(int k = 0; k < 11; ++k) {
								histogram.add(0);
							}
							
							histograms.put(metricName, histogram);
						}
						
						System.out.println(td.getTextContent());
						continue;
					}
					
					int rank = j - 1;
					
					
					
					Node style = td.getAttributes().getNamedItem("style");
					if(style != null) {
						int index = rankToBucket(rank);
						histogram.set(index, histogram.get(index).intValue() + 1);
						remainingItems--;
					//System.out.println(style.getTextContent());
					}
					
					//System.out.println(td.getTextContent());
					
				}
				
				
				if(histogram != null) {
					int index = histogram.size() - 1;
					histogram.set(index, histogram.get(index).intValue() + remainingItems);
				}
				//histogram[histogram.length - 1] += remainingItems;

				/*
				for(int k = 0; k < histogram.length; ++k) {
					String range = k + " - " + 10 * (k + 1);
					
					System.out.println(range + ": " + histogram[k]);
				}*/
			}
		}		
		
		
		System.out.println(files.size());
		
		for(Entry<String, List<Number>> entry : histograms.entrySet()) {
			//System.out.println(entry.getKey());
						
			
			List<Number> histogram = entry.getValue();
			
			for(int k = 0; k < histogram.size(); ++k) {
				String range = k + " - " + 10 * (k + 1);
				
				System.out.println(range + ": " + histogram.get(k));
			}
		}
		
		System.out.println(createRankingHistogramCharts(histograms));
		
		
		/*
		DescenderIterator<File> it = new DescenderIterator<File>(baseDir, descender);
		
		while(it.hasNext()) {
			while(it.canDescend()) {
				it.descend();
			}
			
			List<File> files = it.next();
			
			File file = files.get(files.size() - 1);
			if(!file.getName().equals("report.html")) {
				continue;
			}
			
			System.out.println(file.getAbsolutePath());
			
		}
		*/

		
	}
	
	
	public static String createRankingHistogramCharts(Map<String, List<Number>> metricToData)
	{
		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		
		PrintStream out = new PrintStream(tmp);
		
		out.println("<!DOCTYPE HTML>");
		out.println("<html>");
		out.println("<head>");

		out.println("<title></title>");
		
		out.println("<style type='text/css'>");
		out.println("body {color: #000000;font-family: Helvetica,Arial,sans-serif;font-size: small;}");
		out.println("h1 {background-color: #E5ECF9;border-top: 1px solid #3366CC;font-size: 130%;font-weight: bold;margin: 2em 0 0 -10px;padding: 1px 3px;}");
		out.println("td {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 6px 12px;text-align: left;vertical-align: top;}");
		out.println("img {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 20px 20px;}");
		out.println("</style>");
		
		
		out.println("</head>");
		out.println("<title>Evaluation results</title>");
		out.println("<body>");
		
		out.println("<h1>Distribution of the ranking of negative links</h1>");
		out.println("Left = greatest outliers, based on 33 Samples consisting of 90 positive links and 10 negative ones. Each bar corresponds to a bucket of 10 resources.");

		
		for(Entry<String, List<Number>> entry : metricToData.entrySet()) {

			GChart chart = HTMLReport.createSimpleBarChart(entry.getKey(), entry.getValue(), 0, null);

			out.print("<img style='float:left;padding:5px;margin:5px' src='");
			out.print(chart.toURLForHTML());
			out.println("'/>");			
		}
		

		
		out.println("</body>");
		out.println("</html>");
		
		return tmp.toString();
	}
	
	public static int rankToBucket(int rank) {
		
		// Ten buckets + 1 Extra buckets for out-of-range stuff
		int n = 10;
		int maxRank = 100;
		int result = Math.min((int)(rank / (float)maxRank * n), 10);
		
		if(result >= 9) {
			System.out.println(result);
		}
		
		return result;
	}
	
}

