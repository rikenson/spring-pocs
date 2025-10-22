package com.tiger.pocs.viewsynch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ViewConfigurationService {

    private static final TypeReference<List<MaterializedView>> VIEW_LIST_TYPE = new TypeReference<>() {
    };
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private List<MaterializedView> viewDefinitions = List.of();

    @PostConstruct
    void loadViewConfigurations() {
        ClassPathResource resource = new ClassPathResource("views.yml");
        if (!resource.exists()) {
            log.warn("views.yml not found in classpath");
            return;
        }

        try {
            this.viewDefinitions = YAML_MAPPER.readValue(resource.getInputStream(), VIEW_LIST_TYPE);
            log.info("Loaded {} view definitions from views.yml", viewDefinitions.size());
            
            // Log each view definition for debugging
            for (MaterializedView view : viewDefinitions) {
                log.info("Loaded view: {} with schedule: {} targeting collection: {}", 
                        view.getViewName(), view.getRefreshSchedule(), view.getTargetCollection());
            }
        } catch (Exception e) {
            log.error("Failed to load view configurations from views.yml", e);
            throw new MaterializedViewException
                    .ConfigurationException("Failed to load view configurations from views.yml", e);
        }
    }

    public Optional<MaterializedView> getViewDefinition(String viewName) {
        return viewDefinitions.stream().filter(def -> def.getViewName().equals(viewName)).findFirst();
    }

    public List<MaterializedView> getAllViewDefinitions() {
        return viewDefinitions;
    }

}