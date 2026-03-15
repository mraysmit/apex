class RiskScore {
    /**
     * Compute risk level based on trade characteristics.
     *
     * @param payload Map with keys: notional (Number), counterpartyRating (String)
     * @return String risk classification: HIGH, MEDIUM, or LOW
     */
    def run(Map payload) {
        def notional = payload.get('notional') ?: 0
        def rating = payload.get('counterpartyRating') ?: 'UNKNOWN'

        // High risk: large notional OR poor counterparty rating
        if (notional > 1000000 || rating == 'CCC' || rating == 'D') {
            return 'HIGH'
        }

        // Medium risk: moderate notional OR below-investment-grade
        if (notional > 100000 || rating == 'BB' || rating == 'B') {
            return 'MEDIUM'
        }

        return 'LOW'
    }

    /**
     * Calculate risk-adjusted notional.
     *
     * @param notional The trade notional
     * @param riskLevel The computed risk level
     * @return BigDecimal risk-adjusted value
     */
    def riskAdjustedNotional(BigDecimal notional, String riskLevel) {
        def multiplier = switch (riskLevel) {
            case 'HIGH' -> 1.5
            case 'MEDIUM' -> 1.2
            default -> 1.0
        }
        return notional * multiplier
    }
}
