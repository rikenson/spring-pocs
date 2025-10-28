package com.tiger.pocs.tester;

import com.ffelicite.category.domain.CategoryEntity;
import com.ffelicite.collaborator.domain.CollaboratorEntity;
import com.ffelicite.event.domain.StakeholderEntity;
import com.ffelicite.gallery.domain.GalleryEntity;
import com.ffelicite.gallery.domain.GalleryItemEntity;
import com.ffelicite.kernel.config.LocaleContextHolder;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.senbyoz.ffelicite.dto.*;

import java.util.*;

public interface GenericMapper<E, NR, UR, R> {

    E toEntity(NR request);

    void toUpdatedEntity(UR request, @MappingTarget E entity);

    R toResponse(E entity);

    default String getCurrentLanguage() {
        return LocaleContextHolder.getLocale().getLanguage();
    }

    @Named("mapStringToMap")
    default Map<String, String> mapStringToMap(String value) {
        return value == null ? Collections.emptyMap() : Map.of(getCurrentLanguage(), value);
    }

    @Named("mapMapToString")
    default String mapMapToString(Map<String, String> map) {
        if (Objects.isNull(map) || map.isEmpty()) {
            return null;
        }
        return map.getOrDefault(getCurrentLanguage(), map.get("en"));
    }

    @Named("mapToListSimpleCollaborator")
    default List<SimpleCollaboratorResponse> mapToListSimpleCollaborator(List<CollaboratorEntity> collaborators) {
        if (Objects.isNull(collaborators) || collaborators.isEmpty()) {
            return Collections.emptyList();
        }
        return collaborators.stream().map(this::mapToSimpleCollaborator).toList();
    }

    @Named("mapToSimpleCollaborator")
    default SimpleCollaboratorResponse mapToSimpleCollaborator(CollaboratorEntity collaborator) {
        if (Objects.isNull(collaborator)) {
            return null;
        }
        var simpleCollaborator = new SimpleCollaboratorResponse();
        simpleCollaborator.setId(collaborator.getId());
        simpleCollaborator.setFullName(collaborator.getFirstName() + " " + collaborator.getLastName());
        simpleCollaborator.setEmails(collaborator.getEmails());
        simpleCollaborator.setCategory(mapCategoryToSimpleCategory(collaborator.getCategory()));
        simpleCollaborator.setPhones(collaborator.getPhones());
        return simpleCollaborator;
    }

    @Named("mapToListStakeholder")
    default List<StakeholderResponse> mapToListStakeholder(List<StakeholderEntity> stakeholders) {
        if (Objects.isNull(stakeholders) || stakeholders.isEmpty()) {
            return Collections.emptyList();
        }
        return stakeholders.stream().map(this::mapToSimpleStakeholder).toList();
    }


    @Named("mapToSimpleStakeholder")
    default StakeholderResponse mapToSimpleStakeholder(StakeholderEntity stakeholder) {
        if (Objects.isNull(stakeholder)) {
            return null;
        }
        var response = new StakeholderResponse();
        var role = stakeholder.getRole();
        response.setCollaborator(mapToSimpleCollaborator(stakeholder.getCollaborator()));
        response.setRole(role.getOrDefault(getCurrentLanguage(), role.get("en")));
        return response;
    }

    @Named("mapGalleryItems")
    default List<SimpleGalleryItemResponse> mapGalleryItems(List<GalleryItemEntity> galleryItems) {
        if (Objects.isNull(galleryItems) || galleryItems.isEmpty()) {
            return Collections.emptyList();
        }
        return galleryItems.stream().map(gItem -> {
            var item = new SimpleGalleryItemResponse();
            item.setId(gItem.getId());
            item.setName(gItem.getName().get(getCurrentLanguage()));
            item.setDescription(gItem.getDescription().get(getCurrentLanguage()));
            return item;
        }).toList();

    }


    @Named("mapCategoryToSimpleCategory")
    default SimpleCategory mapCategoryToSimpleCategory(CategoryEntity category) {
        if (Objects.isNull(category)) {
            return null;
        }
        var simpleCollaborator = new SimpleCategory();
        simpleCollaborator.setId(category.getId());
        simpleCollaborator.setName(category.getName().get(getCurrentLanguage()));
        return simpleCollaborator;

    }


    @Named("mapToListGallery")
    default List<SimpleGalleryResponse> mapToListGallery(List<GalleryEntity> galleries) {
        if (Objects.isNull(galleries) || galleries.isEmpty()) {
            return Collections.emptyList();
        }
        return galleries.stream().map(this::mapToSimpleGallery).toList();
    }


    @Named("mapToSimpleGallery")
    default SimpleGalleryResponse mapToSimpleGallery(GalleryEntity gallery) {
        if (Objects.isNull(gallery)) {
            return null;
        }



        var response = new SimpleGalleryResponse();
        if(Objects.isNull(response.getItems())){
            response.setItems(new ArrayList<>());
        }
        response.setId(gallery.getId());
        response.setName(gallery.getName().get(getCurrentLanguage()));
        response.getItems().addAll(mapGalleryItems(gallery.getItems()));
        response.setCategory(mapCategoryToSimpleCategory(gallery.getCategory()));
        return response;

    }


    default Map<String, String> mapUpdateStringToMap(String value, Map<String, String> existingMap) {
        if (Objects.isNull(existingMap)) {
            existingMap = new HashMap<>();
        }
        if (Objects.nonNull(value)) {
            existingMap.put(getCurrentLanguage(), value);
        }
        return existingMap.isEmpty() ? Collections.emptyMap() : existingMap;
    }
}
