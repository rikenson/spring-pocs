# Client Management Module

A comprehensive, reactive client management module providing REST APIs, advanced search capabilities, and MongoDB integration for managing client information in the reactive POCs application.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Components](#components)
- [API Endpoints](#api-endpoints)
- [Search Capabilities](#search-capabilities)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)
- [Error Handling](#error-handling)
- [Performance](#performance)
- [Testing](#testing)
- [Best Practices](#best-practices)

## Overview

The Client Management Module provides a complete solution for managing client information with the following key features:

- **Reactive REST APIs**: Non-blocking endpoints built with Spring WebFlux
- **Advanced Search**: Flexible filtering, pagination, and sorting capabilities
- **MongoDB Integration**: Reactive MongoDB operations with custom repositories
- **Type Safety**: Strong typing with generated OpenAPI models
- **Error Handling**: Comprehensive error handling and validation
- **Performance Optimization**: Efficient queries and materialized views

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client Layer                          │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐      │
│  │   Web UI      │  │  Mobile App   │  │  API Clients  │      │
│  └───────────────┘  └───────────────┘  └───────────────┘      │
└─────────────────┬───────────────┬───────────────┬─────────────┘
                 │               │               │
                 ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller Layer                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │               ClientSummaryController                   │   │
│  │  • POST /clients/search                                 │   │
│  │  • POST /clients/search-one                             │   │
│  │  • GET /clients/{id}                                    │   │
│  │  • PUT /clients/{id}                                    │   │
│  │  • DELETE /clients/{id}                                 │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service Layer                            │
│  ┌─────────────────────┬─────────────────────┬───────────────┐ │
│  │ GenericSearchService │ FilterProcessor     │ ClientUtils   │ │
│  │                     │                     │               │ │
│  │ • Search operations │ • Filter processing │ • Utilities   │ │
│  │ • Pagination        │ • Query building    │ • Validation  │ │
│  │ • Sorting           │ • Type conversion   │ • Mapping     │ │
│  └─────────────────────┴─────────────────────┴───────────────┘ │
└─────────────────────────────┬───────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Repository Layer                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  GenericRepository                     │   │
│  │  • ReactiveMongoTemplate                               │   │
│  │  • Dynamic query building                              │   │
│  │  • Aggregation pipeline support                       │   │
│  │  • Index optimization                                 │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                     MongoDB                             │   │
│  │  • clients collection                                  │   │
│  │  • Indexed queries                                     │   │
│  │  • Reactive operations                                 │   │
│  │  • Change streams                                      │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Features

### Core Features
- ✅ **Reactive CRUD Operations**: Full Create, Read, Update, Delete with reactive streams
- ✅ **Advanced Search**: Multi-field filtering with pagination and sorting
- ✅ **Type Safety**: OpenAPI-generated models with validation
- ✅ **Performance Optimization**: Efficient MongoDB queries and indexing
- ✅ **Error Handling**: Comprehensive error responses with proper HTTP status codes

### Search Features
- ✅ **Multi-field Filtering**: Filter by multiple client attributes simultaneously
- ✅ **Pagination**: Configurable page size with efficient pagination
- ✅ **Sorting**: Multi-field sorting with ascending/descending options
- ✅ **Partial Matching**: Text search with pattern matching
- ✅ **Range Queries**: Numeric and date range filtering
- ✅ **External Reference Search**: Search by external system references

### Integration Features
- ✅ **OpenAPI Integration**: Auto-generated client models and API interfaces
- ✅ **MongoDB Reactive**: Non-blocking database operations
- ✅ **WebFlux Configuration**: Optimized reactive web configuration
- ✅ **Exception Handling**: Standardized error responses

## Components

### 1. Controller Layer

#### ClientSummaryController
The main REST controller providing client management endpoints.

```java
@RestController
@RequestMapping(\"/api/v1/clients\")
@RequiredArgsConstructor
@Slf4j
public class ClientSummaryController implements ClientsApi {
    
    private final GenericSearchService searchService;
    
    @Override
    public Mono<ResponseEntity<ClientPage>> searchClientsPaginated(
            Mono<ClientSummaryFilter> clientSummaryFilter, 
            ServerHttpRequest request) {
        
        return clientSummaryFilter
            .flatMap(filter -> searchService.searchPaginated(
                filter, 
                ClientEntity.class, 
                ClientSummaryDto.class
            ))
            .map(page -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(page))
            .doOnNext(response -> log.info(\"🟢 CLIENT SEARCH: Completed paginated search\"))
            .onErrorMap(this::handleSearchError);
    }
}
```

**Key Features:**
- Implements OpenAPI-generated `ClientsApi` interface
- Reactive endpoint handlers using `Mono` and `Flux`
- Comprehensive logging and error handling
- Content type and response header management

### 2. Service Layer

#### GenericSearchService
Core service providing flexible search capabilities across different entity types.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GenericSearchService {
    
    private final GenericRepository repository;
    private final FilterProcessor filterProcessor;
    
    public <F, E, D> Mono<ClientPage> searchPaginated(
            F filter, 
            Class<E> entityClass, 
            Class<D> dtoClass) {
        
        return Mono.fromCallable(() -> filterProcessor.processFilter(filter))
            .flatMap(processedFilter -> {
                Query query = buildQuery(processedFilter);
                return executePagedQuery(query, entityClass, dtoClass, processedFilter);
            })
            .doOnSubscribe(subscription -> 
                log.info(\"🟡 SEARCH: Starting paginated search for {}\", entityClass.getSimpleName()))
            .doOnNext(page -> 
                log.info(\"🟢 SEARCH: Completed search - {} results\", page.getTotalElements()));
    }
    
    private <E, D> Mono<ClientPage> executePagedQuery(
            Query query, 
            Class<E> entityClass, 
            Class<D> dtoClass, 
            ProcessedFilter processedFilter) {
        
        return Mono.zip(
            repository.findWithQuery(query, entityClass)
                .cast(ClientEntity.class)
                .map(this::mapToDto)
                .collectList(),
            repository.countWithQuery(query, entityClass)
        ).map(tuple -> buildPageResponse(tuple.getT1(), tuple.getT2(), processedFilter));
    }
}
```

**Key Features:**
- Generic design supporting multiple entity types
- Reactive query execution with parallel count queries
- Filter processing and query building
- DTO mapping and pagination response construction

#### FilterProcessor
Processes and validates filter objects, converting them to MongoDB queries.

```java
@Component
@Slf4j
public class FilterProcessor {
    
    public ProcessedFilter processFilter(Object filter) {
        if (filter instanceof ClientSummaryFilter clientFilter) {
            return processClientFilter(clientFilter);
        }
        throw new UnsupportedOperationException(\"Filter type not supported: \" + filter.getClass());
    }
    
    private ProcessedFilter processClientFilter(ClientSummaryFilter filter) {
        return ProcessedFilter.builder()
            .criteria(buildCriteria(filter))
            .pageable(buildPageable(filter))
            .entityType(\"ClientEntity\")
            .filterType(\"ClientSummaryFilter\")
            .build();
    }
    
    private Criteria buildCriteria(ClientSummaryFilter filter) {
        Criteria criteria = new Criteria();
        
        // Text filtering
        if (StringUtils.hasText(filter.getClientName())) {
            criteria = criteria.and(\"clientName\")
                .regex(filter.getClientName(), \"i\"); // Case-insensitive
        }
        
        // Exact matching
        if (filter.getClientNumber() != null) {
            criteria = criteria.and(\"clientNumber\").is(filter.getClientNumber());
        }
        
        // Boolean filtering
        if (filter.getDeleted() != null) {
            criteria = criteria.and(\"deleted\").is(filter.getDeleted());
        }
        
        // Range filtering
        if (filter.getMinVersion() != null || filter.getMaxVersion() != null) {
            Criteria versionCriteria = criteria.and(\"version\");
            if (filter.getMinVersion() != null) {
                versionCriteria = versionCriteria.gte(filter.getMinVersion());
            }
            if (filter.getMaxVersion() != null) {
                versionCriteria = versionCriteria.lte(filter.getMaxVersion());
            }
        }
        
        // External reference filtering
        if (StringUtils.hasText(filter.getExternalReferenceType()) || 
            StringUtils.hasText(filter.getExternalReferenceValue())) {
            criteria = criteria.and(\"externalReferences\")
                .elemMatch(buildExternalRefCriteria(filter));
        }
        
        return criteria;
    }
}
```

**Key Features:**
- Type-safe filter processing
- Multiple filter types support (text, exact, boolean, range)
- MongoDB Criteria building
- External reference nested queries

### 3. Repository Layer

#### GenericRepository
Reactive repository providing flexible database operations.

```java
@Repository
@RequiredArgsConstructor
@Slf4j
public class GenericRepository {
    
    private final ReactiveMongoTemplate mongoTemplate;
    
    public <T> Flux<T> findWithQuery(Query query, Class<T> entityClass) {
        return mongoTemplate.find(query, entityClass)
            .doOnSubscribe(subscription -> 
                log.debug(\"🔍 REPO: Executing query for {}: {}\", 
                    entityClass.getSimpleName(), query.toString()))
            .doOnComplete(() -> 
                log.debug(\"✅ REPO: Query completed for {}\", entityClass.getSimpleName()));
    }
    
    public <T> Mono<Long> countWithQuery(Query query, Class<T> entityClass) {
        return mongoTemplate.count(query, entityClass)
            .doOnNext(count -> 
                log.debug(\"📊 REPO: Count result for {}: {}\", 
                    entityClass.getSimpleName(), count));
    }
    
    public <T> Mono<T> findById(Object id, Class<T> entityClass) {
        return mongoTemplate.findById(id, entityClass)
            .doOnNext(entity -> 
                log.debug(\"🎯 REPO: Found entity by ID: {}\", id))
            .switchIfEmpty(Mono.error(
                new EntityNotFoundException(\"Entity not found with ID: \" + id)));
    }
    
    public <T> Mono<T> save(T entity) {
        return mongoTemplate.save(entity)
            .doOnNext(saved -> 
                log.debug(\"💾 REPO: Saved entity: {}\", 
                    saved.getClass().getSimpleName()));
    }
    
    public <T> Mono<Void> deleteById(Object id, Class<T> entityClass) {
        return mongoTemplate.findAndRemove(
                Query.query(Criteria.where(\"id\").is(id)), 
                entityClass)
            .doOnNext(deleted -> 
                log.debug(\"🗑️ REPO: Deleted entity by ID: {}\", id))
            .then();
    }
}
```

**Key Features:**
- Reactive MongoDB operations
- Generic type support
- Comprehensive logging
- Error handling for not found cases

### 4. Configuration

#### ClientWebFluxConfig
WebFlux configuration optimized for client management operations.

```java
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ClientWebFluxConfig implements WebFluxConfigurer {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(
            new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
        configurer.defaultCodecs().jackson2JsonDecoder(
            new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
        
        // Configure limits
        configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
        
        log.info(\"🔧 CLIENT CONFIG: Configured WebFlux message codecs\");
    }
    
    @Bean
    public WebFilter loggingWebFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            log.info(\"🌐 CLIENT REQUEST: {} {}\", 
                request.getMethod(), request.getPath());
            
            return chain.filter(exchange)
                .doOnTerminate(() -> 
                    log.info(\"✅ CLIENT RESPONSE: {} completed\", request.getPath()));
        };
    }
}
```

## API Endpoints

### Client Search Endpoints

#### POST /api/v1/clients/search
Search clients with pagination and filtering.

**Request Body:**
```json
{
  \"clientName\": \"Acme\",
  \"deleted\": false,
  \"page\": 0,
  \"size\": 20,
  \"sortBy\": \"clientName\",
  \"sortDirection\": \"ASC\"
}
```

**Response:**
```json
{
  \"content\": [
    {
      \"id\": 1,
      \"version\": 1,
      \"clientName\": \"Acme Corporation\",
      \"clientNumber\": \"CLI-001\",
      \"lastUpdateUserId\": 123,
      \"deleted\": false,
      \"externalReferences\": [
        {
          \"externalReferenceType\": \"CRM_ID\",
          \"externalReferenceValue\": \"CRM-12345\",
          \"lastUpdateUserId\": 123,
          \"deleted\": false
        }
      ]
    }
  ],
  \"totalPages\": 5,
  \"totalElements\": 95,
  \"size\": 20,
  \"number\": 0,
  \"first\": true,
  \"last\": false,
  \"empty\": false
}
```

#### POST /api/v1/clients/search-one
Find a single client matching the filter criteria.

**Request Body:**
```json
{
  \"clientNumber\": \"CLI-001\"
}
```

**Response:**
```json
{
  \"id\": 1,
  \"version\": 1,
  \"clientName\": \"Acme Corporation\",
  \"clientNumber\": \"CLI-001\",
  \"lastUpdateUserId\": 123,
  \"deleted\": false,
  \"externalReferences\": []
}
```

### Client CRUD Endpoints

#### GET /api/v1/clients/{id}
Retrieve a specific client by ID.

**Response:**
```json
{
  \"id\": 1,
  \"version\": 1,
  \"clientName\": \"Acme Corporation\",
  \"clientNumber\": \"CLI-001\",
  \"lastUpdateUserId\": 123,
  \"deleted\": false
}
```

#### PUT /api/v1/clients/{id}
Update an existing client.

**Request Body:**
```json
{
  \"version\": 2,
  \"clientName\": \"Acme Corporation Ltd\",
  \"clientNumber\": \"CLI-001\",
  \"lastUpdateUserId\": 456,
  \"deleted\": false
}
```

#### DELETE /api/v1/clients/{id}
Delete a client (soft delete).

**Response:** `204 No Content`

## Search Capabilities

### Filter Types

#### Text Filtering
```json
{
  \"clientName\": \"Acme\"  // Case-insensitive partial match
}
```

#### Exact Matching
```json
{
  \"clientNumber\": \"CLI-001\",  // Exact match
  \"deleted\": false              // Boolean exact match
}
```

#### Range Filtering
```json
{
  \"minVersion\": 1,    // Minimum version (inclusive)
  \"maxVersion\": 10    // Maximum version (inclusive)
}
```

#### External Reference Filtering
```json
{
  \"externalReferenceType\": \"CRM_ID\",
  \"externalReferenceValue\": \"CRM-12345\"
}
```

### Pagination and Sorting

```json
{
  \"page\": 0,              // Page number (0-indexed)
  \"size\": 20,             // Page size (1-100)
  \"sortBy\": \"clientName\", // Sort field
  \"sortDirection\": \"ASC\"   // ASC or DESC
}
```

### Advanced Search Examples

#### Multi-field Search
```json
{
  \"clientName\": \"Corp\",
  \"deleted\": false,
  \"minVersion\": 1,
  \"externalReferenceType\": \"CRM_ID\",
  \"page\": 0,
  \"size\": 50,
  \"sortBy\": \"clientName\",
  \"sortDirection\": \"ASC\"
}
```

#### Search with External References
```json
{
  \"externalReferenceType\": \"ERP_ID\",
  \"externalReferenceValue\": \"ERP-67890\"
}
```

## Configuration

### MongoDB Configuration

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/reactive-pocs
      database: reactive-pocs
      
# Index configuration for optimal performance
```

### WebFlux Configuration

```yaml
spring:
  webflux:
    multipart:
      max-in-memory-size: 1MB
    static-path-pattern: /static/**
```

### Client Module Configuration

```yaml
client:
  search:
    default-page-size: 20
    max-page-size: 100
    default-sort-field: id
    enable-full-text-search: true
    
  validation:
    enable-strict-validation: true
    max-client-name-length: 100
    
  performance:
    enable-query-caching: true
    cache-ttl: 300  # seconds
```

## Usage Examples

### 1. Basic Client Search

```java
@RestController
public class ExampleController {
    
    private final GenericSearchService searchService;
    
    public Mono<ClientPage> searchActiveClients() {
        ClientSummaryFilter filter = ClientSummaryFilter.builder()
            .deleted(false)
            .page(0)
            .size(20)
            .sortBy(\"clientName\")
            .sortDirection(\"ASC\")
            .build();
            
        return searchService.searchPaginated(
            filter, 
            ClientEntity.class, 
            ClientSummaryDto.class);
    }
}
```

### 2. Complex Search with Multiple Filters

```java
public Mono<ClientPage> searchComplexClients() {
    ClientSummaryFilter filter = ClientSummaryFilter.builder()
        .clientName(\"Corporation\")
        .deleted(false)
        .minVersion(1)
        .maxVersion(5)
        .externalReferenceType(\"CRM_ID\")
        .page(0)
        .size(50)
        .sortBy(\"lastUpdateUserId\")
        .sortDirection(\"DESC\")
        .build();
        
    return searchService.searchPaginated(
        filter, 
        ClientEntity.class, 
        ClientSummaryDto.class);
}
```

### 3. Single Client Lookup

```java
public Mono<ClientSummaryDto> findClientByNumber(String clientNumber) {
    ClientSummaryFilter filter = ClientSummaryFilter.builder()
        .clientNumber(clientNumber)
        .build();
        
    return searchService.searchOne(
        filter, 
        ClientEntity.class, 
        ClientSummaryDto.class);
}
```

### 4. Client CRUD Operations

```java
@Service
public class ClientService {
    
    private final GenericRepository repository;
    
    public Mono<ClientEntity> createClient(ClientSummaryDto dto) {
        ClientEntity entity = mapToEntity(dto);
        return repository.save(entity);
    }
    
    public Mono<ClientEntity> updateClient(Long id, ClientSummaryDto dto) {
        return repository.findById(id, ClientEntity.class)
            .map(existing -> updateEntityFromDto(existing, dto))
            .flatMap(repository::save);
    }
    
    public Mono<Void> deleteClient(Long id) {
        return repository.deleteById(id, ClientEntity.class);
    }
}
```

## Error Handling

### Exception Types

#### EntityNotFoundException
```java
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
```

#### ValidationException
```java
public class ValidationException extends RuntimeException {
    private final List<String> errors;
    
    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}
```

### Error Response Handling

```java
@Component
public class ClientExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(404)
            .error(\"Not Found\")
            .message(e.getMessage())
            .build();
            
        return ResponseEntity.status(404).body(error);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(400)
            .error(\"Validation Failed\")
            .message(e.getMessage())
            .details(e.getErrors())
            .build();
            
        return ResponseEntity.status(400).body(error);
    }
}
```

## Performance

### Query Optimization

#### MongoDB Indexes
```javascript
// Recommended indexes for optimal performance
db.clients.createIndex({ \"clientName\": \"text\" });
db.clients.createIndex({ \"clientNumber\": 1 });
db.clients.createIndex({ \"deleted\": 1 });
db.clients.createIndex({ \"lastUpdateUserId\": 1 });
db.clients.createIndex({ \"version\": 1 });
db.clients.createIndex({ \"externalReferences.externalReferenceType\": 1 });
db.clients.createIndex({ \"externalReferences.externalReferenceValue\": 1 });

// Compound indexes for common query patterns
db.clients.createIndex({ \"deleted\": 1, \"clientName\": 1 });
db.clients.createIndex({ \"clientName\": \"text\", \"deleted\": 1 });
```

#### Query Patterns
```java
// Efficient query building
Query query = new Query();

// Use indexed fields first
query.addCriteria(Criteria.where(\"deleted\").is(false));

// Add text search with index
if (hasText(filter.getClientName())) {
    query.addCriteria(Criteria.where(\"clientName\").regex(filter.getClientName(), \"i\"));
}

// Limit results for performance
query.limit(filter.getSize() + 1); // +1 to check if there are more results
```

### Memory Management

#### Streaming Results
```java
public Flux<ClientSummaryDto> streamClients(ClientSummaryFilter filter) {
    Query query = buildQuery(filter);
    
    return repository.findWithQuery(query, ClientEntity.class)
        .map(this::mapToDto)
        .doOnNext(client -> log.debug(\"Streamed client: {}\", client.getId()));
}
```

#### Pagination Optimization
```java
// Efficient pagination without counting for large datasets
public Mono<ClientPage> searchPaginatedOptimized(ClientSummaryFilter filter) {
    Query query = buildQuery(filter);
    query.limit(filter.getSize() + 1); // +1 to detect next page
    
    return repository.findWithQuery(query, ClientEntity.class)
        .collectList()
        .map(results -> buildOptimizedPageResponse(results, filter));
}
```

## Testing

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class GenericSearchServiceTest {
    
    @Mock
    private GenericRepository repository;
    
    @Mock
    private FilterProcessor filterProcessor;
    
    @InjectMocks
    private GenericSearchService searchService;
    
    @Test
    void shouldSearchClientsWithFilter() {
        // Given
        ClientSummaryFilter filter = createTestFilter();
        ProcessedFilter processedFilter = createProcessedFilter();
        List<ClientEntity> entities = createTestEntities();
        
        when(filterProcessor.processFilter(filter)).thenReturn(processedFilter);
        when(repository.findWithQuery(any(), eq(ClientEntity.class)))
            .thenReturn(Flux.fromIterable(entities));
        when(repository.countWithQuery(any(), eq(ClientEntity.class)))
            .thenReturn(Mono.just(10L));
        
        // When
        Mono<ClientPage> result = searchService.searchPaginated(
            filter, ClientEntity.class, ClientSummaryDto.class);
        
        // Then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page.getContent()).hasSize(2);
                assertThat(page.getTotalElements()).isEqualTo(10);
            })
            .verifyComplete();
    }
}
```

### Integration Testing

```java
@SpringBootTest
@Testcontainers
class ClientModuleIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(\"mongo:6.0.2\");
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldSearchClientsEndToEnd() {
        // Given
        ClientSummaryFilter filter = ClientSummaryFilter.builder()
            .clientName(\"Test Client\")
            .deleted(false)
            .page(0)
            .size(20)
            .build();
        
        // When & Then
        webTestClient.post()
            .uri(\"/api/v1/clients/search\")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(filter), ClientSummaryFilter.class)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ClientPage.class)
            .value(page -> {
                assertThat(page.getContent()).isNotEmpty();
                assertThat(page.getTotalElements()).isGreaterThan(0);
            });
    }
}
```

## Best Practices

### 1. Controller Design

```java
// ✅ Good: Use reactive types consistently
@PostMapping(\"/search\")
public Mono<ResponseEntity<ClientPage>> search(@RequestBody Mono<ClientSummaryFilter> filter) {
    return filter
        .flatMap(f -> searchService.searchPaginated(f, ClientEntity.class, ClientSummaryDto.class))
        .map(ResponseEntity::ok);
}

// ❌ Avoid: Blocking operations in controllers
@PostMapping(\"/search\")
public ResponseEntity<ClientPage> search(@RequestBody ClientSummaryFilter filter) {
    ClientPage page = searchService.searchPaginated(filter).block(); // DON'T BLOCK!
    return ResponseEntity.ok(page);
}
```

### 2. Error Handling

```java
// ✅ Good: Reactive error handling
public Mono<ClientSummaryDto> findClient(Long id) {
    return repository.findById(id, ClientEntity.class)
        .map(this::mapToDto)
        .onErrorMap(Exception.class, e -> new ServiceException(\"Error finding client\", e))
        .switchIfEmpty(Mono.error(new EntityNotFoundException(\"Client not found: \" + id)));
}

// ❌ Avoid: Imperative error handling
public ClientSummaryDto findClient(Long id) {
    try {
        ClientEntity entity = repository.findById(id, ClientEntity.class).block();
        if (entity == null) {
            throw new EntityNotFoundException(\"Client not found: \" + id);
        }
        return mapToDto(entity);
    } catch (Exception e) {
        throw new ServiceException(\"Error finding client\", e);
    }
}
```

### 3. Query Building

```java
// ✅ Good: Efficient query building
private Query buildOptimizedQuery(ClientSummaryFilter filter) {
    Query query = new Query();
    
    // Add most selective criteria first
    if (filter.getClientNumber() != null) {
        query.addCriteria(Criteria.where(\"clientNumber\").is(filter.getClientNumber()));
    }
    
    // Add indexed criteria
    if (filter.getDeleted() != null) {
        query.addCriteria(Criteria.where(\"deleted\").is(filter.getDeleted()));
    }
    
    // Add text search last
    if (hasText(filter.getClientName())) {
        query.addCriteria(Criteria.where(\"clientName\").regex(filter.getClientName(), \"i\"));
    }
    
    return query;
}
```

### 4. Mapping and Validation

```java
// ✅ Good: Validation and mapping
private ClientSummaryDto mapToDto(ClientEntity entity) {
    if (entity == null) {
        return null;
    }
    
    return ClientSummaryDto.builder()
        .id(entity.getId())
        .version(entity.getVersion())
        .clientName(entity.getClientName())
        .clientNumber(entity.getClientNumber())
        .lastUpdateUserId(entity.getLastUpdateUserId())
        .deleted(entity.getDeleted())
        .externalReferences(mapExternalReferences(entity.getExternalReferences()))
        .build();
}
```

---

## Contributing

When contributing to the Client Management Module:

1. **Follow reactive patterns**: Use Mono/Flux throughout
2. **Add comprehensive tests**: Both unit and integration tests
3. **Update documentation**: Keep README and API docs current
4. **Monitor performance**: Ensure queries are efficient
5. **Handle errors properly**: Use reactive error handling patterns

For questions or issues, refer to the main project README or create an issue in the project repository.