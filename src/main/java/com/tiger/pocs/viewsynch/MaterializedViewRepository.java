package com.tiger.pocs.viewsynch;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterializedViewRepository extends MongoRepository<MaterializedView, String> {
    
    Optional<MaterializedView> findByViewName(String viewName);
    
    List<MaterializedView> findByStatus(MaterializedView.RefreshStatus status);

}