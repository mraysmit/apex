/**
 * A valid script that returns a classification — used alongside a
 * broken script to demonstrate partial failure in multi-enrichment
 * configurations.
 */
def run(Map payload) {
    def notional = payload.get('notional') ?: 0
    return notional > 500000 ? 'LARGE' : 'SMALL'
}
