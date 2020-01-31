package games.strategy.engine.stats;

import games.strategy.engine.data.*;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.TuvUtils;
import java.util.function.Predicate;

import games.strategy.util.IntegerMap;

public class TuvStat extends AbstractStat {
  @Override
  public String getName() {
    return "TUV";
  }

  @Override
  public double getValue(final PlayerID player, final GameData data) {
    final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(player, data);
    final Predicate<Unit> unitIsOwnedBy = Matches.unitIsOwnedBy(player);
    return data.getMap().getTerritories().stream()
            .map(Territory::getUnits)
            .map(units -> units.getMatches(unitIsOwnedBy))
            .mapToInt(owned -> TuvUtils.getTuv(owned, costs))
            .sum();
  }
}
