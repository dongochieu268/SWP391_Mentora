package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.BankQuestion;
import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.domain.level.Material;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.dto.MaterialForm;
import com.edunac.mentora.dto.NodeContentForm;
import com.edunac.mentora.service.assessment.QuestionBankService;
import com.edunac.mentora.service.learning.NodeContentService;
import com.edunac.mentora.service.level.MaterialService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LecturerMaterialControllerTest {

    private MaterialService materialService;
    private QuestionBankService questionBankService;
    private SubjectService subjectService;
    private NodeContentService nodeContentService;
    private LecturerMaterialController controller;
    private User lecturer;
    private Subject subject;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        materialService = mock(MaterialService.class);
        questionBankService = mock(QuestionBankService.class);
        subjectService = mock(SubjectService.class);
        nodeContentService = mock(NodeContentService.class);
        controller = new LecturerMaterialController(
                materialService,
                questionBankService,
                subjectService,
                nodeContentService
        );

        lecturer = new User();
        lecturer.setId(7);

        subject = new Subject();
        subject.setId(3);
        subject.setCode("SWP391");
        subject.setName("Software Project");

        session = new MockHttpSession();
        session.setAttribute("loggedInUser", lecturer);
    }

    @Test
    void listUsesSelectedSubjectAndMaterialData() {
        Material material = new Material();
        material.setId(11);
        BankQuestion tagged = new BankQuestion();
        BankQuestion bankQuestion = new BankQuestion();
        NodeContent content = new NodeContent();
        when(subjectService.getActiveSubjects()).thenReturn(List.of(subject));
        when(materialService.findBySubject(subject.getId())).thenReturn(List.of(material));
        when(materialService.findQuestionsByMaterial(material.getId(), lecturer)).thenReturn(List.of(tagged));
        when(nodeContentService.getByMaterialId(material.getId(), lecturer)).thenReturn(List.of(content));
        when(questionBankService.findActiveBySubject(subject.getId(), null, null, null)).thenReturn(List.of(bankQuestion));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.list(subject.getId(), material.getId(), false, null, session, model);

        assertEquals("lecturer/material/list", view);
        assertEquals("materials", model.get("activePage"));
        assertSame(material, model.get("selectedMaterial"));
        assertEquals(List.of(tagged), model.get("materialQuestions"));
        assertEquals(List.of(content), model.get("materialContents"));
        assertEquals(List.of(bankQuestion), model.get("bankQuestions"));
    }

    @Test
    void listLoadsSelectedMaterialContentIntoFormForEditing() {
        Material material = new Material();
        material.setId(11);
        NodeContent content = new NodeContent();
        content.setId(21);
        NodeContentForm editForm = new NodeContentForm();
        editForm.setId(content.getId());
        editForm.setMaterialId(material.getId());
        editForm.setContentType("TEXT");
        editForm.setContentText("Edit me");
        when(subjectService.getActiveSubjects()).thenReturn(List.of(subject));
        when(materialService.findBySubject(subject.getId())).thenReturn(List.of(material));
        when(materialService.findQuestionsByMaterial(material.getId(), lecturer)).thenReturn(List.of());
        when(nodeContentService.getByMaterialId(material.getId(), lecturer)).thenReturn(List.of(content));
        when(questionBankService.findActiveBySubject(subject.getId(), null, null, null)).thenReturn(List.of());
        when(nodeContentService.findByIdAndMaterialId(content.getId(), material.getId(), lecturer))
                .thenReturn(content);
        when(nodeContentService.toForm(content)).thenReturn(editForm);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.list(subject.getId(), material.getId(), false, content.getId(), session, model);

        assertEquals("lecturer/material/list", view);
        assertSame(editForm, model.get("contentForm"));
        verify(nodeContentService).findByIdAndMaterialId(content.getId(), material.getId(), lecturer);
    }

    @Test
    void listCanStayInCreateModeEvenWhenSubjectHasMaterials() {
        Material material = new Material();
        material.setId(11);
        when(subjectService.getActiveSubjects()).thenReturn(List.of(subject));
        when(materialService.findBySubject(subject.getId())).thenReturn(List.of(material));
        when(questionBankService.findActiveBySubject(subject.getId(), null, null, null)).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.list(subject.getId(), null, true, null, session, model);

        assertEquals("lecturer/material/list", view);
        assertEquals(null, model.get("selectedMaterial"));
        MaterialForm form = (MaterialForm) model.get("form");
        assertEquals(subject.getId(), form.getSubjectId());
        assertEquals(null, form.getId());
    }

    @Test
    void createMaterialRedirectsToNewMaterialModeForFastSequentialEntry() {
        MaterialForm form = new MaterialForm();
        form.setSubjectId(subject.getId());
        form.setTitle("OOP Basics");
        form.setDescription("Encapsulation");

        String redirect = controller.create(
                form,
                new BeanPropertyBindingResult(form, "form"),
                session,
                new RedirectAttributesModelMap()
        );

        assertEquals("redirect:/lecturer/materials?subjectId=3&newMaterial=true", redirect);
        verify(materialService).create(subject.getId(), "OOP Basics", "Encapsulation", lecturer);
    }

    @Test
    void assignQuestionsDelegatesToMaterialService() {
        String redirect = controller.assignQuestions(
                11,
                subject.getId(),
                List.of(21, 22),
                session,
                new RedirectAttributesModelMap()
        );

        assertEquals("redirect:/lecturer/materials?subjectId=3&materialId=11", redirect);
        verify(materialService).assignQuestions(11, List.of(21, 22), lecturer);
    }

    @Test
    void saveMaterialContentDelegatesToNodeContentService() {
        NodeContentForm form = new NodeContentForm();
        form.setContentType("TEXT");
        form.setContentText("Study this");
        form.setDisplayOrder(1);

        String redirect = controller.saveContent(
                11,
                subject.getId(),
                form,
                new BeanPropertyBindingResult(form, "contentForm"),
                null,
                session,
                new RedirectAttributesModelMap()
        );

        assertEquals("redirect:/lecturer/materials?subjectId=3&materialId=11", redirect);
        assertEquals(11, form.getMaterialId());
        verify(nodeContentService).saveMaterialForm(form, null, lecturer);
    }

    @Test
    void deleteMaterialContentDelegatesToNodeContentService() {
        String redirect = controller.deleteContent(
                11,
                21,
                subject.getId(),
                session,
                new RedirectAttributesModelMap()
        );

        assertEquals("redirect:/lecturer/materials?subjectId=3&materialId=11", redirect);
        verify(nodeContentService).deleteMaterialContent(21, 11, lecturer);
    }
}
