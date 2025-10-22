package com.tiger.pocs.client.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenericSearchService {
    
    private final ReactiveMongoTemplate mongoTemplate;
    private final QueryBuilder queryBuilder;
    private final CollectionResolver collectionResolver;
    private final PaginationExtractor paginationExtractor;
    
    // Generic search methods
    public <T> Flux<T> search(Object filter, Class<T> resultType) {
        Query query = queryBuilder.buildQuery(filter);
        String collection = collectionResolver.resolveCollection(resultType);
        return mongoTemplate.find(query, resultType, collection);
    }
    
    public <T> Mono<Page<T>> searchPaginated(Object filter, Class<T> resultType) {
        Query query = queryBuilder.buildQuery(filter);
        String collection = collectionResolver.resolveCollection(resultType);
        PageRequest pageRequest = paginationExtractor.extractPageRequest(filter);
        
        Query paginatedQuery = Query.of(query).with(pageRequest);
        
        return mongoTemplate.find(paginatedQuery, resultType, collection)
                .collectList()
                .zipWith(mongoTemplate.count(query, collection))
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageRequest, tuple.getT2()));
    }
    
    public <T> Mono<T> findOne(Object filter, Class<T> resultType) {
        Query query = queryBuilder.buildQuery(filter);
        String collection = collectionResolver.resolveCollection(resultType);
        return mongoTemplate.findOne(query, resultType, collection);
    }
    
    // Helper method for creating typed search service instances
    @SuppressWarnings("unchecked")
    public <T, F> SearchService<T, F> forType(Class<T> resultType) {
        return new SearchService<T, F>() {
            @Override
            public Flux<T> search(F filter) {
                return GenericSearchService.this.search(filter, resultType);
            }
            
            @Override
            public Mono<Page<T>> searchPaginated(F filter) {
                return GenericSearchService.this.searchPaginated(filter, resultType);
            }
            
            @Override
            public Mono<T> findOne(F filter) {
                return GenericSearchService.this.findOne(filter, resultType);
            }
        };
    }
}