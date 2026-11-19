package org.beurer.bf990;

/** Least-squares straight line through chart points; x is the day number, y the value. */
final class Trend {
    final double slope, intercept;
    private Trend(double slope, double intercept) { this.slope = slope; this.intercept = intercept; }

    double at(double x) { return intercept + slope * x; }

    /** Returns null when fewer than two distinct days are present. */
    static Trend fit(long[] days, double[] values) {
        int n = days.length;
        if (n < 2) return null;
        double mx = 0, my = 0;
        for (int i = 0; i < n; i++) { mx += days[i]; my += values[i]; }
        mx /= n; my /= n;
        double sxx = 0, sxy = 0;
        for (int i = 0; i < n; i++) { double dx = days[i] - mx; sxx += dx * dx; sxy += dx * (values[i] - my); }
        if (sxx == 0) return null;
        double slope = sxy / sxx;
        return new Trend(slope, my - slope * mx);
    }
}
