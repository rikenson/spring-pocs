package com.tiger.pocs.client.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class QueryBuilder {
    
    private final FieldProcessor fieldProcessor;
    
    public QueryBuilder(FieldProcessor fieldProcessor) {
        this.fieldProcessor = fieldProcessor;
    }
    
    public Query buildQuery(Object filter) {
        if (filter == null) return new Query();
        
        Query query = new Query();
        Map<String, RangeValue> rangeFields = new HashMap<>();
        
        // Process all fields
        for (Field field : filter.getClass().getDeclaredFields()) {
            fieldProcessor.processField(field, filter, query, rangeFields);
        }
        
        // Apply ranges
        applyRanges(rangeFields, query);
        
        // Apply sorting
        applySorting(filter, query);
        
        // Apply limit
        Integer limit = extractFieldValue(filter, "limit", Integer.class);
        if (limit != null && limit > 0) {
            query.limit(limit);
        }
        
        return query;
    }
    
    private void applyRanges(Map<String, RangeValue> ranges, Query query) {
        ranges.forEach((field, range) -> {
            Criteria criteria = Criteria.where(field);
            if (range.min != null && range.max != null) {
                criteria.gte(range.min).lte(range.max);
            } else if (range.min != null) {
                criteria.gte(range.min);
            } else if (range.max != null) {
                criteria.lte(range.max);
            }
            query.addCriteria(criteria);
        });
    }
    
    private void applySorting(Object filter, Query query) {
        String sortBy = extractFieldValue(filter, "sortBy", String.class);
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }
        
        String sortDirection = extractFieldValue(filter, "sortDirection", String.class);
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
            
        query.with(Sort.by(direction, FieldNameMapper.toMongoField(sortBy)));
    }
    
    private <T> T extractFieldValue(Object obj, String fieldName, Class<T> type) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    static class RangeValue {
        Object min;
        Object max;
    }
}