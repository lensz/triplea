package games.strategy.triplea.ai.proAI.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.data.ProPlaceTerritory;
import games.strategy.triplea.ai.proAI.data.ProPurchaseOption;
import games.strategy.triplea.ai.proAI.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.proAI.data.ProResourceTracker;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.ai.proAI.simulate.ProDummyDelegateBridge;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.AbstractPlaceDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

/**
 * Pro AI purchase utilities.
 */
public class ProPurchaseUtils {

  public static List<ProPurchaseOption> findPurchaseOptionsForTerritory(final PlayerID player,
      final List<ProPurchaseOption> purchaseOptions, final Territory t) {
    final List<ProPurchaseOption> result = new ArrayList<ProPurchaseOption>();
    for (final ProPurchaseOption ppo : purchaseOptions) {
      if (canTerritoryUsePurchaseOption(player, ppo, t)) {
        result.add(ppo);
      }
    }
    return result;
  }

  public static boolean canTerritoryUsePurchaseOption(final PlayerID player, final ProPurchaseOption ppo,
      final Territory t) {
    if (ppo == null) {
      return false;
    }
    final List<Unit> units = ppo.getUnitType().create(ppo.getQuantity(), player, true);
    return canUnitsBePlaced(units, player, t);
  }

  public static boolean canUnitsBePlaced(final List<Unit> units, final PlayerID player, final Territory t) {
    final GameData data = ProData.getData();

    final AbstractPlaceDelegate placeDelegate = (AbstractPlaceDelegate) data.getDelegateList().getDelegate("place");
    final IDelegateBridge bridge = new ProDummyDelegateBridge(ProData.getProAI(), player, data);
    placeDelegate.setDelegateBridgeAndPlayer(bridge);
    final String s = placeDelegate.canUnitsBePlaced(t, units, player);
    if (s == null) {
      return true;
    }
    return false;
  }

  public static List<ProPurchaseOption> removeInvalidPurchaseOptions(final PlayerID player, final GameData data,
      final List<ProPurchaseOption> purchaseOptions, final ProResourceTracker resourceTracker,
      final int remainingUnitProduction, final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    for (final Iterator<ProPurchaseOption> it = purchaseOptions.iterator(); it.hasNext();) {
      final ProPurchaseOption purchaseOption = it.next();

      // Check PU cost and production
      if (!resourceTracker.hasEnough(purchaseOption) || purchaseOption.getQuantity() > remainingUnitProduction) {
        it.remove();
        continue;
      }

      // Check max unit limits (-1 is unlimited)
      final int maxBuilt = purchaseOption.getMaxBuiltPerPlayer();
      final UnitType type = purchaseOption.getUnitType();
      if (maxBuilt == 0) {
        it.remove();
      } else if (maxBuilt > 0) {

        // Find number of unit type that are already built and about to be placed
        int currentlyBuilt = 0;
        final CompositeMatch<Unit> unitTypeOwnedBy =
            new CompositeMatchAnd<Unit>(Matches.unitIsOfType(type), Matches.unitIsOwnedBy(player));
        final List<Territory> allTerritories = data.getMap().getTerritories();
        for (final Territory t : allTerritories) {
          currentlyBuilt += t.getUnits().countMatches(unitTypeOwnedBy);
        }
        currentlyBuilt += Match.countMatches(unitsToPlace, unitTypeOwnedBy);
        for (final Territory t : purchaseTerritories.keySet()) {
          for (final ProPlaceTerritory placeTerritory : purchaseTerritories.get(t).getCanPlaceTerritories()) {
            currentlyBuilt += Match.countMatches(placeTerritory.getPlaceUnits(), unitTypeOwnedBy);
          }
        }
        final int allowedBuild = maxBuilt - currentlyBuilt;
        if (allowedBuild - purchaseOption.getQuantity() < 0) {
          it.remove();
        }
      }
    }
    return purchaseOptions;
  }

  public static ProPurchaseOption randomizePurchaseOption(final Map<ProPurchaseOption, Double> purchaseEfficiencies,
      final String type) {

    ProLogger.trace("Select purchase option for " + type);
    double totalEfficiency = 0;
    for (final Double efficiency : purchaseEfficiencies.values()) {
      totalEfficiency += efficiency;
    }
    final Map<ProPurchaseOption, Double> purchasePercentages = new LinkedHashMap<ProPurchaseOption, Double>();
    double upperBound = 0.0;
    for (final ProPurchaseOption ppo : purchaseEfficiencies.keySet()) {
      final double chance = purchaseEfficiencies.get(ppo) / totalEfficiency * 100;
      upperBound += chance;
      purchasePercentages.put(ppo, upperBound);
      ProLogger.trace(ppo.getUnitType().getName() + ", probability=" + chance + ", upperBound=" + upperBound);
    }
    final double randomNumber = Math.random() * 100;
    ProLogger.trace("Random number: " + randomNumber);
    for (final ProPurchaseOption ppo : purchasePercentages.keySet()) {
      if (randomNumber <= purchasePercentages.get(ppo)) {
        return ppo;
      }
    }
    return purchasePercentages.keySet().iterator().next();
  }

  public static List<Unit> findMaxPurchaseDefenders(final PlayerID player, final Territory t,
      final List<ProPurchaseOption> landPurchaseOptions) {

    ProLogger.info("Find max purchase defenders for " + t.getName());
    final GameData data = ProData.getData();

    // Determine most cost efficient defender that can be produced in this territory
    final Resource PUs = data.getResourceList().getResource(Constants.PUS);
    final int PUsRemaining = player.getResources().getQuantity(PUs);
    final List<ProPurchaseOption> purchaseOptionsForTerritory =
        findPurchaseOptionsForTerritory(player, landPurchaseOptions, t);
    ProPurchaseOption bestDefenseOption = null;
    double maxDefenseEfficiency = 0;
    for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
      if (ppo.getDefenseEfficiency() > maxDefenseEfficiency && ppo.getCost() <= PUsRemaining) {
        bestDefenseOption = ppo;
        maxDefenseEfficiency = ppo.getDefenseEfficiency();
      }
    }

    // Determine number of defenders I can purchase
    final List<Unit> placeUnits = new ArrayList<Unit>();
    if (bestDefenseOption != null) {
      ProLogger.debug("Best defense option: " + bestDefenseOption.getUnitType().getName());
      int remainingUnitProduction = getUnitProduction(t, data, player);
      int PUsSpent = 0;
      while (true) {

        // If out of PUs or production then break
        if (bestDefenseOption.getCost() > (PUsRemaining - PUsSpent)
            || remainingUnitProduction < bestDefenseOption.getQuantity()) {
          break;
        }

        // Create new temp defenders
        PUsSpent += bestDefenseOption.getCost();
        remainingUnitProduction -= bestDefenseOption.getQuantity();
        placeUnits.addAll(bestDefenseOption.getUnitType().create(bestDefenseOption.getQuantity(), player, true));
      }
      ProLogger.debug("Potential purchased defenders: " + placeUnits);
    }
    return placeUnits;
  }

  public static Map<Territory, ProPurchaseTerritory> findPurchaseTerritories(final PlayerID player) {

    ProLogger.info("Find all purchase territories");
    final GameData data = ProData.getData();

    // Find all territories that I can place units on
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    List<Territory> ownedAndNotConqueredFactoryTerritories = new ArrayList<Territory>();
    if (ra != null && ra.getPlacementAnyTerritory()) {
      ownedAndNotConqueredFactoryTerritories = data.getMap().getTerritoriesOwnedBy(player);
    } else {
      ownedAndNotConqueredFactoryTerritories =
          Match.getMatches(data.getMap().getTerritories(),
              ProMatches.territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
    }
    ownedAndNotConqueredFactoryTerritories =
        Match.getMatches(ownedAndNotConqueredFactoryTerritories,
            ProMatches.territoryCanMoveLandUnits(player, data, false));

    // Create purchase territory holder for each factory territory
    final Map<Territory, ProPurchaseTerritory> purchaseTerritories = new HashMap<Territory, ProPurchaseTerritory>();
    for (final Territory t : ownedAndNotConqueredFactoryTerritories) {
      final int unitProduction = getUnitProduction(t, data, player);
      final ProPurchaseTerritory ppt = new ProPurchaseTerritory(t, data, player, unitProduction);
      purchaseTerritories.put(t, ppt);
      ProLogger.debug(ppt.toString());
    }
    return purchaseTerritories;
  }

  public static int getUnitProduction(final Territory territory, final GameData data, final PlayerID player) {

    final CompositeMatchAnd<Unit> factoryMatch =
        new CompositeMatchAnd<Unit>(Matches.UnitIsOwnedAndIsFactoryOrCanProduceUnits(player), Matches
            .unitIsBeingTransported().invert());
    if (territory.isWater()) {
      factoryMatch.add(Matches.UnitIsLand.invert());
    } else {
      factoryMatch.add(Matches.UnitIsSea.invert());
    }
    final Collection<Unit> factoryUnits = territory.getUnits().getMatches(factoryMatch);
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    final boolean originalFactory = (ta == null ? false : ta.getOriginalFactory());
    final boolean playerIsOriginalOwner =
        factoryUnits.size() > 0 ? player.equals(getOriginalFactoryOwner(territory, player)) : false;
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    if (originalFactory && playerIsOriginalOwner) {
      if (ra != null && ra.getMaxPlacePerTerritory() != -1) {
        return Math.max(0, ra.getMaxPlacePerTerritory());
      }
      return Integer.MAX_VALUE;
    }
    if (ra != null && ra.getPlacementAnyTerritory()) {
      return Integer.MAX_VALUE;
    }
    return TripleAUnit.getProductionPotentialOfTerritory(territory.getUnits().getUnits(), territory, player, data,
        true, true);
  }

  private static PlayerID getOriginalFactoryOwner(final Territory territory, final PlayerID player) {

    final Collection<Unit> factoryUnits = territory.getUnits().getMatches(Matches.UnitCanProduceUnits);
    if (factoryUnits.size() == 0) {
      throw new IllegalStateException("No factory in territory:" + territory);
    }
    final Iterator<Unit> iter = factoryUnits.iterator();
    while (iter.hasNext()) {
      final Unit factory2 = iter.next();
      if (player.equals(OriginalOwnerTracker.getOriginalOwner(factory2))) {
        return OriginalOwnerTracker.getOriginalOwner(factory2);
      }
    }
    final Unit factory = factoryUnits.iterator().next();
    return OriginalOwnerTracker.getOriginalOwner(factory);
  }

  /**
   * @return a comparator that sorts cheaper units before expensive ones
   */
  public static Comparator<Unit> getCostComparator() {
    return new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        return Double.compare(getCost(o1.getType(), o1.getOwner(), o1.getData()),
            getCost(o2.getType(), o2.getOwner(), o2.getData()));
      }
    };
  }

  /**
   * How many PU's does it cost the given player to produce the given unit type.
   * <p>
   * If the player cannot produce the given unit, return Integer.MAX_VALUE
   * <p>
   */
  public static double getCost(final UnitType unitType, final PlayerID player, final GameData data) {
    if (unitType == null) {
      throw new IllegalArgumentException("null unit type");
    }
    if (player == null) {
      throw new IllegalArgumentException("null player id");
    }
    final Resource PUs = data.getResourceList().getResource(Constants.PUS);
    final ProductionRule rule = getProductionRule(unitType, player, data);
    if (rule == null) {
      return ProData.unitValueMap.getInt(unitType);
    } else {
      return ((double) rule.getCosts().getInt(PUs)) / rule.getResults().totalValues();
    }
  }

  /**
   * Get the production rule for the given player, for the given unit type.
   * <p>
   * If no such rule can be found, then return null.
   */
  public static ProductionRule getProductionRule(final UnitType unitType, final PlayerID player, final GameData data) {
    if (unitType == null) {
      throw new IllegalArgumentException("null unit type");
    }
    if (player == null) {
      throw new IllegalArgumentException("null player id");
    }
    final ProductionFrontier frontier = player.getProductionFrontier();
    if (frontier == null) {
      return null;
    }
    for (final ProductionRule rule : frontier) {
      if (rule.getResults().getInt(unitType) > 0) {
        return rule;
      }
    }
    return null;
  }

  public static List<Unit> getPlaceUnits(final Territory t,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    final List<Unit> placeUnits = new ArrayList<Unit>();
    if (purchaseTerritories == null) {
      return placeUnits;
    }
    for (final Territory purchaseTerritory : purchaseTerritories.keySet()) {
      for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories()) {
        if (t.equals(ppt.getTerritory())) {
          placeUnits.addAll(ppt.getPlaceUnits());
        }
      }
    }
    return placeUnits;
  }
}
