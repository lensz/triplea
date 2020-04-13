package games.strategy.engine.stats;

import com.google.common.collect.HashBasedTable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.tree.TreeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 * Analyzes a game's history and aggregates interesting statistics in a {@link Statistics} object.
 */
@Log
@RequiredArgsConstructor
public class StatisticsAggregator {
  private static final Map<OverTimeStatisticType, IStat> defaultStatisticsMapping = new HashMap<>();
  static {
      defaultStatisticsMapping.put(OverTimeStatisticType.PredefinedStatistics.TUV, new TuvStat());
      defaultStatisticsMapping.put(OverTimeStatisticType.PredefinedStatistics.PRODUCTION, new ProductionStat());
      defaultStatisticsMapping.put(OverTimeStatisticType.PredefinedStatistics.UNITS, new UnitsStat());
      defaultStatisticsMapping.put(OverTimeStatisticType.PredefinedStatistics.VC, new VictoryCityStat());
  }
  private final Statistics underConstruction = new Statistics();
  private final GameData game;

  private static Map<OverTimeStatisticType, IStat> createOverTimeStatisticsMapping(
      final List<Resource> resources) {
    final Map<OverTimeStatisticType, IStat> statisticsMapping =
        new HashMap<>(defaultStatisticsMapping);
    resources.forEach(
        resource ->
            statisticsMapping.put(
                new OverTimeStatisticType.ResourceStatistic(resource), new ResourceStat(resource)));
    return statisticsMapping;
  }

  public Statistics aggregate() {
    log.info("Aggregating statistics for game " + game.getGameName());
    collectOverTimeStatistics();
    return underConstruction;
  }

  private void collectOverTimeStatistics() {
    final Map<OverTimeStatisticType, IStat> overTimeStatisticSources =
        createOverTimeStatisticsMapping(game.getResourceList().getResources());
    {
      // initialize over time statistics
      for (final OverTimeStatisticType type : overTimeStatisticSources.keySet()) {
        underConstruction.getOverTimeStatistics().put(type, HashBasedTable.create());
      }
    }

    final List<PlayerID> players = game.getPlayerList().getPlayers();
    final List<String> alliances = new ArrayList<>(game.getAllianceTracker().getAlliances());

    for (final Round round : getRounds()) {
      game.getHistory().gotoNode(round);
      collectOverTimeStatisticsForRound(overTimeStatisticSources, players, alliances, round);
    }
  }

  private void collectOverTimeStatisticsForRound(
      final Map<OverTimeStatisticType, IStat> overTimeStatisticSources,
      final List<PlayerID> players,
      final List<String> alliances,
      final Round round) {
    for (final PlayerID player : players) {
      overTimeStatisticSources.forEach(
          (type, source) ->
              underConstruction
                  .getOverTimeStatistics()
                  .get(type)
                  .put(player.getName(), round, source.getValue(player, game)));
    }
    for (final String alliance : alliances) {
      overTimeStatisticSources.forEach(
          (type, source) ->
              underConstruction
                  .getOverTimeStatistics()
                  .get(type)
                  .put(alliance, round, source.getValue(alliance, game)));
    }
  }

  private List<Round> getRounds() {
    final List<Round> rounds = new ArrayList<>();
    final HistoryNode root = (HistoryNode) game.getHistory().getRoot();
    final Enumeration<TreeNode> rootChildren = root.children();
    while (rootChildren.hasMoreElements()) {
      final TreeNode child = rootChildren.nextElement();
      if (child instanceof Round) {
        rounds.add((Round) child);
      }
    }
    return rounds;
  }
}
