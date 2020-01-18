package games.strategy.engine.statistics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.history.History;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.triplea.Constants;

import javax.swing.tree.TreeNode;
import java.util.*;

class Statistics {

    static int calculateNumberOfRounds(History history) {
        HistoryNode root = (HistoryNode) history.getRoot();
        return root.getChildCount();
    }

    static Map<GamePlayer, int[]> calculatePuOverview(GameData gameData) {
        List<Round> rounds = getRounds(gameData);

        List<GamePlayer> players = gameData.getPlayerList().getPlayers();
        Resource resource = gameData.getResourceList().getResource(Constants.PUS);

        Map<GamePlayer, int[]> puStat = new HashMap<>();
        players.forEach((player) -> puStat.put(player, new int[rounds.size()]));

        for (Round round : rounds) {
            gameData.getHistory().gotoNode(round);

            for (GamePlayer player : players) {
                System.out.println(player.getName());
                puStat.get(player)[round.getRoundNo() - 1] = player.getResources().getQuantity(resource);
            }
        }
        return puStat;
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
