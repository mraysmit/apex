/**
 * Script used to demonstrate method dispatch errors.
 *
 * Has a valid run(Map) and a two-arg helper — callers that invoke
 * a wrong function name or wrong arity will get explicit errors.
 */
def run(Map payload) {
    return 'OK'
}

def twoArgHelper(String a, String b) {
    return a + '-' + b
}
