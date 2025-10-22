package com.tiger.pocs.ingestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "log_offsets")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogOffsetEntity {
    @Id
    private String id;
}