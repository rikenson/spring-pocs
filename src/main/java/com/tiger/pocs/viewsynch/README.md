# View Synchronization Module

A MongoDB materialized view management module that provides automated view creation, refresh scheduling, and synchronization capabilities.

## Overview

The View Synchronization Module enables the creation and management of materialized views in MongoDB. It supports configuration-driven view definitions, automated refresh scheduling, and REST API management for materialized views.

### Key Features

- **Configuration-Driven Views**: Define views in YAML configuration files
- **Automated Refresh Scheduling**: Schedule periodic view refreshes using cron expressions
- **REST API Management**: Create, refresh, and manage views via HTTP endpoints
- **MongoDB Aggregation Support**: Use MongoDB aggregation pipelines for view definitions
- **Index Management**: Automatically create indexes on materialized views
- **Error Handling**: Comprehensive error handling and status tracking

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │        MaterializedViewController                   │   │
│  │  • Create/refresh views                            │   │
│  │  • Query view definitions                          │   │
│  │  • Manage schedules                               │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────┬───────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Service Layer                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         MaterializedViewService                     │   │
│  │  • View creation and refresh                       │   │
│  │  • Aggregation execution                          │   │
│  │  • Collection management                          │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │        ViewConfigurationService                     │   │
│  │  • Load view definitions from YAML                │   │
│  │  • Configuration validation                       │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │        MaterializedViewScheduler                   │   │
│  │  • Schedule periodic refreshes                     │   │
│  │  • Manage refresh tasks                           │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────┬───────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Data Layer                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │             MongoDB                                 │   │
│  │  • Source collections                              │   │
│  │  • Materialized view collections                   │   │
│  │  • Aggregation pipeline execution                  │   │
│  │  • Index management                               │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Components

### 1. REST API Layer

#### `MaterializedViewController`
REST controller providing HTTP endpoints for view management.

```java
@RestController
@RequestMapping("/materialized-views")
@RequiredArgsConstructor
public class MaterializedViewController {

    private final MaterializedViewService materializedViewService;
    private final ViewConfigurationService viewConfigurationService;
    private final MaterializedViewScheduler materializedViewScheduler;

    @PostMapping("/{viewName}")
    public ResponseEntity<MaterializedView> createMaterializedView(@PathVariable String viewName) {
        MaterializedView createdView = materializedViewService.createMaterializedView(viewName);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdView);
    }

    @PostMapping("/{viewName}/refresh")
    public ResponseEntity<Void> refreshMaterializedView(@PathVariable String viewName) {
        materializedViewService.refreshView(viewName);
        return ResponseEntity.ok().build();
    }
    
    // Additional endpoints for view management...
}
```

**Available Endpoints:**
- `POST /materialized-views/{viewName}` - Create a materialized view
- `GET /materialized-views` - Get all materialized views
- `GET /materialized-views/{viewName}` - Get specific view
- `POST /materialized-views/{viewName}/refresh` - Refresh a view
- `GET /materialized-views/definitions` - Get view definitions
- `POST /materialized-views/schedule/reinitialize` - Reinitialize schedules

### 2. Service Layer

#### `MaterializedViewService`
Core service handling view creation, refresh, and management.

```java
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
    
    public MaterializedView createMaterializedView(String viewName) {
        // Create MongoDB collection and populate with aggregated data
        // Create indexes if specified in configuration
    }
}
```

**Key Features:**
- View creation with collection management
- Aggregation pipeline execution using MongoDB aggregation framework
- Index creation and management
- Data refresh and synchronization
- Error handling and status tracking

#### `ViewConfigurationService`
Loads and manages view definitions from YAML configuration.

```java
@Slf4j
@Service
public class ViewConfigurationService {

    private static final TypeReference<List<MaterializedView>> VIEW_LIST_TYPE = new TypeReference<>() {};
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private List<MaterializedView> viewDefinitions = List.of();

    @PostConstruct
    void loadViewConfigurations() {
        ClassPathResource resource = new ClassPathResource("views.yml");
        if (!resource.exists()) {
            log.warn("views.yml not found in classpath");
            return;
        }

        try {
            this.viewDefinitions = YAML_MAPPER.readValue(resource.getInputStream(), VIEW_LIST_TYPE);
            log.info("Loaded {} view definitions from views.yml", viewDefinitions.size());
        } catch (Exception e) {
            log.error("Failed to load view configurations from views.yml", e);
            throw new MaterializedViewException.ConfigurationException("Failed to load view configurations from views.yml", e);
        }
    }
}
```

**Key Features:**
- YAML configuration loading from classpath
- View definition validation
- Configuration caching and management

#### `MaterializedViewScheduler`
Manages scheduled refresh tasks for materialized views.

```java
@Component
public class MaterializedViewScheduler {
    // Schedules periodic view refreshes based on cron expressions
    // Manages task lifecycle and cancellation
    // Handles scheduling errors and retries
}
```

### 3. Data Models

#### `MaterializedView`
Main model representing a materialized view configuration and state.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterializedView {
    
    private String id;
    private String viewName;
    private String sourceCollection;
    private String targetCollection;
    private String aggregationPipeline;
    private String refreshSchedule;
    private boolean createIndexes;
    private List<String> indexes;
    private LocalDateTime lastRefreshTime;
    private LocalDateTime nextScheduledRefresh;
    private RefreshStatus status;
    private Long recordCount;
    private String errorMessage;
    private Map<String, Object> metadata;
    
    public enum RefreshStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, SCHEDULED
    }
}
```

## Configuration

### View Definitions

Views are defined in a `views.yml` file in the classpath:

```yaml
- viewName: "client_summary"
  sourceCollection: "clients"
  targetCollection: "client_summary_view"
  refreshSchedule: "0 0 2 * * ?" # Daily at 2 AM
  createIndexes: true
  indexes:
    - "clientName"
    - "lastUpdateDate"
  aggregationPipeline: |
    [
      {
        "$match": {
          "deleted": false
        }
      },
      {
        "$group": {
          "_id": "$clientType",
          "totalClients": {"$sum": 1},
          "lastUpdateDate": {"$max": "$lastUpdateDate"}
        }
      },
      {
        "$project": {
          "clientType": "$_id",
          "totalClients": 1,
          "lastUpdateDate": 1,
          "_id": 0
        }
      }
    ]

- viewName: "monthly_metrics"
  sourceCollection: "events"
  targetCollection: "monthly_metrics_view"
  refreshSchedule: "0 0 1 1 * ?" # Monthly on 1st day at midnight
  createIndexes: true
  indexes:
    - "year"
    - "month"
  aggregationPipeline: |
    [
      {
        "$match": {
          "eventDate": {
            "$gte": {"$dateFromString": {"dateString": "2024-01-01"}}
          }
        }
      },
      {
        "$group": {
          "_id": {
            "year": {"$year": "$eventDate"},
            "month": {"$month": "$eventDate"}
          },
          "eventCount": {"$sum": 1},
          "uniqueUsers": {"$addToSet": "$userId"}
        }
      },
      {
        "$project": {
          "year": "$_id.year",
          "month": "$_id.month",
          "eventCount": 1,
          "uniqueUserCount": {"$size": "$uniqueUsers"},
          "_id": 0
        }
      }
    ]
```

### Required Dependencies

The module requires:
- Spring Boot Web
- Spring Data MongoDB
- Jackson YAML
- Spring Scheduling

### MongoDB Configuration

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/reactive-pocs
      database: reactive-pocs
      
  task:
    scheduling:
      enabled: true
```

## Usage Examples

### Creating a Materialized View

```bash
# Create a materialized view from configuration
curl -X POST http://localhost:8080/materialized-views/client_summary
```

### Refreshing a View

```bash
# Manually refresh a materialized view
curl -X POST http://localhost:8080/materialized-views/client_summary/refresh
```

### Querying Views

```bash
# Get all available views
curl -X GET http://localhost:8080/materialized-views

# Get specific view information
curl -X GET http://localhost:8080/materialized-views/client_summary

# Get view definitions
curl -X GET http://localhost:8080/materialized-views/definitions
```

### Programmatic Usage

```java
@Service
public class ReportingService {
    
    private final MaterializedViewService viewService;
    private final MongoTemplate mongoTemplate;
    
    public void generateReport() {
        // Ensure view is up to date
        viewService.refreshView("client_summary");
        
        // Query the materialized view
        List<Map> summaryData = mongoTemplate.findAll(Map.class, "client_summary_view");
        
        // Generate report from materialized data
        processReportData(summaryData);
    }
}
```

## Error Handling

### Exception Types

- **`MaterializedViewException`**: Base exception for view-related errors
- **`MaterializedViewException.ViewNotFoundException`**: View definition not found
- **`MaterializedViewException.ConfigurationException`**: Configuration loading errors

### Error Responses

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "View Not Found",
  "message": "Materialized view definition for 'unknown_view' not found",
  "path": "/materialized-views/unknown_view"
}
```

## Monitoring and Management

### View Status Tracking

Each view maintains status information:
- Last refresh time
- Next scheduled refresh
- Current status (PENDING, IN_PROGRESS, COMPLETED, FAILED)
- Record count
- Error messages

### Schedule Management

```bash
# Reinitialize all schedules
curl -X POST http://localhost:8080/materialized-views/schedule/reinitialize

# Cancel specific view schedule
curl -X POST http://localhost:8080/materialized-views/client_summary/schedule/cancel
```

## Best Practices

1. **Aggregation Design**: Design efficient aggregation pipelines that minimize data movement
2. **Index Strategy**: Create appropriate indexes on frequently queried fields
3. **Refresh Frequency**: Balance data freshness needs with system performance
4. **Error Handling**: Monitor view refresh failures and implement retry logic
5. **Resource Management**: Consider resource usage during view refresh operations

## Performance Considerations

### Aggregation Optimization

- Use `$match` early in the pipeline to reduce data volume
- Add appropriate indexes on source collection fields used in `$match` and `$sort`
- Consider using `$project` to reduce field selection
- Use `$limit` when appropriate to control result size

### Refresh Scheduling

- Schedule heavy view refreshes during low-traffic periods
- Stagger refresh times for multiple views to avoid resource contention
- Monitor refresh duration and adjust schedules accordingly

## Troubleshooting

### Common Issues

1. **View creation fails**: Check MongoDB connection and permissions
2. **Aggregation errors**: Validate aggregation pipeline syntax in MongoDB shell
3. **Scheduling issues**: Verify cron expression syntax and Spring scheduling configuration
4. **Configuration loading errors**: Check `views.yml` file location and YAML syntax

### Debugging

Enable debug logging for the view synchronization module:

```yaml
logging:
  level:
    com.tiger.pocs.viewsynch: DEBUG
```

This will provide detailed logs for view operations, aggregation execution, and scheduling activities.