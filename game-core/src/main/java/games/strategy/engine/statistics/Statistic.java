package games.strategy.engine.statistics;

import games.strategy.engine.data.Resource;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public interface Statistic {
    String getName();

    enum PredefinedStatistic implements Statistic {
        TUV,
        PRODUCTION,
        UNITS,
        VICTORY_CITY,
        VP;

        @Override
        public String getName() {
            return name();
        }
    }

    @Getter
    @EqualsAndHashCode
    class ResourceStatistic implements Statistic {
        private String name;

        ResourceStatistic(Resource resource) {
            this.name = resource.getName();
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
