package dev.mars.rulesengine.core.service.lookup;

import java.util.List;

/**
 * Generic interface for record matching.
 */
public interface RecordMatcher<T> {
    /**
     * Find records that match any of the specified validators.
     * 
     * @param sourceRecords The source records to check
     * @param validatorNames The names of the validators to check against
     * @return A list of matching records
     */
    List<T> findMatchingRecords(List<T> sourceRecords, List<String> validatorNames);
    
    /**
     * Find records that don't match any of the specified validators.
     * 
     * @param sourceRecords The source records to check
     * @param validatorNames The names of the validators to check against
     * @return A list of non-matching records
     */
    List<T> findNonMatchingRecords(List<T> sourceRecords, List<String> validatorNames);
}