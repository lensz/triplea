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
        jTabbedPane.addTab(
                "TUVs over time",
                createSimpleStatPanel(
                        gameData,
                        statisticMapMap,
                        "TUV Overview",
                        "TUV",
                        Statistics.Statistic.TUV
                )
        );
        jTabbedPane.addTab(
                "Production over time",
                createSimpleStatPanel(
                        gameData,
                        statisticMapMap,
                        "Production Overview",
                        "Production",
                        Statistics.Statistic.PRODUCTION
                )
        );
        jTabbedPane.addTab(
                "Units over time",
                createSimpleStatPanel(
                        gameData,
                        statisticMapMap,
                        "Units Overview",
                        "Units",
                        Statistics.Statistic.UNITS
                )
        );
        jTabbedPane.addTab(
                "VCs over time",
                createSimpleStatPanel(
                        gameData,
                        statisticMapMap,
                        "Victory City Overview",
                        "Victory Cities",
                        Statistics.Statistic.VICTORY_CITY
                )
        );
        jTabbedPane.addTab(
                "VPs over time",
                createSimpleStatPanel(
                        gameData,
                        statisticMapMap,
                        "VP Overview",
                        "VPs",
                        Statistics.Statistic.VP
                )
        );

        this.add(jTabbedPane);
    }

    private JPanel createSimpleStatPanel(
            GameData gameData,
            Map<Statistics.Statistic, SortedMap<String, double[]>> statistics,
            String title,
            String yAxisLabel, Statistics.Statistic statistic
    ) {
        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title(title)
                .xAxisTitle("#Round")
                .yAxisTitle(yAxisLabel)
                .theme(Styler.ChartTheme.Matlab)
                .build();
        double[] rounds = createRoundXAxisValues(gameData);
        for (Map.Entry<String, double[]> entry : statistics.get(statistic).entrySet()) {
            chart.addSeries(entry.getKey(), rounds, entry.getValue());
        }
        return new XChartPanel<>(chart);
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
