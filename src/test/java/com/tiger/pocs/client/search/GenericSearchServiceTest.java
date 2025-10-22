package com.tiger.pocs.client.search;

import com.tiger.pocs.payload.ClientSummaryFilter;
import com.tiger.pocs.payload.ClientSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericSearchServiceTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;
    
    @Mock
    private QueryBuilder queryBuilder;
    
    @Mock
    private CollectionResolver collectionResolver;
    
    @Mock
    private PaginationExtractor paginationExtractor;

    private GenericSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new GenericSearchService(mongoTemplate, queryBuilder, collectionResolver, paginationExtractor);
        
        // Setup default behaviors
        when(collectionResolver.resolveCollection(any())).thenReturn("client_summary");
        when(queryBuilder.buildQuery(any())).thenReturn(new Query());
    }

    @Test
    void testSearchWithTextFields() {
        // Given
        ClientSummaryFilter filter = new ClientSummaryFilter();
        filter.setClientName("John");
        
        List<ClientSummaryDto> expectedResults = Arrays.asList(
            createClientDto("John Doe"),
            createClientDto("John Smith")
        );

        when(mongoTemplate.find(any(Query.class), eq(ClientSummaryDto.class), anyString()))
            .thenReturn(Flux.fromIterable(expectedResults));

        // When
        Flux<ClientSummaryDto> result = searchService.search(filter, ClientSummaryDto.class);

        // Then
        StepVerifier.create(result)
            .expectNextCount(2)
            .verifyComplete();

        verify(mongoTemplate).find(any(Query.class), eq(ClientSummaryDto.class), eq("client_summary"));
        verify(queryBuilder).buildQuery(filter);
    }

    @Test
    void testSearchWithRangeFields() {
        // Given
        ClientSummaryFilter filter = new ClientSummaryFilter();
        filter.setMinVersion(1);
        filter.setMaxVersion(5);
        
        List<ClientSummaryDto> expectedResults = Arrays.asList(createClientDto("Client1"));

        when(mongoTemplate.find(any(Query.class), eq(ClientSummaryDto.class), anyString()))
            .thenReturn(Flux.fromIterable(expectedResults));

        // When
        Flux<ClientSummaryDto> result = searchService.search(filter, ClientSummaryDto.class);

        // Then
        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void testSearchPaginated() {
        // Given
        ClientSummaryFilter filter = new ClientSummaryFilter();
        
        List<ClientSummaryDto> pageContent = Arrays.asList(
            createClientDto("Client1"),
            createClientDto("Client2")
        );
        
        when(paginationExtractor.extractPageRequest(any())).thenReturn(PageRequest.of(0, 20));
        when(mongoTemplate.find(any(Query.class), eq(ClientSummaryDto.class), anyString()))
            .thenReturn(Flux.fromIterable(pageContent));
        when(mongoTemplate.count(any(Query.class), anyString()))
            .thenReturn(Mono.just(2L));

        // When
        Mono<Page<ClientSummaryDto>> result = searchService.searchPaginated(filter, ClientSummaryDto.class);

        // Then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page.getContent()).hasSize(2);
                assertThat(page.getTotalElements()).isEqualTo(2);
                assertThat(page.getNumber()).isEqualTo(0);
                assertThat(page.getSize()).isEqualTo(20); // Default size
            })
            .verifyComplete();
    }

    @Test
    void testFindOne() {
        // Given
        ClientSummaryFilter filter = new ClientSummaryFilter();
        filter.setClientName("John");
        
        ClientSummaryDto expectedResult = createClientDto("John Doe");

        when(mongoTemplate.findOne(any(Query.class), eq(ClientSummaryDto.class), anyString()))
            .thenReturn(Mono.just(expectedResult));

        // When
        Mono<ClientSummaryDto> result = searchService.findOne(filter, ClientSummaryDto.class);

        // Then
        StepVerifier.create(result)
            .expectNext(expectedResult)
            .verifyComplete();
    }

    @Test
    void testSearchWithHasFields() {
        // Given
        ClientSummaryFilter filter = new ClientSummaryFilter();
        filter.setDeleted(true);
        
        List<ClientSummaryDto> expectedResults = Arrays.asList(createClientDto("Client1"));

        when(mongoTemplate.find(any(Query.class), eq(ClientSummaryDto.class), anyString()))
            .thenReturn(Flux.fromIterable(expectedResults));

        // When
        Flux<ClientSummaryDto> result = searchService.search(filter, ClientSummaryDto.class);

        // Then
        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete();

        verify(mongoTemplate).find(any(Query.class), eq(ClientSummaryDto.class), anyString());
        verify(queryBuilder).buildQuery(filter);
    }

    @Test
    void testSearchWithSorting() {
        // Given
        ClientSummaryFilter filter = new ClientSummaryFilter();
        filter.setSortBy("clientName");
        filter.setSortDirection(ClientSummaryFilter.SortDirectionEnum.ASC);
        
        List<ClientSummaryDto> expectedResults = Arrays.asList(
            createClientDto("Alice"),
            createClientDto("Bob")
        );

        when(mongoTemplate.find(any(Query.class), eq(ClientSummaryDto.class), anyString()))
            .thenReturn(Flux.fromIterable(expectedResults));

        // When
        Flux<ClientSummaryDto> result = searchService.search(filter, ClientSummaryDto.class);

        // Then
        StepVerifier.create(result)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void testSearchWithNullFilter() {
        // Given
        List<ClientSummaryDto> expectedResults = Arrays.asList(createClientDto("Client1"));

        when(mongoTemplate.find(any(Query.class), eq(ClientSummaryDto.class), anyString()))
            .thenReturn(Flux.fromIterable(expectedResults));

        // When
        Flux<ClientSummaryDto> result = searchService.search(null, ClientSummaryDto.class);

        // Then
        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void testForTypeMethod() {
        // Given
        ClientSummaryFilter filter = new ClientSummaryFilter();
        filter.setClientName("Test");
        
        List<ClientSummaryDto> expectedResults = Arrays.asList(createClientDto("Test Client"));

        when(mongoTemplate.find(any(Query.class), eq(ClientSummaryDto.class), anyString()))
            .thenReturn(Flux.fromIterable(expectedResults));

        // When
        SearchService<ClientSummaryDto, ClientSummaryFilter> typedService = 
            searchService.forType(ClientSummaryDto.class);
        Flux<ClientSummaryDto> result = typedService.search(filter);

        // Then
        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete();
    }

    private ClientSummaryDto createClientDto(String name) {
        ClientSummaryDto dto = new ClientSummaryDto();
        dto.setClientName(name);
        return dto;
    }
}