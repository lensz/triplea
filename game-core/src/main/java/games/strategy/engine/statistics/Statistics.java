package games.strategy.engine.statistics;

import games.strategy.engine.data.*;
import games.strategy.engine.history.*;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.delegate.data.BattleRecords;
import games.strategy.triplea.ui.AbstractStatPanel;
import games.strategy.triplea.ui.StatPanel;
import games.strategy.triplea.util.TuvUtils;
import lombok.*;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

import javax.swing.tree.TreeNode;
import java.util.*;
import java.util.stream.Collectors;

class Statistics {

    @Getter
    @Setter
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    static class BattleStatistics {
        private Map<String, Double> battleTypeCount = new HashMap<>();
        private Map<String, Double> battleSiteCount = new HashMap<>();
        private double totalUnitsLostAttacker = 0;
        private double totalUnitsLostDefender = 0;
        private Map<GamePlayer, Map<UnitType, Integer>> casualtiesPerPlayerPerUnitType = new LinkedHashMap<>();
        /**
         * Map<"Battle", "initial tuv of participating units attacker + defender">
         * Battle = <battleTime, battleSite, attacker>
         */
        private Map<Triple<Round, Territory, GamePlayer>, Double> initialTuvBattle = new HashMap<>();
    }

    private static class BattleStatMeasurer {

        private final BattleStatistics workingOn = new BattleStatistics();

        void lookAt(BattleRecord battle) {
            String battleType = battle.getBattleType().name();
            Map<String, Double> battleTypeCount = workingOn.getBattleTypeCount();
            double battleTypeCountValue = battleTypeCount.getOrDefault(battleType, 0.0);
            battleTypeCount.put(battleType, battleTypeCountValue + 1);

            String battleSite = battle.getBattleSite().getName();
            Map<String, Double> battleSiteCount = workingOn.getBattleSiteCount();
            double battleSiteCountValue = battleSiteCount.getOrDefault(battleSite, 0.0);
            battleSiteCount.put(battleSite, battleSiteCountValue + 1);

            workingOn.totalUnitsLostAttacker += battle.getAttackerLostTuv();
            workingOn.totalUnitsLostDefender += battle.getDefenderLostTuv();
        }

        BattleStatistics getStatistics() {
            return workingOn;
        }
    }

    @Getter
    private static class HistoryTraverseMeasurer {

        private final BattleStatistics workingOn = new BattleStatistics();

        private Map<GamePlayer, IntegerMap<UnitType>> unitCostMap = new HashMap<>();

        HistoryTraverseMeasurer(GameData gameData) {
            workingOn.casualtiesPerPlayerPerUnitType = new LinkedHashMap<>();
            for (GamePlayer player : gameData.getPlayerList().getPlayers()) {
                workingOn.casualtiesPerPlayerPerUnitType.put(player, new LinkedHashMap<>());

                unitCostMap.putIfAbsent(player, TuvUtils.getCostsForTuv(player, gameData));
            }
        }

        private boolean isEventChildAndUserObjectContains(HistoryNode node, String userObjectContainsCondition) {
            return node instanceof EventChild &&
                    node.getUserObject() instanceof String &&
                    ((String) node.getUserObject()).contains(userObjectContainsCondition);
        }

        void lookAt(TreeNode treeNode) {
            HistoryNode node = (HistoryNode) treeNode;
            if (isEventChildAndUserObjectContains(node, "Battle casualty summary:")) {
                EventChild battleSummary = (EventChild) node;
                List<TripleAUnit> killed = (List<TripleAUnit>) battleSummary.getRenderingData();
                for (TripleAUnit killedUnit : killed) {
                    workingOn.casualtiesPerPlayerPerUnitType.putIfAbsent(killedUnit.getOwner(), new LinkedHashMap<>());
                    workingOn.casualtiesPerPlayerPerUnitType.get(killedUnit.getOwner()).putIfAbsent(killedUnit.getType(), 0);
                    workingOn.casualtiesPerPlayerPerUnitType.get(killedUnit.getOwner()).compute(killedUnit.getType(), (tripleAUnit, integer) -> integer + 1);
                }
            }
            if (
                    isEventChildAndUserObjectContains(node, " attack with ") ||
                    isEventChildAndUserObjectContains(node, " defend with ")
            ) {
                EventChild unitSummary = (EventChild) node;
                List<Unit> units = (List<Unit>) unitSummary.getRenderingData();
                GamePlayer player = ((Step)unitSummary.getParent().getParent()).getPlayerId();
                Triple<Round, Territory, GamePlayer> battle = Triple.of(
                        (Round) unitSummary.getParent().getParent().getParent(),
                        (Territory) ((Event)unitSummary.getParent()).getRenderingData(),
                        player
                );
                workingOn.initialTuvBattle.putIfAbsent(battle, 0.0);
                Optional<Integer> tuv = units.stream()
                        .collect(Collectors.groupingBy(Unit::getOwner)).entrySet().stream()
                        .map(gamePlayerListEntry -> TuvUtils.getTuv(gamePlayerListEntry.getValue(), getUnitCostMap().get(gamePlayerListEntry.getKey())))
                        .reduce((i, j) -> i + j);

                workingOn.initialTuvBattle.compute(battle, (__, aDouble) -> aDouble + tuv.get());
            }
        }

        BattleStatistics getStatistics() {
            return workingOn;
        }
    }

    static BattleStatistics calculateBattleStatistics(GameData gameData) {
        HistoryTraverseMeasurer historyBasedMeasurer = new HistoryTraverseMeasurer(gameData);
        Enumeration<TreeNode> treeNodeEnumeration = ((HistoryNode) gameData.getHistory().getRoot())
                .breadthFirstEnumeration();
        while (treeNodeEnumeration.hasMoreElements()) {
            historyBasedMeasurer.lookAt(treeNodeEnumeration.nextElement());
        }

        BattleStatMeasurer battleRecordsMeasurer = new BattleStatMeasurer();
        BattleRecordsList battleRecordsList = gameData.getBattleRecordsList();
        battleRecordsList.getBattleRecordsMap().values().stream()
                .flatMap(brs -> BattleRecords.getAllRecords(brs).stream())
                .forEach(battleRecordsMeasurer::lookAt);
        return BattleStatistics.builder()
                .battleTypeCount(battleRecordsMeasurer.getStatistics().getBattleTypeCount())
                .battleSiteCount(battleRecordsMeasurer.getStatistics().getBattleSiteCount())
                .totalUnitsLostAttacker(battleRecordsMeasurer.getStatistics().getTotalUnitsLostAttacker())
                .totalUnitsLostDefender(battleRecordsMeasurer.getStatistics().getTotalUnitsLostDefender())

                .initialTuvBattle(historyBasedMeasurer.getStatistics().getInitialTuvBattle())
                .casualtiesPerPlayerPerUnitType(historyBasedMeasurer.getStatistics().getCasualtiesPerPlayerPerUnitType())
                .build();
    }

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
