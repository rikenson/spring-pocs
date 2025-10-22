package com.tiger.pocs.ingestion.domain;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ExternalReferenceEntity {
    private Boolean deleted;
}