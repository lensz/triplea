package games.strategy.engine.statistics;

import games.strategy.engine.history.History;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import javax.swing.*;

public class StatisticsDialog extends JPanel {

    public StatisticsDialog(History history) {
        double[] xData = new double[] {0.0, 1.0, 2.0};
        double[] yData = new double[] {2.0, 1.0, 40.0};

        XYChart chart = QuickChart.getChart("Sample Chart", "X", "Y", "y(x)", xData, yData);

        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);
        this.add(chartPanel);
    }

}
