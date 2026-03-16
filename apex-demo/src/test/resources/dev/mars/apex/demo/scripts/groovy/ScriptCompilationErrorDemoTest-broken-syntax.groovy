/**
 * INTENTIONALLY BROKEN Groovy script — used to demonstrate
 * compilation error handling in APEX.
 *
 * The unclosed brace below is a deliberate syntax error.
 */
def run(Map payload) {
    if (payload.get('notional') > 0) {
        return 'VALID'
    // missing closing brace — compiler should reject this
