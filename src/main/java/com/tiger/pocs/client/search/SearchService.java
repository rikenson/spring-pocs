package com.tiger.pocs.client.search;

import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SearchService<T, F> {
    
    Flux<T> search(F filter);
    
    Mono<Page<T>> searchPaginated(F filter);
    
    Mono<T> findOne(F filter);
}