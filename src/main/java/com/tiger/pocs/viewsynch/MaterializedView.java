package com.tiger.pocs.viewsynch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SCHEDULED
    }
}