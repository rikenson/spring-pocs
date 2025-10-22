package com.tiger.pocs.client.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class FieldProcessor {
    
    private static final Pattern TEXT_SEARCH_PATTERN = Pattern.compile(
        ".*(name|email|title|description|service|label|comment).*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final String[] META_FIELDS = {
        "page", "size", "limit", "sortBy", "sortDirection", "offset"
    };
    
    public void processField(Field field, Object filter, Query query, Map<String, QueryBuilder.RangeValue> rangeFields) {
        field.setAccessible(true);
        try {
            Object value = field.get(filter);
            if (value == null) return;
            
            String fieldName = field.getName();
            
            // Skip meta fields
            if (isMetaField(fieldName)) return;
            
            // Handle range fields
            if (fieldName.startsWith("min") || fieldName.startsWith("max")) {
                processRangeField(fieldName, value, rangeFields);
                return;
            }
            
            // Handle "has" fields
            if (fieldName.startsWith("has") && value instanceof Boolean) {
                processHasField(fieldName, (Boolean) value, query);
                return;
            }
            
            // Handle regular fields
            processCriteriaField(fieldName, value, field.getType(), query);
            
        } catch (Exception e) {
            log.debug("Field processing error for {}: {}", field.getName(), e.getMessage());
        }
    }
    
    private void processRangeField(String fieldName, Object value, Map<String, QueryBuilder.RangeValue> ranges) {
        String prefix = fieldName.substring(0, 3);
        String actualField = FieldNameMapper.toMongoField(fieldName.substring(3));
        
        QueryBuilder.RangeValue range = ranges.computeIfAbsent(actualField, k -> new QueryBuilder.RangeValue());
        if ("min".equals(prefix)) {
            range.min = value;
        } else if ("max".equals(prefix)) {
            range.max = value;
        }
    }
    
    private void processHasField(String fieldName, Boolean value, Query query) {
        String mongoField = FieldNameMapper.toMongoField(fieldName.substring(3));
        if (Boolean.TRUE.equals(value)) {
            query.addCriteria(Criteria.where(mongoField).exists(true).ne(null).ne(""));
        } else {
            query.addCriteria(Criteria.where(mongoField).orOperator(
                Criteria.where(mongoField).exists(false),
                Criteria.where(mongoField).is(null),
                Criteria.where(mongoField).is("")
            ));
        }
    }
    
    private void processCriteriaField(String fieldName, Object value, Class<?> fieldType, Query query) {
        String mongoField = FieldNameMapper.toMongoField(fieldName);

        switch (value) {
            case String val -> processStringField(fieldName, val, mongoField, query);
            case Collection collection -> processCollectionField(collection, mongoField, query);
            case Enum ignored -> query.addCriteria(Criteria.where(mongoField).is(value.toString()));
            case null, default -> query.addCriteria(Criteria.where(mongoField).is(value));
        }
    }
    
    private void processStringField(String fieldName, String value, String mongoField, Query query) {
        if (value.isEmpty()) return;
        
        if (TEXT_SEARCH_PATTERN.matcher(fieldName).matches()) {
            query.addCriteria(Criteria.where(mongoField).regex(Pattern.quote(value), "i"));
        } else {
            query.addCriteria(Criteria.where(mongoField).is(value));
        }
    }
    
    private void processCollectionField(Collection<?> collection, String mongoField, Query query) {
        if (!collection.isEmpty()) {
            query.addCriteria(Criteria.where(mongoField).in(collection));
        }
    }
    
    private boolean isMetaField(String fieldName) {
        return Arrays.asList(META_FIELDS).contains(fieldName);
    }
}