package com.tiger.pocs.client.search;

import org.springframework.stereotype.Component;

@Component
public class FieldNameMapper {
    
    public static String toMongoField(String javaFieldName) {
        if (javaFieldName == null || javaFieldName.isEmpty()) {
            return javaFieldName;
        }
        
        // Handle special cases
        if (javaFieldName.equals("id")) {
            return "_id";
        }
        
        // Already in snake_case
        if (javaFieldName.contains("_")) {
            return javaFieldName;
        }
        
        // Convert camelCase to snake_case
        return javaFieldName
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .toLowerCase();
    }
}