/**
 * INTENTIONALLY blocks beyond the configured timeout — used to
 * demonstrate timeout enforcement by ScriptExecutor.
 */
def run(Map payload) {
    Thread.sleep(30000)
    return 'SHOULD_NOT_REACH_HERE'
}
