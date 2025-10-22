package com.tiger.pocs.viewsynch;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterializedViewScheduler {

    private final MaterializedViewService materializedViewService;
    private final ViewConfigurationService viewConfigurationService;
    
    private TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("materialized-view-scheduler-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
        
        scheduleAllViews();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down materialized view scheduler");
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
        
        if (taskScheduler instanceof ThreadPoolTaskScheduler) {
            ((ThreadPoolTaskScheduler) taskScheduler).shutdown();
        }
    }

    public void scheduleAllViews() {
        log.info("Initializing scheduled refresh for all materialized views");
        
        List<MaterializedView> views = viewConfigurationService.getAllViewDefinitions();
        log.info("Found {} view definitions to schedule", views.size());
        
        for (MaterializedView view : views) {
            log.info("Processing view: {} with schedule: {}", view.getViewName(), view.getRefreshSchedule());
            scheduleViewRefresh(view);
        }
    }

    public void scheduleViewRefresh(MaterializedView view) {
        String viewName = view.getViewName();
        String schedule = view.getRefreshSchedule();
        
        if (schedule == null || schedule.trim().isEmpty()) {
            log.debug("No refresh schedule defined for view: {}", viewName);
            return;
        }

        try {
            // Cancel existing scheduled task if any
            ScheduledFuture<?> existingTask = scheduledTasks.get(viewName);
            if (existingTask != null) {
                existingTask.cancel(false);
            }

            // Create cron trigger for 6-field cron expressions (with seconds)
            CronTrigger cronTrigger = new CronTrigger(schedule);
            
            // Schedule the refresh task
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                () -> refreshView(viewName),
                cronTrigger
            );
            
            scheduledTasks.put(viewName, scheduledTask);
            log.info("Scheduled automatic refresh for view '{}' with schedule: {}", viewName, schedule);
            
        } catch (Exception e) {
            log.error("Failed to schedule refresh for view '{}' with schedule '{}': {}", 
                    viewName, schedule, e.getMessage());
        }
    }

    private void refreshView(String viewName) {
        try {
            log.info("Executing scheduled refresh for materialized view: {}", viewName);
            materializedViewService.refreshView(viewName);
            log.info("Successfully completed scheduled refresh for view: {}", viewName);
        } catch (Exception e) {
            log.error("Failed to execute scheduled refresh for view '{}': {}", viewName, e.getMessage(), e);
        }
    }

    public void cancelViewRefresh(String viewName) {
        ScheduledFuture<?> task = scheduledTasks.remove(viewName);
        if (task != null) {
            task.cancel(false);
            log.info("Cancelled scheduled refresh for view: {}", viewName);
        }
    }
}