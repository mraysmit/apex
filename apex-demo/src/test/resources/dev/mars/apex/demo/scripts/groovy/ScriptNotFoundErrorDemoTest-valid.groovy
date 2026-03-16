/**
 * A valid script that exists — used only to ensure the runtime-scripts
 * subsystem initialises correctly so that "not-found" errors are genuine
 * lookup failures rather than configuration failures.
 */
def run(Map payload) {
    return 'OK'
}
