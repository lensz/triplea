package games.strategy.engine.statistics;

import games.strategy.engine.data.GameData;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import java.util.Map;

public class StatisticsDialog extends JPanel {

    public StatisticsDialog(GameData gameData) {
        Map<Statistic, Map<String, double[]>> statisticsData = Statistics.calculateGameStatisticsOverRounds(gameData);

        JLabel numberOfRounds = new JLabel("Number of rounds: " + Statistics.calculateNumberOfRounds(gameData.getHistory()));

        JTabbedPane jTabbedPane = new JTabbedPane();
        {
            jTabbedPane.addTab("General", numberOfRounds);
            jTabbedPane.addTab(
                    "TUVs over time",
                    createSimpleStatPanel(
                            gameData,
                            statisticsData,
                            "TUV Overview",
                            "TUV",
                            Statistic.PredefinedStatistic.TUV
                    )
            );
            jTabbedPane.addTab(
                    "Production over time",
                    createSimpleStatPanel(
                            gameData,
                            statisticsData,
                            "Production Overview",
                            "Production",
                            Statistic.PredefinedStatistic.PRODUCTION
                    )
            );
            jTabbedPane.addTab(
                    "Units over time",
                    createSimpleStatPanel(
                            gameData,
                            statisticsData,
                            "Units Overview",
                            "Units",
                            Statistic.PredefinedStatistic.UNITS
                    )
            );
            jTabbedPane.addTab(
                    "VCs over time",
                    createSimpleStatPanel(
                            gameData,
                            statisticsData,
                            "Victory City Overview",
                            "Victory Cities",
                            Statistic.PredefinedStatistic.VICTORY_CITY
                    )
            );
            jTabbedPane.addTab(
                    "VPs over time",
                    createSimpleStatPanel(
                            gameData,
                            statisticsData,
                            "VP Overview",
                            "VPs",
                            Statistic.PredefinedStatistic.VP
                    )
            );
            gameData.getResourceList().getResources()
                    .forEach(resource ->
                        jTabbedPane.addTab(
                                String.format("%s over time", resource.getName()),
                                createSimpleStatPanel(
                                        gameData,
                                        statisticsData,
                                        String.format("%s Overview", resource.getName()),
                                        String.format("%ss", resource.getName()),
                                        new Statistic.ResourceStatistic(resource)
                                )
                        )
                    );
        }
        jTabbedPane.addTab("Battle statistics", new BattleStatisticsTab(Statistics.calculateBattleStatistics(gameData)));

        this.add(jTabbedPane);
    }

    private JPanel createSimpleStatPanel(
            GameData gameData,
            Map<Statistic, Map<String, double[]>> statistics,
            String title,
            String yAxisLabel,
            Statistic statistic
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
