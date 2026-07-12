package com.edunac.mentora.service.learning;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learning.ContentType;
import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.level.Material;
import com.edunac.mentora.dto.NodeContentForm;
import com.edunac.mentora.repository.learning.NodeContentRepository;
import com.edunac.mentora.service.level.MaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NodeContentServiceMaterialTest {

    private NodeContentRepository repository;
    private LearningNodeService learningNodeService;
    private NodeContentStorageService storageService;
    private MaterialService materialService;
    private NodeContentService service;
    private User lecturer;
    private Material material;

    @BeforeEach
    void setUp() {
        repository = mock(NodeContentRepository.class);
        learningNodeService = mock(LearningNodeService.class);
        storageService = mock(NodeContentStorageService.class);
        materialService = mock(MaterialService.class);
        service = new NodeContentService(repository, learningNodeService, storageService, materialService);

        lecturer = new User();
        lecturer.setId(7);

        material = new Material();
        material.setId(11);
        when(materialService.requireOwned(material.getId(), lecturer)).thenReturn(material);
    }

    @Test
    void getByMaterialIdRequiresOwnershipAndReturnsMaterialContents() {
        NodeContent content = new NodeContent();
        when(repository.findByMaterial_IdOrderByDisplayOrderAscIdAsc(material.getId()))
                .thenReturn(List.of(content));

        List<NodeContent> contents = service.getByMaterialId(material.getId(), lecturer);

        assertEquals(List.of(content), contents);
        verify(materialService).requireOwned(material.getId(), lecturer);
    }

    @Test
    void saveMaterialFormStoresContentWithMaterialOwnerOnly() {
        NodeContentForm form = new NodeContentForm();
        form.setMaterialId(material.getId());
        form.setContentType(ContentType.TEXT.name());
        form.setTitle("Intro");
        form.setContentText("Read this first");
        form.setDisplayOrder(2);
        when(repository.save(any(NodeContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveMaterialForm(form, null, lecturer);

        verify(repository).save(any(NodeContent.class));
        NodeContent saved = savedContent();
        assertSame(material, saved.getMaterial());
        assertNull(saved.getNode());
        assertEquals("TEXT", saved.getContentType());
        assertEquals("Read this first", saved.getContentText());
        assertEquals(2, saved.getDisplayOrder());
    }

    @Test
    void findByIdAndMaterialIdRejectsContentWithBothNodeAndMaterialOwners() {
        NodeContent content = new NodeContent();
        content.setMaterial(material);
        content.setNode(new LearningNode());
        when(repository.findByIdAndMaterial_Id(21, material.getId())).thenReturn(Optional.of(content));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.findByIdAndMaterialId(21, material.getId(), lecturer));

        assertEquals("Nội dung không được thuộc đồng thời node và material.", ex.getMessage());
    }

    @Test
    void deleteMaterialContentUsesMaterialScopedLookupAndDeletesManagedFile() {
        NodeContent content = new NodeContent();
        content.setMaterial(material);
        content.setContentUrl("/uploads/materials/file.pdf");
        when(repository.findByIdAndMaterial_Id(21, material.getId())).thenReturn(Optional.of(content));

        service.deleteMaterialContent(21, material.getId(), lecturer);

        verify(storageService).deleteIfManaged("/uploads/materials/file.pdf");
        verify(repository).delete(content);
    }

    @Test
    void findByIdAndMaterialIdReturnsNotFoundForWrongMaterial() {
        when(repository.findByIdAndMaterial_Id(21, material.getId())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.findByIdAndMaterialId(21, material.getId(), lecturer));
    }

    private NodeContent savedContent() {
        return org.mockito.Mockito.mockingDetails(repository)
                .getInvocations()
                .stream()
                .filter(invocation -> "save".equals(invocation.getMethod().getName()))
                .map(invocation -> (NodeContent) invocation.getArgument(0))
                .findFirst()
                .orElseThrow();
    }
}
