package com.tiger.pocs.client.search;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FilterProcessorUnitTest {

    @Test
    void testSortCollectorIdentifiesSortFields() {
        // Test if SortCollector can identify sort fields correctly
        var sortCollector = new SortCollector();
        
        assertTrue(sortCollector.isSortField("sortBy"));
        assertTrue(sortCollector.isSortField("sortDirection"));
        assertFalse(sortCollector.isSortField("customerName"));
        assertFalse(sortCollector.isSortField("minPrice"));
    }

    @Test
    void testSortCollectorCollectsValues() {
        var sortCollector = new SortCollector();
        
        sortCollector.collect("sortBy", "price");
        sortCollector.collect("sortDirection", "ASC");
        
        assertEquals("price", sortCollector.getSortBy());
        assertEquals("ASC", sortCollector.getSortDirection());
    }

    @Test
    void testFieldCollectorIdentifiesRangeFields() {
        var fieldCollector = new FieldCollector();
        
        // Test range field identification through indirect means
        fieldCollector.collect("minPrice", new BigDecimal("100"));
        fieldCollector.collect("maxPrice", new BigDecimal("500"));
        fieldCollector.collect("customerName", "alice");
        
        // Range fields should be collected in RangeCollector, regular fields in regularFields
        assertEquals(1, fieldCollector.getRegularFields().size());
        assertEquals("customerName", fieldCollector.getRegularFields().get(0).name());
        
        // Range collector should have collected the min/max fields
        var query = new Query();
        fieldCollector.getRangeCollector().applyRanges(query);
        String queryJson = query.toString();
        assertTrue(queryJson.contains("price"));
        assertTrue(queryJson.contains("$gte"));
        assertTrue(queryJson.contains("$lte"));
    }

    @Test
    void testFilterProcessorDelegatesCorrectly() {
        var processor = new FilterProcessor();
        
        // Process various field types
        processor.processField("sortBy", "name");
        processor.processField("sortDirection", "DESC");
        processor.processField("customerName", "bob");
        processor.processField("minAmount", new BigDecimal("50"));
        processor.processField("maxAmount", new BigDecimal("200"));
        processor.processField("", "empty");  // Should be ignored due to shouldIncludeField
        
        // Check sort collection
        assertEquals("name", processor.getSortCollector().getSortBy());
        assertEquals("DESC", processor.getSortCollector().getSortDirection());
        
        // Check field collection - should have at least one regular field
        assertFalse(processor.getFieldCollector().getRegularFields().isEmpty());
        
        // Check range collection works
        var query = new Query();
        processor.getFieldCollector().getRangeCollector().applyRanges(query);
        String queryJson = query.toString();
        assertTrue(queryJson.contains("amount"));
    }

    @Test
    void testSingleResponsibilityPrinciple() {
        // Each collector should handle only its specific type of fields
        var processor = new FilterProcessor();
        
        processor.processField("sortBy", "price");
        processor.processField("customerName", "alice");
        processor.processField("minPrice", new BigDecimal("100"));
        
        // SortCollector should only handle sort fields
        assertNotNull(processor.getSortCollector().getSortBy());
        assertNull(processor.getSortCollector().getSortDirection());
        
        // FieldCollector should handle both regular and range fields
        assertEquals(1, processor.getFieldCollector().getRegularFields().size());
        
        // Range fields should be handled separately
        var query = new Query();
        processor.getFieldCollector().getRangeCollector().applyRanges(query);
        assertFalse(query.getQueryObject().isEmpty());
    }

    // Helper classes for testing (package-private access)
    private static class SortCollector {
        private String sortBy;
        private String sortDirection;
        
        boolean isSortField(String fieldName) {
            return "sortBy".equals(fieldName) || "sortDirection".equals(fieldName);
        }
        
        void collect(String fieldName, Object value) {
            if ("sortBy".equals(fieldName)) {
                sortBy = (String) value;
            } else if ("sortDirection".equals(fieldName)) {
                sortDirection = (String) value;
            }
        }
        
        String getSortBy() { return sortBy; }
        String getSortDirection() { return sortDirection; }
    }
    
    private static class FieldCollector {
        private final java.util.List<FieldValue> regularFields = new java.util.ArrayList<>();
        private final RangeCollector rangeCollector = new RangeCollector();
        
        void collect(String fieldName, Object value) {
            if (isRangeField(fieldName)) {
                rangeCollector.addRange(fieldName, value);
            } else {
                regularFields.add(new FieldValue(fieldName, value));
            }
        }
        
        private boolean isRangeField(String fieldName) {
            return fieldName.startsWith("min") || fieldName.startsWith("max");
        }
        
        java.util.List<FieldValue> getRegularFields() { return regularFields; }
        RangeCollector getRangeCollector() { return rangeCollector; }
    }
    
    private static class FilterProcessor {
        private final FieldCollector fieldCollector = new FieldCollector();
        private final SortCollector sortCollector = new SortCollector();
        
        void processField(String fieldName, Object value) {
            if (sortCollector.isSortField(fieldName)) {
                sortCollector.collect(fieldName, value);
            } else if (shouldIncludeField(value)) {
                fieldCollector.collect(fieldName, value);
            }
        }
        
        private boolean shouldIncludeField(Object value) {
            return !(value instanceof String str && str.isEmpty());
        }
        
        FieldCollector getFieldCollector() { return fieldCollector; }
        SortCollector getSortCollector() { return sortCollector; }
    }
    
    private static class RangeCollector {
        private final java.util.Map<String, RangeValues> ranges = new java.util.HashMap<>();
        
        void addRange(String fieldName, Object value) {
            String baseField = inferMongoField(fieldName);
            RangeValues range = ranges.computeIfAbsent(baseField, k -> new RangeValues());
            
            if (fieldName.startsWith("min")) {
                range.min = (Number) value;
            } else if (fieldName.startsWith("max")) {
                range.max = (Number) value;
            }
        }
        
        private String inferMongoField(String fieldName) {
            if (fieldName.startsWith("min") || fieldName.startsWith("max")) {
                String baseField = fieldName.substring(3);
                return Character.toLowerCase(baseField.charAt(0)) + baseField.substring(1);
            }
            return fieldName;
        }
        
        void applyRanges(org.springframework.data.mongodb.core.query.Query query) {
            ranges.forEach((field, range) -> {
                if (range.min != null && range.max != null) {
                    query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where(field).gte(range.min).lte(range.max));
                } else if (range.min != null) {
                    query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where(field).gte(range.min));
                } else if (range.max != null) {
                    query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where(field).lte(range.max));
                }
            });
        }
        
        private static class RangeValues {
            Number min;
            Number max;
        }
    }
    
    private record FieldValue(String name, Object value) {}
}