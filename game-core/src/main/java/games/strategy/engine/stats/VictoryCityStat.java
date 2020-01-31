package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.attachments.TerritoryAttachment;
import java.util.Objects;

public class VictoryCityStat extends AbstractStat {
  @Override
  public String getName() {
    return "VC";
  }

  @Override
  public double getValue(final PlayerID player, final GameData data) {
    // return sum of victory cities
    return data.getMap().getTerritories().stream()
        .filter(place -> place.getOwner().equals(player))
        .map(TerritoryAttachment::get)
        .filter(Objects::nonNull)
        .mapToInt(TerritoryAttachment::getVictoryCity)
        .sum();
  }
}
