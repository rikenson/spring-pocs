package com.tiger.pocs.viewsynch;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<MaterializedView>> getAllMaterializedViews() {
        List<MaterializedView> views = materializedViewService.getAllViews();
        return ResponseEntity.ok(views);
    }

    @GetMapping("/{viewName}")
    public ResponseEntity<MaterializedView> getMaterializedView(@PathVariable String viewName) {
        return materializedViewService.getView(viewName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{viewName}/refresh")
    public ResponseEntity<Void> refreshMaterializedView(@PathVariable String viewName) {
        materializedViewService.refreshView(viewName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/definitions")
    public ResponseEntity<List<MaterializedView>> getAvailableViewDefinitions() {
        List<MaterializedView> definitions = viewConfigurationService.getAllViewDefinitions();
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/definitions/{viewName}")
    public ResponseEntity<MaterializedView> getViewDefinition(@PathVariable String viewName) {
        return viewConfigurationService.getViewDefinition(viewName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/schedule/reinitialize")
    public ResponseEntity<Void> reinitializeSchedules() {
        materializedViewScheduler.scheduleAllViews();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{viewName}/schedule/cancel")
    public ResponseEntity<Void> cancelViewSchedule(@PathVariable String viewName) {
        materializedViewScheduler.cancelViewRefresh(viewName);
        return ResponseEntity.ok().build();
    }
}