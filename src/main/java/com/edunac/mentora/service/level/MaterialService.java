package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.BankQuestion;
import com.edunac.mentora.domain.level.Material;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.repository.assessment.BankQuestionRepository;
import com.edunac.mentora.repository.learning.NodeContentRepository;
import com.edunac.mentora.repository.level.LevelMaterialRepository;
import com.edunac.mentora.repository.level.MaterialRepository;
import com.edunac.mentora.repository.subject.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final SubjectRepository subjectRepository;
    private final LevelMaterialRepository levelMaterialRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final NodeContentRepository nodeContentRepository;

    public MaterialService(MaterialRepository materialRepository,
                           SubjectRepository subjectRepository,
                           LevelMaterialRepository levelMaterialRepository,
                           BankQuestionRepository bankQuestionRepository,
                           NodeContentRepository nodeContentRepository) {
        this.materialRepository = materialRepository;
        this.subjectRepository = subjectRepository;
        this.levelMaterialRepository = levelMaterialRepository;
        this.bankQuestionRepository = bankQuestionRepository;
        this.nodeContentRepository = nodeContentRepository;
    }

    @Transactional(readOnly = true)
    public List<Material> findBySubject(Integer subjectId) {
        if (subjectId == null) {
            return List.of();
        }
        return materialRepository.findBySubjectIdOrderByCreatedAtDesc(subjectId);
    }

    @Transactional(readOnly = true)
    public List<Material> findOwnedBySubject(Integer subjectId, User creator) {
        requireRequester(creator);
        if (subjectId == null) {
            return List.of();
        }
        return materialRepository.findBySubjectIdAndCreatedBy_IdOrderByCreatedAtDesc(
                subjectId, creator.getId());
    }

    @Transactional(readOnly = true)
    public List<Material> findByCreator(User creator) {
        requireRequester(creator);
        return materialRepository.findByCreatedBy_IdOrderByCreatedAtDesc(creator.getId());
    }

    @Transactional(readOnly = true)
    public Material requireOwned(Integer materialId, User requester) {
        Material material = requireMaterial(materialId);
        requireOwner(material, requester);
        return material;
    }

    @Transactional(readOnly = true)
    public List<BankQuestion> findQuestionsByMaterial(Integer materialId, User requester) {
        Material material = requireOwned(materialId, requester);
        return bankQuestionRepository.findByMaterial_IdOrderByUpdatedAtDesc(material.getId());
    }

    public Material create(Integer subjectId, String title, String description, User creator) {
        requireRequester(creator);
        Subject subject = requireActiveSubject(subjectId);

        Material material = new Material();
        material.setSubject(subject);
        material.setCreatedBy(creator);
        material.setTitle(normalizeTitle(title));
        material.setDescription(blankToNull(description));
        return materialRepository.save(material);
    }

    public Material update(Integer materialId, String title, String description, User requester) {
        Material material = requireOwned(materialId, requester);
        material.setTitle(normalizeTitle(title));
        material.setDescription(blankToNull(description));
        return materialRepository.save(material);
    }

    public void delete(Integer materialId, User requester) {
        Material material = requireOwned(materialId, requester);
        if (levelMaterialRepository.existsByMaterial_Id(material.getId())) {
            throw new IllegalStateException("Material đang được sử dụng trong cấu hình level, không thể xóa.");
        }
        if (nodeContentRepository.existsByMaterial_Id(material.getId())) {
            throw new IllegalStateException("Material đang có nội dung, không thể xóa.");
        }
        materialRepository.delete(material);
    }

    public int assignQuestions(Integer materialId, Collection<Integer> questionIds, User requester) {
        Material material = requireOwned(materialId, requester);
        List<BankQuestion> questions = requireQuestions(questionIds);
        for (BankQuestion question : questions) {
            if (!sameSubject(question, material)) {
                throw new IllegalArgumentException("Câu hỏi không thuộc môn học của material này.");
            }
            question.setMaterial(material);
        }
        bankQuestionRepository.saveAll(questions);
        return questions.size();
    }

    public int unassignQuestions(Integer materialId, Collection<Integer> questionIds, User requester) {
        Material material = requireOwned(materialId, requester);
        List<BankQuestion> changed = new ArrayList<>();
        for (BankQuestion question : requireQuestions(questionIds)) {
            Material currentMaterial = question.getMaterial();
            if (currentMaterial != null && material.getId().equals(currentMaterial.getId())) {
                question.setMaterial(null);
                changed.add(question);
            }
        }
        bankQuestionRepository.saveAll(changed);
        return changed.size();
    }

    private Material requireMaterial(Integer materialId) {
        if (materialId == null) {
            throw new IllegalArgumentException("Material không hợp lệ.");
        }
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy material."));
    }

    private Subject requireActiveSubject(Integer subjectId) {
        if (subjectId == null) {
            throw new IllegalArgumentException("Vui lòng chọn môn học.");
        }
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học."));
        if (!subject.isActive()) {
            throw new IllegalArgumentException("Môn học không còn hoạt động.");
        }
        return subject;
    }

    private List<BankQuestion> requireQuestions(Collection<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một câu hỏi.");
        }
        List<Integer> uniqueIds = new ArrayList<>(new LinkedHashSet<>(questionIds));
        List<BankQuestion> questions = bankQuestionRepository.findByIdIn(uniqueIds);
        if (questions.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("Có câu hỏi không tồn tại.");
        }
        return questions;
    }

    private boolean sameSubject(BankQuestion question, Material material) {
        if (question.getQuestionBank() == null || question.getQuestionBank().getSubject() == null) {
            return false;
        }
        Integer questionSubjectId = question.getQuestionBank().getSubject().getId();
        Integer materialSubjectId = material.getSubject() != null ? material.getSubject().getId() : null;
        return materialSubjectId != null && materialSubjectId.equals(questionSubjectId);
    }

    private void requireOwner(Material material, User requester) {
        requireRequester(requester);
        if (material.getCreatedBy() == null || !requester.getId().equals(material.getCreatedBy().getId())) {
            throw new IllegalStateException("Bạn không có quyền chỉnh sửa material này.");
        }
    }

    private void requireRequester(User requester) {
        if (requester == null || requester.getId() == null) {
            throw new IllegalStateException("Vui lòng đăng nhập.");
        }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tên material không được để trống.");
        }
        String normalized = title.trim();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Tên material tối đa 255 ký tự.");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
