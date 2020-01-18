package games.strategy.engine.statistics;

import games.strategy.engine.data.GameData;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import java.util.Map;
import java.util.SortedMap;

public class StatisticsDialog extends JPanel {

    public StatisticsDialog(GameData gameData) {
        Map<Statistics.Statistic, SortedMap<String, double[]>> statisticMapMap = Statistics.calculateGameStatisticsOverRounds(gameData);

        JLabel numberOfRounds = new JLabel("Number of rounds: " + Statistics.calculateNumberOfRounds(gameData.getHistory()));

        JTabbedPane jTabbedPane = new JTabbedPane();
        jTabbedPane.addTab("General", numberOfRounds);
        jTabbedPane.addTab("TUVs over time", createTUVOverview(gameData, statisticMapMap));
        jTabbedPane.addTab("VCs over time", createVictoryCityOverview(gameData, statisticMapMap));

        this.add(jTabbedPane);
    }

    private final XYChartBuilder chartDefaults = new XYChartBuilder()
            .width(800)
            .height(600)
            .xAxisTitle("#Round")
            .theme(Styler.ChartTheme.Matlab);

    private JPanel createTUVOverview(GameData gameData, Map<Statistics.Statistic, SortedMap<String, double[]>> statistics) {
        XYChart chart = chartDefaults
                .title("TUV Overview")
                .yAxisTitle("TUV")
                .build();

        addStatisticsSeriesToChart(chart, statistics, Statistics.Statistic.TUV, gameData);
        return new XChartPanel<>(chart);
    }

    private JPanel createVictoryCityOverview(GameData gameData, Map<Statistics.Statistic, SortedMap<String, double[]>> statistics) {
        XYChart chart = chartDefaults
                .title("Victory City Overview")
                .yAxisTitle("Victory Cities")
                .build();
        addStatisticsSeriesToChart(chart, statistics, Statistics.Statistic.VICTORY_CITY, gameData);
        return new XChartPanel<>(chart);
    }

    private void addStatisticsSeriesToChart(
            XYChart chart,
            Map<Statistics.Statistic, SortedMap<String, double[]>> statistics,
            Statistics.Statistic statistic,
            GameData gameData
    ) {
        double[] rounds = createRoundXAxisValues(gameData);
        for (Map.Entry<String, double[]> entry : statistics.get(statistic).entrySet()) {
            chart.addSeries(entry.getKey(), rounds, entry.getValue());
        }
    }

    private double[] createRoundXAxisValues(GameData gameData) {
        int currentRound = gameData.getCurrentRound();
        double[] result = new double[currentRound];
        for (int i = 0; i < currentRound; i++) {
            result[i] = i + 1;
        }
        return result;
    }
}
