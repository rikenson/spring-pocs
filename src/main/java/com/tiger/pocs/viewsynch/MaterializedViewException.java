package com.tiger.pocs.viewsynch;

public class MaterializedViewException extends RuntimeException {
    
    public MaterializedViewException(String message) {
        super(message);
    }
    
    public MaterializedViewException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class ViewNotFoundException extends MaterializedViewException {
        public ViewNotFoundException(String viewName) {
            super("Materialized view not found: " + viewName);
        }
    }
    
    public static class ConfigurationException extends MaterializedViewException {
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}