package games.strategy.engine.statistics;

import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XChartPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BattleStatisticsTab extends JPanel {
    BattleStatisticsTab(Statistics.BattleStatistics statistics) {
        this.add(createBattleTypesChart(statistics));
        this.add(createMostContestTerritoryPanel(statistics));
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
}
