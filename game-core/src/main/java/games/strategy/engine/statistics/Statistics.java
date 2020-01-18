package games.strategy.engine.statistics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.History;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.ui.StatPanel;

import javax.swing.tree.TreeNode;
import java.util.*;

class Statistics {

    private static final Map<Statistic, IStat> statisticMapping = Map.of(
            Statistic.TUV, new StatPanel.TuvStat(),
            Statistic.VICTORY_CITY, new StatPanel.VictoryCityStat()
    );

    enum Statistic {
        TUV,
        VICTORY_CITY
    }

    static Map<Statistic, SortedMap<String, double[]>> calculateGameStatisticsOverRounds(GameData gameData) {
        List<Round> rounds = getRounds(gameData);
        List<GamePlayer> players = gameData.getPlayerList().getPlayers();
        Set<String> alliances = gameData.getAllianceTracker().getAlliances();

        Map<Statistic, SortedMap<String, double[]>> result = new HashMap<>();
        {
            // initialize
            statisticMapping.keySet().forEach(
                    (statistic) ->  {
                        result.putIfAbsent(statistic, new TreeMap<>());
                        players.forEach(player ->
                            result.get(statistic).put(player.getName(), new double[rounds.size()])
                        );
                        alliances.forEach(alliance ->
                                result.get(statistic).put(alliance, new double[rounds.size()])
                        );
                    }
            );
        }

        for (Round round : rounds) {
            gameData.getHistory().gotoNode(round);

            for (Map.Entry<Statistic, IStat> statistic : statisticMapping.entrySet()) {
                int roundIndex = round.getRoundNo() - 1;
                for (GamePlayer player : players) {
                    result.get(statistic.getKey()).get(player.getName())[roundIndex] = statistic.getValue().getValue(player, gameData);
                }
                for (String alliance : alliances) {
                    result.get(statistic.getKey()).get(alliance)[roundIndex] = statistic.getValue().getValue(alliance, gameData);
                }
            }
        }

        return result;
    }

    static int calculateNumberOfRounds(History history) {
        HistoryNode root = (HistoryNode) history.getRoot();
        return root.getChildCount();
    }

    private static List<Round> getRounds(GameData gameData) {
        List<Round> rounds = new ArrayList<>();
        HistoryNode root = (HistoryNode) gameData.getHistory().getRoot();
        Enumeration<TreeNode> rootChildren = root.children();
        while (rootChildren.hasMoreElements()) {
            TreeNode child = rootChildren.nextElement();
            if (child instanceof Round) {
                rounds.add((Round) child);
            }
        }
        return rounds;
    }
}
