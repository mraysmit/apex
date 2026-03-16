/**
 * ALWAYS throws — used to test default-value recovery
 * and partial enrichment failure.
 */
def run(Map payload) {
    throw new RuntimeException("Intentional failure for recovery demo")
}
