package org.beurer.bf990;

/** Seven evenly spaced ticks around the observed values. */
final class ChartRange {
    final double min, max, step;
    final int decimals;

    private ChartRange(double min, double step) {
        this.min = min;
        this.step = step;
        this.max = min + 6 * step;
        this.decimals = Math.max(0, java.math.BigDecimal.valueOf(step).stripTrailingZeros().scale());
    }

    static ChartRange forValues(double min, double max) {
        double span = max - min;
        // A single value needs a visible window even when the observed span is zero.
        double padding = span > 0 ? span * 0.2 : Math.max(Math.abs(min) * 0.02, 0.1);
        double low = min - padding, high = max + padding;
        double desired = (high - low) / 6;
        if (desired >= 1) desired = Math.ceil(desired);
        double magnitude = Math.pow(10, Math.floor(Math.log10(desired)));
        double[] factors = {1, 2, 2.5, 5, 10};
        for (;;) {
            for (double factor : factors) {
                double step = java.math.BigDecimal.valueOf(factor)
                        .multiply(java.math.BigDecimal.valueOf(magnitude))
                        .round(java.math.MathContext.DECIMAL64).doubleValue();
                if (step + 1e-10 < desired || (desired >= 1 && step != Math.rint(step))) continue;
                double base = Math.floor(low / step) * step;
                if (base + 6 * step + step * 1e-9 >= high) return new ChartRange(base, step);
            }
            magnitude *= 10;
        }
    }
}
