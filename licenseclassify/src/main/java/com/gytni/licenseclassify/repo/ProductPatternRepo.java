package com.gytni.licenseclassify.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.gytni.licenseclassify.model.ProductPattern;

public interface ProductPatternRepo extends CrudRepository<ProductPattern, UUID> {
    List<ProductPattern> findByWorkingSetId(UUID id);
    List<ProductPattern> findByUnclassified(boolean unclassified);
    List<ProductPattern> findByWorkingSetIdOrderByCreatedDesc(UUID workingSetId);
    List<ProductPattern> findByWorkingSetIdAndUnclassifiedFalse(UUID workingSetId);
}
