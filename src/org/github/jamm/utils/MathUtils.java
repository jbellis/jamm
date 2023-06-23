package org.github.jamm.utils;

public final class MathUtils
{
    /**
     * Rounds x up to the next multiple of m.
     *
     * @param x the number to round
     * @param m the multiple (must be a power of 2)
     * @return the rounded value of x up to the next multiple of m.
     */
    public static long roundTo(long x, int m) {
        return (x + m - 1) & -m;
    }

    /**
     * Rounds x up to the next multiple of m.
     *
     * @param x the number to round
     * @param m the multiple (must be a power of 2)
     * @return the rounded value of x up to the next multiple of m.
     */
    public static int roundTo(int x, int m) {
        return (x + m - 1) & -m;
    }

    private MathUtils()
    {
    }
}
