package games.strategy.triplea.ui.statistics;

import com.google.common.collect.Table;
import games.strategy.engine.data.GameData;
import games.strategy.engine.history.Round;
import games.strategy.engine.stats.Statistics;
import games.strategy.engine.stats.StatisticsAggregator;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import javax.swing.*;

public class StatisticsDialog extends JPanel {

  @Getter
  @RequiredArgsConstructor
  private static class OverTimeChart {
    private final String title;
    private final String axisTitle;
    private final Table<String, Round, Double> data;
  }

  private final XYChartBuilder xyChartDefaults =
      new XYChartBuilder().theme(Styler.ChartTheme.Matlab).xAxisTitle("#Rounds");

  public StatisticsDialog(final GameData game) {
    final Statistics statistics = new StatisticsAggregator(game).aggregate();

    final List<OverTimeChart> overTimeCharts = new ArrayList<>();
    overTimeCharts.add(new OverTimeChart(
                "Production",
                "Production from territories",
                statistics.getProductionOfPlayerInRound()));
    overTimeCharts.add(new OverTimeChart("TUV", "TUV", statistics.getTuvOfPlayerInRound()));
    overTimeCharts.add(new OverTimeChart("Units", "Units", statistics.getUnitsOfPlayerInRound()));
    overTimeCharts.add(new OverTimeChart(
                "VC", "Victory Cities", statistics.getVictoryCitiesOfPlayerInRound()));

    final JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Lines", createDummyXyGraph(statistics));
    tabbedPane.addTab("Pie", createDummyPieChart(statistics));
    for (final OverTimeChart chartData : overTimeCharts) {
      tabbedPane.addTab(chartData.getTitle(), createChart(chartData));
    }
    this.add(tabbedPane);
  }

  private JPanel createChart(final OverTimeChart chartData) {
    final XYChart chart =
        xyChartDefaults.title(chartData.getTitle()).yAxisTitle(chartData.getAxisTitle()).build();
    chartData
        .getData()
        .rowMap()
        .forEach((key, value) -> chart.addSeries(key, new ArrayList<>(value.values())));
    return new XChartPanel<>(chart);
  }

  private JPanel createDummyXyGraph(final Statistics statistics) {
    final XYChart chart = xyChartDefaults.title("Sample Chart: " + statistics.toString()).build();
    chart.addSeries("some value1", new double[] {2.0, 0.0, 40.0});
    chart.addSeries("some value2", new double[] {3.0, 1.0, 41.0});
    chart.addSeries("some value3", new double[] {4.0, 2.0, 42.0});
    chart.addSeries("some value4", new double[] {5.0, 3.0, 43.0});
    chart.addSeries("some value5", new double[] {6.0, 4.0, 44.0});
    chart.addSeries("some value6", new double[] {7.0, 5.0, 45.0});
    chart.addSeries("some value7", new double[] {8.0, 6.0, 46.0});
    return new XChartPanel<>(chart);
  }

  private JPanel createDummyPieChart(final Statistics statistics) {
    final PieChart chart =
        new PieChartBuilder()
            .theme(Styler.ChartTheme.XChart)
            .title("Sample Chart: " + statistics.toString())
            .build();
    chart.addSeries("Value 1", 27);
    chart.addSeries("Value 2", 63);
    chart.addSeries("Value 3", 1);
    chart.addSeries("Value 4", 9);
    chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSE);
    return new XChartPanel<>(chart);
  }
}
