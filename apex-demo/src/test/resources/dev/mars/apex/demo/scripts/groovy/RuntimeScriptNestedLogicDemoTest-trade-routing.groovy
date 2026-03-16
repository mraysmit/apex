/**
 * Return routing decision from nested business conditions.
 *
 * Uses nested if/else logic intentionally (no switch/case).
 */
def run(Map payload) {
    def productType = (payload.get('productType') ?: 'UNKNOWN').toString()

    if (productType == 'DERIVATIVE') {
        def notional = payload.get('notional') ?: 0
        def tier = (payload.get('counterpartyTier') ?: 'TIER2').toString()
        def region = (payload.get('region') ?: 'UNKNOWN').toString()

        if (notional > 2000000) {
            if (tier == 'TIER3' || tier == 'TIER4') {
                return 'MANUAL_REVIEW'
            }
            if (region == 'SANCTIONED') {
                return 'MANUAL_REVIEW'
            }
            return 'OTC_DESK'
        } else {
            def currency = (payload.get('currency') ?: 'USD').toString()
            def marginPosted = payload.get('marginPosted') == true

            if (marginPosted) {
                if (currency == 'USD' || currency == 'EUR') {
                    return 'STP_DERIV'
                }
            }
            return 'ANALYST_REVIEW'
        }
    } else {
        def method = (payload.get('paymentMethod') ?: 'LOCAL').toString()
        def amount = payload.get('amount') ?: 0
        def priorityClient = payload.get('isPriorityClient') == true

        if (method == 'SWIFT') {
            if (amount > 250000) {
                if (!priorityClient) {
                    return 'COMPLIANCE_REVIEW'
                }
                return 'STP_PAYMENTS'
            }
            return 'STP_PAYMENTS'
        }
        return 'MANUAL_REVIEW'
    }
}
