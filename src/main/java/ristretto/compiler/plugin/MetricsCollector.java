package ristretto.compiler.plugin;

import ristretto.compiler.plugin.VariableFinalModifier.VariableScope;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

final class MetricsCollector implements VariableFinalModifier.Observer {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final Map<VariableScope, AtomicInteger> finalModifierAddedCount = new HashMap<>();
    private final Map<VariableScope, AtomicInteger> annotatedAsMutableCount = new HashMap<>();

    private MetricsCollector() {
    }

    static MetricsCollector newCollector() {
        return new MetricsCollector();
    }

    private static int count(Map<VariableScope, AtomicInteger> countByScope, VariableScope scope) {
        AtomicInteger count = countByScope.get(scope);
        if (count == null) {
            return 0;
        }
        return count.get();
    }

    private static void increment(Map<VariableScope, AtomicInteger> countByScope, VariableScope scope) {
        countByScope.computeIfAbsent(scope, newScope -> new AtomicInteger(0)).incrementAndGet();
    }

    private static BigDecimal percentage(int count, int total) {
        return BigDecimal.valueOf(count)
            .divide(BigDecimal.valueOf(total), 4, RoundingMode.FLOOR)
            .multiply(HUNDRED)
            .setScale(2, RoundingMode.FLOOR);
    }

    Optional<Metrics> calculate(VariableScope scope) {
        return Metrics.calculate(count(finalModifierAddedCount, scope), count(annotatedAsMutableCount, scope));
    }

    @Override
    public void finalModifierAdded(VariableScope scope) {
        increment(finalModifierAddedCount, scope);
    }

    @Override
    public void annotatedAsMutable(VariableScope scope) {
        increment(annotatedAsMutableCount, scope);
    }

    static final class Metrics {

        final int inspectedCount;
        final BigDecimal finalModifierAddedPercentage;
        final BigDecimal annotatedAsMutablePercentage;

        private Metrics(int inspectedCount, BigDecimal finalModifierAddedPercentage, BigDecimal annotatedAsMutablePercentage) {
            this.inspectedCount = inspectedCount;
            this.finalModifierAddedPercentage = finalModifierAddedPercentage;
            this.annotatedAsMutablePercentage = annotatedAsMutablePercentage;
        }

        private static Optional<Metrics> calculate(int finalModifierAdded, int annotatedAsMutable) {
            int inspected = finalModifierAdded + annotatedAsMutable;

            if (inspected == 0) {
                return Optional.empty();
            }

            return Optional.of(new Metrics(
                inspected,
                percentage(finalModifierAdded, inspected),
                percentage(annotatedAsMutable, inspected)
            ));
        }
    }
}
