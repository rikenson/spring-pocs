package com.tiger.pocs.client.search;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class PaginationExtractor {
    
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    
    public PageRequest extractPageRequest(Object filter) {
        if (filter == null) {
            return PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
        }
        
        Integer page = extractFieldValue(filter, "page", Integer.class);
        Integer size = extractFieldValue(filter, "size", Integer.class);
        Integer limit = extractFieldValue(filter, "limit", Integer.class);
        
        int pageNumber = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int pageSize = determinePageSize(size, limit);
        
        return PageRequest.of(pageNumber, pageSize);
    }
    
    private int determinePageSize(Integer size, Integer limit) {
        if (size != null && size > 0) {
            return size;
        }
        if (limit != null && limit > 0) {
            return limit;
        }
        return DEFAULT_SIZE;
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
}