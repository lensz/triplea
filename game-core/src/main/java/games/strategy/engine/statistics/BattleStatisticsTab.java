package games.strategy.engine.statistics;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.history.Round;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XChartPanel;
import org.triplea.util.Triple;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

class BattleStatisticsTab extends JPanel {
    BattleStatisticsTab(Statistics.BattleStatistics statistics) {

        JPanel jPanel = new JPanel(new GridLayout(2,2));
        jPanel.add(createBattleTypesChart(statistics));
        jPanel.add(createMostContestTerritoryPanel(statistics));
        jPanel.add(createTuvLossesPanel(statistics));
        jPanel.add(createBiggestBattlePanel(statistics));
        this.add(jPanel);
    }

    private JTextArea createTuvLossesPanel(Statistics.BattleStatistics statistics) {
        return new JTextArea(
                String.format("TUV lost by attackers:\t%.0f\nTUV lost by defenders:\t%.0f",
                        statistics.getTotalUnitsLostAttacker(),
                        statistics.getTotalUnitsLostDefender()
                )
        );
    }

    private XChartPanel<PieChart> createBattleTypesChart(Statistics.BattleStatistics statistics) {
        PieChartBuilder pieChartBuilder = new PieChartBuilder()
                .title("Battle Types")
                .width(50)
                .height(50);
        PieChart chart = pieChartBuilder.build();
        statistics.getBattleTypeCount().forEach(chart::addSeries);
        return new XChartPanel<>(chart);
    }

    private JScrollPane createTablePanel(List<?> entries, List<String> columnNames, BiFunction<Integer, Integer, Object> getValueAtXY, String title) {
        JTable table = new JTable();
        table.setModel(new AbstractTableModel() {
            @Override
            public String getColumnName(int i) {
                return columnNames.get(i);
            }

            @Override
            public int getRowCount() {
                return entries.size();
            }

            @Override
            public int getColumnCount() {
                return columnNames.size();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return getValueAtXY.apply(columnIndex, rowIndex);
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        scrollPane.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        title,
                        TitledBorder.CENTER,
                        TitledBorder.TOP
                )
        );
        return scrollPane;
    }

    private JScrollPane createMostContestTerritoryPanel(Statistics.BattleStatistics statistics) {
        List<Map.Entry<String, Double>> battleSitesByNumberOfBattles = statistics.getBattleSiteCount().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return createTablePanel(
                battleSitesByNumberOfBattles,
                List.of("Territory", "#Battles"),
                (columnIndex, rowIndex) -> {
                    if (columnIndex == 0) {
                        return battleSitesByNumberOfBattles.get(rowIndex).getKey();
                    }
                    return battleSitesByNumberOfBattles.get(rowIndex).getValue();
                },
                "Most contested Territory"
        );
    }

    private JScrollPane createBiggestBattlePanel(Statistics.BattleStatistics statistics) {
        List<Map.Entry<Triple<Round, Territory, GamePlayer>, Double>> tuvPerBattle = statistics.getInitialTuvBattle().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return createTablePanel(
                tuvPerBattle,
                List.of("Battle", "TUV army size"),
                (columnIndex, rowIndex) -> {
                    if (columnIndex == 0) {
                        Triple<Round, Territory, GamePlayer> battle = tuvPerBattle.get(rowIndex).getKey();
                        return battle.getFirst().getTitle() + " : " + battle.getSecond().toString();
                    }
                    return tuvPerBattle.get(rowIndex).getValue();
                },
                "Biggest battle"
        );
    }
}
