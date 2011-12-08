package nl.vu.qa_for_lod.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.aksw.commons.util.IniUtils;
import org.openjena.atlas.lib.MapUtils;

enum Color {
	RED,
	GREEN
}

class MetricReport
{
	private String name;
	private double change;
	

	//private boolean isGreen;
	private Color color;
	
	public MetricReport(String name, double change, Color color)
	{
		this.name = name;
		this.change = change;
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public double getChange() {
		return change;
	}
	
	public Color getColor() {
		//return change < 0.0 ? Color.GREEN : Color.RED;
		//return isGreen ?
		return color;
	}

	@Override
	public String toString() {
		return "MetricReport [name=" + name + ", change=" + change + "]";
	}
	
}

class DatasetReport
{
	private String name;
	private Map<String, MetricReport> metricReports = new HashMap<String, MetricReport>();
	
	public DatasetReport(String name) {
		this.name = name;
	}

	/*
	public DatasetReport(Map<String, MetricReport> metricReports) {
		this.metricReports = metricReports;
	}
	*/
	
	public Map<String, MetricReport> getMetricReports() {
		return metricReports;
	}
	
	public void add(MetricReport metricReport) {
		metricReports.put(metricReport.getName(), metricReport);
	}

	@Override
	public String toString() {
		return "DatasetReport [name=" + name + ", metricReports="
				+ metricReports + "]";
	}
	
	
}


class EvalRun
{
	private List<DatasetReport> reports = new ArrayList<DatasetReport>();
	
	/*
	public EvalRun(List<DatasetReport> reports) {
		this.reports = reports;
	}
	*/
	
	public List<DatasetReport> getReports() {
		return reports;
	}

	@Override
	public String toString() {
		return "EvalRun [reports=" + reports + "]";
	}
	
	
}


class Dataset
{
	private String name;
	private EvalRun positive = new EvalRun();
	private EvalRun negative = new EvalRun();
	
	public Dataset(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public EvalRun getPositive() {
		return positive;
	}

	public EvalRun getNegative() {
		return negative;
	}

	@Override
	public String toString() {
		return "Dataset [name=" + name + ", positive=" + positive
				+ ", negative=" + negative + "]";
	}
	
	
}



public class AggregateResults {

	public static void main(String[] args)
			throws Exception	
	{
		createReportMixed("positive");
		createReportMixed("negative");
	}

	public static void createReportMixed(String type)
			throws Exception
		{
			Map<String, String> map = IniUtils.loadIniFile(new File("config"));
			
			
			
			String mixedRepoStr = map.get("mixedRepo");
			mixedRepoStr = "/home/raven/Documents/LinkQA/ESWC2012/Evaluation/runs/1/repos/mixed";
			
			File mixedRepo = new File(mixedRepoStr);
			
			List<Dataset> result = new ArrayList<Dataset>();
			
					

			Dataset ds = new Dataset("");
			
			collect(mixedRepo, ds, type);
			//collect(entry, dataset, "negative");			
			
			result.add(ds);

			
			Set<String> metricNames = new TreeSet<String>();
			for(Dataset dataset : result) {
				for(DatasetReport dr : dataset.getPositive().getReports()) {
					metricNames.addAll(dr.getMetricReports().keySet());
				}
				
				for(DatasetReport dr : dataset.getNegative().getReports()) {
					metricNames.addAll(dr.getMetricReports().keySet());
				}
			}

			
			// Do the aggregation and create the following table:
			// Dataset Metric correctGreens totalGreens correctReds totalReds
			
			PrintStream out = System.out;

			
			out.println("<!DOCTYPE HTML><html><head><title>Evaluation Results</title>");
			out.println("<style type=\"text/css\">");
			out.println("body {color: #000000;font-family: Helvetica,Arial,sans-serif;font-size: small;}");
			out.println("h1 {background-color: #E5ECF9;border-top: 1px solid #3366CC;font-size: 130%;font-weight: bold;margin: 2em 0 0 -10px;padding: 1px 3px;}");
			out.println("td {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 6px 12px;text-align: left;vertical-align: top;}");
			out.println("img {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 20px 20px;}");
			out.println("</style></head>");

			out.println("<body>");
			
			out.println("<table>");
			// Table headings
			
			out.print("<tr><th>Dataset</th>");
			for(String metricName : metricNames) {
				out.print("<th>" + metricName + "+</th>");
				out.print("<th>" + metricName + "-</th>");
			}
			out.println("</tr>");
			
			
			
			for(Dataset dataset : result) {
				
				out.print("<tr><td>" + dataset.getName() + "</td>");

				Map<String, Integer> posAggregation = new HashMap<String, Integer>();
				Map<String, Integer> negAggregation = new HashMap<String, Integer>();
				Map<String, Integer> posAggregationTotal = new HashMap<String, Integer>();
				Map<String, Integer> negAggregationTotal = new HashMap<String, Integer>();

				for(String metricName : metricNames) {
					posAggregation.put(metricName, 0);
					negAggregation.put(metricName, 0);
					posAggregationTotal.put(metricName, 0);
					negAggregationTotal.put(metricName, 0);
					
					
					
					for(DatasetReport dr : dataset.getPositive().getReports()) {
						
						
						MetricReport mr = dr.getMetricReports().get(metricName);
						if(mr == null) {
							continue;
						}

						boolean isCorrect = mr.getColor().equals(Color.GREEN);
						if(isCorrect) {
							MapUtils.increment(posAggregation, mr.getName());
						}
						MapUtils.increment(posAggregationTotal, mr.getName());
					}

					for(DatasetReport dr : dataset.getNegative().getReports()) {
						
						
						MetricReport mr = dr.getMetricReports().get(metricName);
						if(mr == null) {
							continue;
						}

						boolean isCorrect = mr.getColor().equals(Color.RED);
						if(isCorrect) {
							MapUtils.increment(negAggregation, mr.getName());
						}
						MapUtils.increment(negAggregationTotal, mr.getName());
					}

				}
			
		
				for(String metricName : metricNames) {
					int posDenom = posAggregationTotal.get(metricName);
					int negDenom = negAggregationTotal.get(metricName);
					
					Double posRatio = posDenom == 0 ? null : posAggregation.get(metricName) / (double)posDenom;
					Double negRatio = negDenom == 0 ? null : negAggregation.get(metricName) / (double)negDenom;
					
					
					
					
					out.print("<td style='background-color:" + ratioToColor(posRatio) + ";'>" + posAggregation.get(metricName) + "/" + posAggregationTotal.get(metricName) + "</td>");
					out.print("<td style='background-color:" + ratioToColor(negRatio) + ";'>" + negAggregation.get(metricName) + "/" + negAggregationTotal.get(metricName) + "</td>");
				}

				out.println("</tr>");
			}
			
			out.println("</table>");
			
			out.println("</body>");

			out.flush();
			
			
			// Alternatively, we could want to see each metric individually
			
		}
		
	
	
	public static void posnegRepo(String args[])
		throws Exception
	{
		Map<String, String> map = IniUtils.loadIniFile(new File("config"));
		
		String posNegRepoStr = map.get("posNegRepo");
		
		File posNegRepo = new File(posNegRepoStr);
		
		List<Dataset> result = new ArrayList<Dataset>();
		
				
		for(File entry : posNegRepo.listFiles()) {
			//System.out.println("Candidate: " + entry.getName());
			

			Dataset dataset = new Dataset(entry.getName());
			
			collect(entry, dataset, "positive");
			collect(entry, dataset, "negative");			
			
			result.add(dataset);
		}

		
		Set<String> metricNames = new TreeSet<String>();
		for(Dataset dataset : result) {
			for(DatasetReport dr : dataset.getPositive().getReports()) {
				metricNames.addAll(dr.getMetricReports().keySet());
			}
			
			for(DatasetReport dr : dataset.getNegative().getReports()) {
				metricNames.addAll(dr.getMetricReports().keySet());
			}
		}

		
		// Do the aggregation and create the following table:
		// Dataset Metric correctGreens totalGreens correctReds totalReds
		
		PrintStream out = System.out;

		
		out.println("<!DOCTYPE HTML><html><head><title>Evaluation Results</title>");
		out.println("<style type=\"text/css\">");
		out.println("body {color: #000000;font-family: Helvetica,Arial,sans-serif;font-size: small;}");
		out.println("h1 {background-color: #E5ECF9;border-top: 1px solid #3366CC;font-size: 130%;font-weight: bold;margin: 2em 0 0 -10px;padding: 1px 3px;}");
		out.println("td {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 6px 12px;text-align: left;vertical-align: top;}");
		out.println("img {background-color: #FFFFFF;border: 1px solid #BBBBBB;padding: 20px 20px;}");
		out.println("</style></head>");

		out.println("<body>");
		
		out.println("<table>");
		// Table headings
		
		out.print("<tr><th>Dataset</th>");
		for(String metricName : metricNames) {
			out.print("<th>" + metricName + "+</th>");
			out.print("<th>" + metricName + "-</th>");
		}
		out.println("</tr>");
		
		
		
		for(Dataset dataset : result) {
			
			out.print("<tr><td>" + dataset.getName() + "</td>");

			Map<String, Integer> posAggregation = new HashMap<String, Integer>();
			Map<String, Integer> negAggregation = new HashMap<String, Integer>();
			Map<String, Integer> posAggregationTotal = new HashMap<String, Integer>();
			Map<String, Integer> negAggregationTotal = new HashMap<String, Integer>();

			for(String metricName : metricNames) {
				posAggregation.put(metricName, 0);
				negAggregation.put(metricName, 0);
				posAggregationTotal.put(metricName, 0);
				negAggregationTotal.put(metricName, 0);
				
				
				
				for(DatasetReport dr : dataset.getPositive().getReports()) {
					
					
					MetricReport mr = dr.getMetricReports().get(metricName);
					if(mr == null) {
						continue;
					}

					boolean isCorrect = mr.getColor().equals(Color.GREEN);
					if(isCorrect) {
						MapUtils.increment(posAggregation, mr.getName());
					}
					MapUtils.increment(posAggregationTotal, mr.getName());
				}

				for(DatasetReport dr : dataset.getNegative().getReports()) {
					
					
					MetricReport mr = dr.getMetricReports().get(metricName);
					if(mr == null) {
						continue;
					}

					boolean isCorrect = mr.getColor().equals(Color.RED);
					if(isCorrect) {
						MapUtils.increment(negAggregation, mr.getName());
					}
					MapUtils.increment(negAggregationTotal, mr.getName());
				}

			}
		
	
			for(String metricName : metricNames) {
				int posDenom = posAggregationTotal.get(metricName);
				int negDenom = negAggregationTotal.get(metricName);
				
				Double posRatio = posDenom == 0 ? null : posAggregation.get(metricName) / (double)posDenom;
				Double negRatio = negDenom == 0 ? null : negAggregation.get(metricName) / (double)negDenom;
				
				
				
				
				out.print("<td style='background-color:" + ratioToColor(posRatio) + ";'>" + posAggregation.get(metricName) + "/" + posAggregationTotal.get(metricName) + "</td>");
				out.print("<td style='background-color:" + ratioToColor(negRatio) + ";'>" + negAggregation.get(metricName) + "/" + negAggregationTotal.get(metricName) + "</td>");
			}

			out.println("</tr>");
		}
		
		out.println("</table>");
		
		out.println("</body>");

		out.flush();
		
		
		// Alternatively, we could want to see each metric individually
		
	}
	
	
	public static String ratioToColor(Double ratio) {
		
		if(ratio == null) {
			return "#dddddd";
		} else if(ratio > 0.5) {
			return "#aaffaa";
			//double scale = Math.max(aratio * 
			//return StringUtils.bytesToHexString(bytes)
		} else {
			return "#ffaaaa";
		}
	}
	


	private static void collect(File entry, Dataset dataset, String type)
		throws Exception
	{
			
			File typeDir = new File(entry.getAbsoluteFile() + "/" + type);
			
			if(!typeDir.exists()) {
				return;
			}
			//System.out.println("Type: " + typeDir.getName());

			
			EvalRun evalRun;
			
			if(type.equals("positive")) {
				evalRun = dataset.getPositive();
			} else if(type.equals("negative")) {
				evalRun = dataset.getNegative();
			} else {
				throw new RuntimeException("Unknown type: " + type);
			}
			
			Map<Integer, File> sorted = new TreeMap<Integer, File>(); //(new StringPrettyComparator());
			for(File item : typeDir.listFiles()) {
				try {
					sorted.put(Integer.parseInt(item.getName()), item);
				} catch(NumberFormatException e) {
					System.out.println("Filename must be integer - Skipping: " + item.getAbsolutePath());
				}
				
			}
			
			
			//int i = 1;
			for(Entry<Integer, File> e : sorted.entrySet()) {
				int i = e.getKey();
				File item = e.getValue();
				

				//System.out.println("Run: " + item.getName());

				File report = new File(item.getAbsoluteFile() + "/" + "report.html");
				if(!report.exists()) {
					continue;
				}
				

				DatasetReport datasetReport = new DatasetReport(dataset.getName());
				
				String cmd = "./extract-status-from-html-report.sh " + report.getAbsolutePath();
				Process process = Runtime.getRuntime().exec(cmd); 
				InputStream in = process.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				while((line = reader.readLine()) != null) {
					String[] parts = line.split("\\s+");
					if(parts.length < 3) {
						System.err.println("Error in line: " + line);
						continue;
					}

					String metricName = parts[0];
					Double change = Double.parseDouble(parts[2]);
					
					String colorStr = parts[1].trim().toLowerCase();
					Color color = null;
					if(colorStr.equals("green")) {
						color = Color.GREEN;
					} else if(colorStr.equals("red")) {
						color = Color.RED;
					}
					
					MetricReport metricReport = new MetricReport(metricName, change, color);
					
					System.out.println(item.getName() + ": " +  dataset.getName() + "/" + type + "/" + i + ": " + metricName + ", " + change + ", " + color);
					
					datasetReport.add(metricReport);
				}
				
			
				evalRun.getReports().add(datasetReport);
				++i;
				
				System.out.println();
			}
		
		}
		
	}
