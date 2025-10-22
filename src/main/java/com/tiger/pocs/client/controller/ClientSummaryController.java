package com.tiger.pocs.client.controller;

import com.tiger.pocs.payload.ClientSummaryDto;
import com.tiger.pocs.client.search.GenericSearchService;
import com.tiger.pocs.payload.ClientSummaryFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientSummaryController extends BaseController {

    private final GenericSearchService searchService;

    @PostMapping
    public Mono<ResponseEntity<Page<ClientSummaryDto>>> searchPaginated(@RequestBody ClientSummaryFilter filter) {
        return logMono(searchService.searchPaginated(filter, ClientSummaryDto.class), 
                Map.of("filter", filter, "operation", "searchPaginated"))
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/one")
    public Mono<ResponseEntity<ClientSummaryDto>> searchOne(@RequestBody ClientSummaryFilter filter) {
        return logMono(searchService.findOne(filter, ClientSummaryDto.class),
                Map.of("filter", filter, "operation", "findOne"))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}