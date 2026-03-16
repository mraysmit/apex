/**
 * INTENTIONALLY throws a RuntimeException — used to demonstrate
 * that script runtime errors propagate cleanly as enrichment failures.
 */
def run(Map payload) {
    throw new RuntimeException("Intentional business error: invalid trade state")
}
