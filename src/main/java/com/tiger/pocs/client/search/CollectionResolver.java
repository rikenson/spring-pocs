package com.tiger.pocs.client.search;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CollectionResolver {
    
    private static final Map<String, String> COLLECTION_MAPPINGS = new HashMap<>();
    
    static {
        // Predefined collection mappings
        COLLECTION_MAPPINGS.put("ClientSummaryDto", "client_summary");
        COLLECTION_MAPPINGS.put("LogSummaryDto", "log_summary");
        COLLECTION_MAPPINGS.put("NotificationSummaryDto", "notification_summary");
        COLLECTION_MAPPINGS.put("UserSummaryDto", "user_summary");
    }
    
    public String resolveCollection(Class<?> dtoClass) {
        String className = dtoClass.getSimpleName();
        
        // Check predefined mappings first
        if (COLLECTION_MAPPINGS.containsKey(className)) {
            return COLLECTION_MAPPINGS.get(className);
        }
        
        // Infer from class name
        return inferCollectionName(className);
    }
    
    private String inferCollectionName(String className) {
        // Remove common suffixes
        String baseName = className
            .replace("Dto", "")
            .replace("Entity", "")
            .replace("Model", "");
        
        // Convert to snake_case
        String snakeCase = baseName
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .toLowerCase();
        
        // Add summary suffix if not already present
        if (!snakeCase.endsWith("_summary") && !snakeCase.endsWith("_detail")) {
            snakeCase += "_summary";
        }
        
        return snakeCase;
    }
    
    public void addMapping(String className, String collectionName) {
        COLLECTION_MAPPINGS.put(className, collectionName);
    }
}