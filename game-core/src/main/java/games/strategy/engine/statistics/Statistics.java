package games.strategy.engine.statistics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.history.History;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.ui.AbstractStatPanel;
import games.strategy.triplea.ui.StatPanel;

import javax.swing.tree.TreeNode;
import java.util.*;

class Statistics {


    private static final Map<Statistic, IStat> defaultGameStatisticOverRoundsMappings = Map.of(
            Statistic.PredefinedStatistic.TUV, new StatPanel.TuvStat(),
            Statistic.PredefinedStatistic.PRODUCTION, new StatPanel.ProductionStat(),
            Statistic.PredefinedStatistic.UNITS, new StatPanel.UnitsStat(),
            Statistic.PredefinedStatistic.VICTORY_CITY, new StatPanel.VictoryCityStat(),
            Statistic.PredefinedStatistic.VP, new StatPanel.VpStat()
    );

    private static Map<Statistic, IStat> createGameStatisticsOverRoundsMapping(List<Resource> resources) {
        Map<Statistic, IStat> result = new HashMap<>(defaultGameStatisticOverRoundsMappings);
        for (Resource resource : resources) {
            result.putIfAbsent(
                    new Statistic.ResourceStatistic(resource),
                    new AbstractStatPanel.ResourceStat(resource)
            );
        }
        return result;
    }

    static Map<Statistic, Map<String, double[]>> calculateGameStatisticsOverRounds(GameData gameData) {
        Map<Statistic, IStat> statisticsMapping = createGameStatisticsOverRoundsMapping(gameData.getResourceList().getResources());
        List<Round> rounds = getRounds(gameData);
        List<GamePlayer> players = gameData.getPlayerList().getPlayers();
        Set<String> alliances = gameData.getAllianceTracker().getAlliances();

        Map<Statistic, Map<String, double[]>> result = new HashMap<>();
        {
            // initialize
            statisticsMapping.keySet().forEach(
                    (statistic) ->  {
                        result.putIfAbsent(statistic, new LinkedHashMap<>());
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

            for (Map.Entry<Statistic, IStat> statistic : statisticsMapping.entrySet()) {
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
