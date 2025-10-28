package com.tiger.pocs.tester;

import com.ffelicite.kernel.mapper.GenericMapper;
import org.mapstruct.*;
import org.senbyoz.ffelicite.dto.CategoryRequest;
import org.senbyoz.ffelicite.dto.CategoryResponse;
import org.senbyoz.ffelicite.dto.PatchedCategoryRequest;

/**
 * ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 * Game Mapper implementation
 * ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 */

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CategoryMapper extends GenericMapper<CategoryEntity, CategoryRequest, PatchedCategoryRequest, CategoryResponse> {

    @Override
    @Mapping(target = "description", source = "description", qualifiedByName = "mapStringToMap")
    @Mapping(target = "name", source = "name", qualifiedByName = "mapStringToMap")
    CategoryEntity toEntity(CategoryRequest request);

    @Override
    @Mapping(target = "description", expression = "java(mapUpdateStringToMap(request.getDescription(), entity.getDescription()))")
    @Mapping(target = "name", expression = "java(mapUpdateStringToMap(request.getName(), entity.getName()))")
    void toUpdatedEntity(PatchedCategoryRequest request, @MappingTarget CategoryEntity entity);

    @Override
    @Mapping(target = "description", source = "description", qualifiedByName = "mapMapToString")
    @Mapping(target = "name", source = "name", qualifiedByName = "mapMapToString")
    CategoryResponse toResponse(CategoryEntity entity);
}