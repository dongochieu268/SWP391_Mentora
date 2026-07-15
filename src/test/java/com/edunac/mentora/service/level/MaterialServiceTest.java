package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.BankQuestion;
import com.edunac.mentora.domain.assessment.QuestionBank;
import com.edunac.mentora.domain.level.Material;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.domain.subject.SubjectStatus;
import com.edunac.mentora.repository.assessment.BankQuestionRepository;
import com.edunac.mentora.repository.learning.NodeContentRepository;
import com.edunac.mentora.repository.level.LevelMaterialRepository;
import com.edunac.mentora.repository.level.MaterialRepository;
import com.edunac.mentora.repository.subject.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialServiceTest {

    private MaterialRepository materialRepository;
    private SubjectRepository subjectRepository;
    private LevelMaterialRepository levelMaterialRepository;
    private BankQuestionRepository bankQuestionRepository;
    private NodeContentRepository nodeContentRepository;
    private MaterialService service;
    private User lecturer;
    private Subject activeSubject;

    @BeforeEach
    void setUp() {
        materialRepository = mock(MaterialRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        levelMaterialRepository = mock(LevelMaterialRepository.class);
        bankQuestionRepository = mock(BankQuestionRepository.class);
        nodeContentRepository = mock(NodeContentRepository.class);
        service = new MaterialService(
                materialRepository,
                subjectRepository,
                levelMaterialRepository,
                bankQuestionRepository,
                nodeContentRepository
        );

        lecturer = new User();
        lecturer.setId(7);
        lecturer.setFullName("Kieu Tuan Minh");

        activeSubject = new Subject();
        activeSubject.setId(3);
        activeSubject.setCode("SWP391");
        activeSubject.setName("Software Project");
        activeSubject.setStatus(SubjectStatus.ACTIVE.name());
    }

    @Test
    void createStoresTrimmedMaterialForActiveSubjectAndCreator() {
        when(subjectRepository.findById(activeSubject.getId())).thenReturn(Optional.of(activeSubject));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Material saved = service.create(activeSubject.getId(), "  OOP Basics  ", "  Encapsulation  ", lecturer);

        assertSame(activeSubject, saved.getSubject());
        assertSame(lecturer, saved.getCreatedBy());
        assertEquals("OOP Basics", saved.getTitle());
        assertEquals("Encapsulation", saved.getDescription());
    }

    @Test
    void findOwnedBySubjectFiltersBySubjectAndLecturer() {
        Material material = materialOwnedBy(11, activeSubject, lecturer);
        when(materialRepository.findBySubjectIdAndCreatedBy_IdOrderByCreatedAtDesc(
                activeSubject.getId(), lecturer.getId())).thenReturn(List.of(material));

        List<Material> materials = service.findOwnedBySubject(activeSubject.getId(), lecturer);

        assertEquals(List.of(material), materials);
        verify(materialRepository).findBySubjectIdAndCreatedBy_IdOrderByCreatedAtDesc(
                activeSubject.getId(), lecturer.getId());
    }

    @Test
    void createRejectsInactiveSubject() {
        activeSubject.setStatus(SubjectStatus.HIDDEN.name());
        when(subjectRepository.findById(activeSubject.getId())).thenReturn(Optional.of(activeSubject));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(activeSubject.getId(), "OOP Basics", null, lecturer));

        assertEquals("Môn học không còn hoạt động.", ex.getMessage());
        verify(materialRepository, never()).save(any(Material.class));
    }

    @Test
    void updateRejectsRequesterWhoDoesNotOwnMaterial() {
        Material existing = materialOwnedBy(11, activeSubject, user(99));
        when(materialRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.update(existing.getId(), "New title", "New description", lecturer));

        assertEquals("Bạn không có quyền chỉnh sửa material này.", ex.getMessage());
        verify(materialRepository, never()).save(any(Material.class));
    }

    @Test
    void deleteRejectsMaterialUsedByLevelConfiguration() {
        Material existing = materialOwnedBy(11, activeSubject, lecturer);
        when(materialRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(levelMaterialRepository.existsByMaterial_Id(existing.getId())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.delete(existing.getId(), lecturer));

        assertEquals("Material đang được sử dụng trong cấu hình level, không thể xóa.", ex.getMessage());
        verify(materialRepository, never()).delete(any(Material.class));
    }

    @Test
    void deleteRejectsMaterialWithOwnedContent() {
        Material existing = materialOwnedBy(11, activeSubject, lecturer);
        when(materialRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(nodeContentRepository.existsByMaterial_Id(existing.getId())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.delete(existing.getId(), lecturer));

        assertEquals("Material đang có nội dung, không thể xóa.", ex.getMessage());
        verify(materialRepository, never()).delete(any(Material.class));
    }

    @Test
    void assignQuestionsTagsQuestionsFromSameSubject() {
        Material material = materialOwnedBy(11, activeSubject, lecturer);
        BankQuestion first = questionWithSubject(21, activeSubject);
        BankQuestion second = questionWithSubject(22, activeSubject);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(bankQuestionRepository.findByIdIn(List.of(first.getId(), second.getId())))
                .thenReturn(List.of(first, second));
        when(bankQuestionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int updated = service.assignQuestions(material.getId(), List.of(first.getId(), second.getId()), lecturer);

        assertEquals(2, updated);
        assertSame(material, first.getMaterial());
        assertSame(material, second.getMaterial());
        verify(bankQuestionRepository).saveAll(List.of(first, second));
    }

    @Test
    void assignQuestionsRejectsQuestionFromDifferentSubject() {
        Subject otherSubject = new Subject();
        otherSubject.setId(99);
        otherSubject.setCode("PRJ301");
        otherSubject.setName("Java Web");
        otherSubject.setStatus(SubjectStatus.ACTIVE.name());
        Material material = materialOwnedBy(11, activeSubject, lecturer);
        BankQuestion question = questionWithSubject(21, otherSubject);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(bankQuestionRepository.findByIdIn(List.of(question.getId()))).thenReturn(List.of(question));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.assignQuestions(material.getId(), List.of(question.getId()), lecturer));

        assertEquals("Câu hỏi không thuộc môn học của material này.", ex.getMessage());
        verify(bankQuestionRepository, never()).saveAll(anyList());
    }

    @Test
    void unassignQuestionsOnlyClearsQuestionsOwnedByMaterial() {
        Material material = materialOwnedBy(11, activeSubject, lecturer);
        Material otherMaterial = materialOwnedBy(12, activeSubject, lecturer);
        BankQuestion linked = questionWithSubject(21, activeSubject);
        BankQuestion linkedElsewhere = questionWithSubject(22, activeSubject);
        linked.setMaterial(material);
        linkedElsewhere.setMaterial(otherMaterial);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(bankQuestionRepository.findByIdIn(List.of(linked.getId(), linkedElsewhere.getId())))
                .thenReturn(List.of(linked, linkedElsewhere));
        when(bankQuestionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int updated = service.unassignQuestions(material.getId(), List.of(linked.getId(), linkedElsewhere.getId()), lecturer);

        assertEquals(1, updated);
        assertEquals(null, linked.getMaterial());
        assertSame(otherMaterial, linkedElsewhere.getMaterial());
        verify(bankQuestionRepository).saveAll(List.of(linked));
    }

    @Test
    void findQuestionsByMaterialRequiresOwnershipAndReturnsTaggedQuestions() {
        Material material = materialOwnedBy(11, activeSubject, lecturer);
        BankQuestion question = questionWithSubject(21, activeSubject);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(bankQuestionRepository.findByMaterial_IdOrderByUpdatedAtDesc(material.getId()))
                .thenReturn(List.of(question));

        List<BankQuestion> questions = service.findQuestionsByMaterial(material.getId(), lecturer);

        assertEquals(List.of(question), questions);
    }

    private Material materialOwnedBy(Integer id, Subject subject, User owner) {
        Material material = new Material();
        material.setId(id);
        material.setSubject(subject);
        material.setCreatedBy(owner);
        material.setTitle("OOP Basics");
        return material;
    }

    private User user(Integer id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private BankQuestion questionWithSubject(Integer id, Subject subject) {
        QuestionBank bank = new QuestionBank();
        bank.setSubject(subject);

        BankQuestion question = new BankQuestion();
        question.setId(id);
        question.setQuestionBank(bank);
        return question;
    }
}
