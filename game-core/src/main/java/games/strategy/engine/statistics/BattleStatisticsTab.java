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
import java.util.stream.Collectors;

class BattleStatisticsTab extends JPanel {
    BattleStatisticsTab(Statistics.BattleStatistics statistics) {
        this.add(createBattleTypesChart(statistics));

        JPanel jPanel = new JPanel(new GridLayout(3,1));
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
                .title("Battle Types");
        PieChart chart = pieChartBuilder.build();
        statistics.getBattleTypeCount().forEach(chart::addSeries);
        return new XChartPanel<>(chart);
    }

    private JScrollPane createMostContestTerritoryPanel(Statistics.BattleStatistics statistics) {
        List<Map.Entry<String, Double>> battleSitesByNumberOfBattles = statistics.getBattleSiteCount().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        JTable battleSiteTable = new JTable();
        battleSiteTable.setModel(new AbstractTableModel() {
            @Override
            public String getColumnName(int i) {
                return i == 0 ? "Territory" : "#Battles";
            }

            @Override
            public int getRowCount() {
                return battleSitesByNumberOfBattles.size();
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return battleSitesByNumberOfBattles.get(rowIndex).getKey();
                }
                return battleSitesByNumberOfBattles.get(rowIndex).getValue();
            }
        });

        JScrollPane scrollPane = new JScrollPane(battleSiteTable);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(),
                    "Most contested territory",
                    TitledBorder.CENTER,
                    TitledBorder.TOP
                )
        );
        return scrollPane;
    }

    private JScrollPane createBiggestBattlePanel(Statistics.BattleStatistics statistics) {
        List<Map.Entry<Triple<Round, Territory, GamePlayer>, Double>> tuvPerBattle = statistics.getInitialTuvBattle().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        JTable table = new JTable();
        table.setModel(new AbstractTableModel() {
            @Override
            public String getColumnName(int i) {
                return i == 0 ? "Battle" : "Total TUV army size";
            }

            @Override
            public int getRowCount() {
                return tuvPerBattle.size();
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    Triple<Round, Territory, GamePlayer> battle = tuvPerBattle.get(rowIndex).getKey();
                    return battle.getFirst().getTitle() + " : " + battle.getSecond().toString();
                }
                return tuvPerBattle.get(rowIndex).getValue();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(),
                    "Biggest battle",
                    TitledBorder.CENTER,
                    TitledBorder.TOP
                )
        );
        return scrollPane;
    }
}
