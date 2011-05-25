/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.Arrays;

import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.BarChartPlot;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataUtil;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Plots;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 *
 */
public class GraphTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Data data1= DataUtil.scaleWithinRange(0, 4, Arrays.asList(1,2,3,4));
		Data data2= DataUtil.scaleWithinRange(0, 4, Arrays.asList(4,3,2,1));
		BarChartPlot plot1 = Plots.newBarChartPlot(data1, Color.BLUE, "first");
		BarChartPlot plot2 = Plots.newBarChartPlot(data2, Color.GREEN, "second");
		BarChart chart = GCharts.newBarChart(plot1, plot2);
		AxisLabels count = AxisLabelsFactory.newNumericRangeAxisLabels(0,1);
		count.setAxisStyle(AxisStyle.newAxisStyle(Color.BLACK, 13, AxisTextAlignment.CENTER));
		chart.addYAxisLabels(count);
		chart.setSize(500, 200);
		chart.setSpaceBetweenGroupsOfBars(30);
		System.out.println(chart.toURLString());

	}

}
