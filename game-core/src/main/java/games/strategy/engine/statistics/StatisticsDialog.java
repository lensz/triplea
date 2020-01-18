package games.strategy.engine.statistics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import java.util.Map;

public class StatisticsDialog extends JPanel {

    public StatisticsDialog(GameData gameData) {

        this.add(new JLabel("Number of rounds: " + Statistics.calculateNumberOfRounds(gameData.getHistory())));

        this.add(createPuOverview(gameData));
    }

    private JPanel createPuOverview(GameData gameData) {
        Map<GamePlayer, int[]> puStats = Statistics.calculatePuOverview(gameData);

        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title("PU Overview")
                .xAxisTitle("#Round").yAxisTitle("PUs")
                .theme(Styler.ChartTheme.Matlab)
                .build();

        int[] rounds = createRoundXAxisValues(gameData);
        for (Map.Entry<GamePlayer, int[]> entry : puStats.entrySet()) {
            chart.addSeries(entry.getKey().getName(), rounds, entry.getValue());
        }
        return new XChartPanel<>(chart);

    }

    private int[] createRoundXAxisValues(GameData gameData) {
        int currentRound = gameData.getCurrentRound();
        int[] result = new int[currentRound];
        for (int i = 0; i < currentRound; i++) {
            result[i] = i + 1;
        }
        return result;
    }
}
