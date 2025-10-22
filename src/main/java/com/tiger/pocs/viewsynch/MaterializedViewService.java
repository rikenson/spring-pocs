package com.tiger.pocs.viewsynch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterializedViewService {
    
    private final ViewConfigurationService viewConfigurationService;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    
    public void refreshView(String viewName) {
        log.info("Refreshing materialized view: {}", viewName);
        
        // Get view definition from configuration
        Optional<MaterializedView> viewOpt = viewConfigurationService.getViewDefinition(viewName);
        if (viewOpt.isEmpty()) {
            log.warn("Materialized view definition for '{}' not found", viewName);
            throw new MaterializedViewException.ViewNotFoundException(viewName);
        }
        
        MaterializedView view = viewOpt.get();
        
        try {
            // Execute aggregation and refresh the materialized view
            refreshMaterializedViewData(view);
            log.info("Successfully refreshed materialized view: {}", viewName);
            
        } catch (Exception e) {
            log.error("Failed to refresh materialized view: {}", viewName, e);
            throw new MaterializedViewException("Failed to refresh materialized view: " + viewName, e);
        }
    }
    

    public void refreshAllViews() {
        log.info("Refreshing all materialized views");
        List<MaterializedView> views = viewConfigurationService.getAllViewDefinitions();
        
        for (MaterializedView view : views) {
            try {
                refreshView(view.getViewName());
            } catch (Exception e) {
                log.error("Failed to refresh view: {}", view.getViewName(), e);
            }
        }
    }

    public List<MaterializedView> getAllViews() {
        return viewConfigurationService.getAllViewDefinitions();
    }
    
    public Optional<MaterializedView> getView(String viewName) {
        return viewConfigurationService.getViewDefinition(viewName);
    }
    
    public MaterializedView createMaterializedView(String viewName) {
        log.info("Creating materialized view: {}", viewName);
        
        // Check if target collection already exists
        if (mongoTemplate.collectionExists(viewName)) {
            log.warn("Collection '{}' already exists", viewName);
            throw new MaterializedViewException("Collection '" + viewName + "' already exists");
        }
        
        // Get view template from configuration
        Optional<MaterializedView> templateOpt = viewConfigurationService.getViewDefinition(viewName);
        if (templateOpt.isEmpty()) {
            log.error("View definition not found for: {}", viewName);
            throw new MaterializedViewException("View definition not found for: " + viewName);
        }
        
        MaterializedView template = templateOpt.get();
        
        try {
            // Create the actual MongoDB collection and populate it
            createMaterializedViewCollection(template);
            
            // Create indexes if specified
            if (template.isCreateIndexes() && template.getIndexes() != null) {
                createIndexes(template);
            }
            
            log.info("Successfully created materialized view: {}", viewName);
        } catch (Exception e) {
            log.error("Failed to create materialized view collection: {}", viewName, e);
            throw new MaterializedViewException("Failed to create materialized view collection", e);
        }
        
        return template;
    }
    
    private void createMaterializedViewCollection(MaterializedView view) {
        log.info("Creating materialized view collection: {}", view.getTargetCollection());
        
        // Drop existing collection if it exists
        if (mongoTemplate.collectionExists(view.getTargetCollection())) {
            mongoTemplate.dropCollection(view.getTargetCollection());
        }
        
        // Create new collection
        mongoTemplate.createCollection(view.getTargetCollection());
        
        // Populate with initial data
        refreshMaterializedViewData(view);
    }
    
    private void refreshMaterializedViewData(MaterializedView view) {
        try {
            // Check if the materialized view collection exists
            if (!mongoTemplate.collectionExists(view.getTargetCollection())) {
                log.warn("Synchronization could not be done because materialized view '{}' does not exist. Target collection '{}' not found.", 
                    view.getViewName(), view.getTargetCollection());
                return;
            }
            
            // Parse aggregation pipeline from JSON
            List<Map<String, Object>> pipeline = objectMapper.readValue(
                view.getAggregationPipeline(),
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // Convert to MongoDB aggregation operations
            List<AggregationOperation> operations = pipeline.stream()
                .map(stage -> (AggregationOperation) context -> new Document(stage))
                .toList();
            
            Aggregation aggregation = Aggregation.newAggregation(operations);
            
            // Clear existing data in target collection
            mongoTemplate.remove(new Query(), view.getTargetCollection());
            
            // Execute aggregation and insert results
            List<Map> results = mongoTemplate.aggregate(
                aggregation,
                view.getSourceCollection(),
                Map.class
            ).getMappedResults();
            
            if (!results.isEmpty()) {
                mongoTemplate.insert(results, view.getTargetCollection());
            }
            
            log.info("Refreshed materialized view {} with {} records",
                view.getViewName(), results.size());
            
        } catch (Exception e) {
            throw new MaterializedViewException("Failed to refresh materialized view data", e);
        }
    }
    
    private void createIndexes(MaterializedView view) {
        for (String indexField : view.getIndexes()) {
            Index index = new Index().on(indexField, org.springframework.data.domain.Sort.Direction.ASC);
            mongoTemplate.indexOps(view.getTargetCollection()).ensureIndex(index);
            log.info("Created index on field '{}' for collection '{}'", indexField, view.getTargetCollection());
        }
    }
}