package com.edunac.mentora.service.learning;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.BankQuestionStatus;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.level.LevelMaterial;
import com.edunac.mentora.domain.level.Material;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.dto.LevelMaterialForm;
import com.edunac.mentora.dto.NodeLevelForm;
import com.edunac.mentora.repository.assessment.BankQuestionRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.repository.level.LevelMaterialRepository;
import com.edunac.mentora.repository.level.MaterialRepository;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NodeLevelService {

    private final NodeLevelRepository levelRepository;
    private final LevelMaterialRepository levelMaterialRepository;
    private final NodeLevelAttemptRepository attemptRepository;
    private final MaterialRepository materialRepository;
    private final LearningNodeRepository nodeRepository;
    private final BankQuestionRepository bankQuestionRepository;

    public NodeLevelService(NodeLevelRepository levelRepository,
                            LevelMaterialRepository levelMaterialRepository,
                            NodeLevelAttemptRepository attemptRepository,
                            MaterialRepository materialRepository,
                            LearningNodeRepository nodeRepository,
                            BankQuestionRepository bankQuestionRepository) {
        this.levelRepository = levelRepository;
        this.levelMaterialRepository = levelMaterialRepository;
        this.attemptRepository = attemptRepository;
        this.materialRepository = materialRepository;
        this.nodeRepository = nodeRepository;
        this.bankQuestionRepository = bankQuestionRepository;
    }

    // ===== READ =====

    @Transactional(readOnly = true)
    public List<NodeLevel> findByNode(Integer nodeId) {
        return levelRepository.findByLearningNode_IdOrderByLevelNumberAsc(nodeId);
    }

    @Transactional(readOnly = true)
    public NodeLevel findById(Integer levelId) {
        return levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy level."));
    }


    @Transactional(readOnly = true)
    public NodeLevel findByIdForOwner(Integer levelId, User requester) {
        NodeLevel level = findById(levelId);
        Integer ownerId = level.getLearningNode().getLearningPath().getCreatedBy().getId();
        if (!ownerId.equals(requester.getId())) {
            throw new IllegalArgumentException("Không tìm thấy level.");
        }
        return level;
    }
    /** Material cùng môn với node — dùng cho dropdown thêm tài liệu. */
    @Transactional(readOnly = true)
    public List<Material> availableMaterials(Integer nodeId) {
        LearningNode node = requireNode(nodeId);
        Integer subjectId = node.getLearningPath().getSubject().getId();
        return materialRepository.findBySubjectIdOrderByCreatedAtDesc(subjectId);
    }

    /** Số câu ACTIVE per material — để cảnh báo thiếu câu (L4 §3). key = materialId. */
    @Transactional(readOnly = true)
    public Map<Integer, Long> bankQuestionCounts(List<Material> materials) {
        Map<Integer, Long> counts = new HashMap<>();
        for (Material m : materials) {
            counts.put(m.getId(),
                    bankQuestionRepository.countByMaterial_IdAndStatus(
                            m.getId(), BankQuestionStatus.ACTIVE.name()));
        }
        return counts;
    }

    // ===== AUTO DEFAULT LEVEL =====

    /**
     * Tạo Level 1 mặc định khi tạo node mới (L3 §7).
     * Không cần ownership check — gọi nội bộ từ LearningPathService.addNode().
     */
    public NodeLevel createDefaultLevel(Integer nodeId) {
        LearningNode node = requireNode(nodeId);
        NodeLevel level = new NodeLevel();
        level.setLearningNode(node);
        level.setLevelNumber(1);
        level.setTitle("Cơ bản");
        level.setMaxScore(BigDecimal.TEN);
        level.setPassingScore(new BigDecimal("5"));
        return levelRepository.save(level);
    }

    // ===== CRUD LEVEL =====

    public NodeLevel createLevel(Integer nodeId, NodeLevelForm form, User requester) {
        LearningNode node = requireNodeForOwner(nodeId, requester);
        validateScoreFields(form.getMaxScore(), form.getPassingScore());
        validateMaxAttempts(form.getMaxAttempts());

        boolean nodeHasAttempt = attemptRepository.existsByNodeLevel_LearningNode_Id(nodeId);
        if (nodeHasAttempt) {
            // §8: level mới chỉ được thêm ở đỉnh max + 1
            int currentMax = levelRepository.findByLearningNode_IdOrderByLevelNumberAsc(nodeId)
                    .stream().mapToInt(NodeLevel::getLevelNumber).max().orElse(0);
            int required = currentMax + 1;
            if (form.getLevelNumber() == null || form.getLevelNumber() != required) {
                throw new IllegalStateException(
                        "Node đã có dữ liệu làm bài — level mới chỉ được thêm vào cuối thang.");
            }
        }

        if (form.getLevelNumber() == null || form.getLevelNumber() < 1) {
            throw new IllegalArgumentException("Số thứ tự level phải ≥ 1.");
        }
        if (levelRepository.existsByLearningNode_IdAndLevelNumber(nodeId, form.getLevelNumber())) {
            throw new IllegalArgumentException("Số thứ tự level đã tồn tại trong node này.");
        }

        NodeLevel level = new NodeLevel();
        level.setLearningNode(node);
        applyForm(level, form);
        return levelRepository.save(level);
    }

    public NodeLevel updateLevel(Integer nodeId, Integer levelId, NodeLevelForm form, User requester) {
        requireNodeForOwner(nodeId, requester);
        NodeLevel level = requireLevelInNode(levelId, nodeId);

        boolean levelHasAttempt = attemptRepository.existsByNodeLevel_Id(levelId);
        boolean nodeHasAttempt = attemptRepository.existsByNodeLevel_LearningNode_Id(nodeId);

        // §9: maxScore/passingScore bất biến khi level đã có attempt
        if (levelHasAttempt) {
            boolean maxScoreChanged = form.getMaxScore() != null
                    && form.getMaxScore().compareTo(level.getMaxScore()) != 0;
            boolean passingScoreChanged = form.getPassingScore() != null
                    && form.getPassingScore().compareTo(level.getPassingScore()) != 0;
            if (maxScoreChanged || passingScoreChanged) {
                throw new IllegalStateException(
                        "Level đã có dữ liệu làm bài — không thể sửa thang điểm.");
            }
        }

        // §8: levelNumber bất biến khi node có attempt
        if (nodeHasAttempt) {
            boolean levelNumberChanged = form.getLevelNumber() != null
                    && !form.getLevelNumber().equals(level.getLevelNumber());
            if (levelNumberChanged) {
                throw new IllegalStateException(
                        "Node đã có dữ liệu làm bài — không thể đánh số lại level.");
            }
        }

        // Đổi levelNumber khi node chưa có attempt → kiểm tra unique
        if (form.getLevelNumber() != null
                && !form.getLevelNumber().equals(level.getLevelNumber())
                && levelRepository.existsByLearningNode_IdAndLevelNumber(nodeId, form.getLevelNumber())) {
            throw new IllegalArgumentException("Số thứ tự level đã tồn tại trong node này.");
        }

        validateScoreFields(form.getMaxScore(), form.getPassingScore());
        validateMaxAttempts(form.getMaxAttempts());
        applyForm(level, form);
        return levelRepository.save(level);
    }

    public void deleteLevel(Integer nodeId, Integer levelId, User requester) {
        requireNodeForOwner(nodeId, requester);
        NodeLevel level = requireLevelInNode(levelId, nodeId);

        if (attemptRepository.existsByNodeLevel_Id(levelId)) {
            throw new IllegalStateException("Level đã có dữ liệu làm bài, không thể xóa.");
        }
        // CascadeType.ALL trên NodeLevel.levelMaterials → LevelMaterial xóa theo
        // §8: không dồn lại levelNumber sau khi xóa — gap (vd 1, 3) chấp nhận được
        levelRepository.delete(level);
    }

    // ===== LEVEL MATERIAL =====

    public LevelMaterial addMaterial(Integer nodeId, Integer levelId,
                                     LevelMaterialForm form, User requester) {
        LearningNode node = requireNodeForOwner(nodeId, requester);
        NodeLevel level = requireLevelInNode(levelId, nodeId);

        if (form.getMaterialId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn tài liệu.");
        }
        if (form.getQuestionCount() == null || form.getQuestionCount() < 1) {
            throw new IllegalArgumentException("Số câu hỏi phải lớn hơn 0.");
        }

        Material material = materialRepository.findById(form.getMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu."));

        Integer nodeSubjectId = node.getLearningPath().getSubject().getId();
        if (!material.getSubject().getId().equals(nodeSubjectId)) {
            throw new IllegalArgumentException(
                    "Material không thuộc môn học của lộ trình này.");
        }

        // Chặn thêm trùng (L4 §2)
        if (levelMaterialRepository.existsByNodeLevel_IdAndMaterial_Id(levelId, form.getMaterialId())) {
            throw new IllegalArgumentException(
                    "Material này đã được thêm vào level. Sửa số câu trên dòng hiện có.");
        }

        LevelMaterial lm = new LevelMaterial();
        lm.setNodeLevel(level);
        lm.setMaterial(material);
        lm.setQuestionCount(form.getQuestionCount());
        return levelMaterialRepository.save(lm);
    }

    public void removeMaterial(Integer nodeId, Integer levelId,
                               Integer levelMaterialId, User requester) {
        requireNodeForOwner(nodeId, requester);
        requireLevelInNode(levelId, nodeId);
        LevelMaterial lm = levelMaterialRepository.findById(levelMaterialId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cấu hình tài liệu."));
        if (!lm.getNodeLevel().getId().equals(levelId)) {
            throw new IllegalArgumentException("Cấu hình tài liệu không thuộc level này.");
        }
        levelMaterialRepository.delete(lm);
    }

    // ===== NODE DELETE SUPPORT (L1 §3, L3 §6) =====

    /** L1 §3 — chặn xóa node khi bất kỳ level nào của nó đã có attempt. */
    @Transactional(readOnly = true)
    public boolean nodeHasAttempts(Integer nodeId) {
        return attemptRepository.existsByNodeLevel_LearningNode_Id(nodeId);
    }

    /**
     * L3 §6 — cascade xóa toàn bộ NodeLevel (+ LevelMaterial theo orphanRemoval)
     * của một node. Gọi từ LearningPathService.deleteNode() SAU khi đã xác nhận
     * node không có attempt nào (nodeHasAttempts()).
     */
    public void deleteLevelsForNode(Integer nodeId) {
        levelRepository.deleteAll(levelRepository.findByLearningNode_IdOrderByLevelNumberAsc(nodeId));
    }

    // ===== CLONE (L1 §4) =====

    /**
     * Sao chép toàn bộ NodeLevel + LevelMaterial từ node nguồn sang node đích.
     * KHÔNG sao chép NodeLevelAttempt.
     * Gọi từ LearningPathService.clonePath().
     */
    public void cloneLevels(Integer sourceNodeId, Integer targetNodeId) {
        LearningNode targetNode = requireNode(targetNodeId);
        List<NodeLevel> sourceLevels =
                levelRepository.findByLearningNode_IdOrderByLevelNumberAsc(sourceNodeId);
        for (NodeLevel src : sourceLevels) {
            NodeLevel copy = new NodeLevel();
            copy.setLearningNode(targetNode);
            copy.setLevelNumber(src.getLevelNumber());
            copy.setTitle(src.getTitle());
            copy.setMaxScore(src.getMaxScore());
            copy.setPassingScore(src.getPassingScore());
            copy.setMaxAttempts(src.getMaxAttempts());
            copy.setDurationMinutes(src.getDurationMinutes());
            NodeLevel savedCopy = levelRepository.save(copy);

            for (LevelMaterial srcLm : levelMaterialRepository.findByNodeLevel_Id(src.getId())) {
                LevelMaterial lmCopy = new LevelMaterial();
                lmCopy.setNodeLevel(savedCopy);
                lmCopy.setMaterial(srcLm.getMaterial());
                lmCopy.setQuestionCount(srcLm.getQuestionCount());
                levelMaterialRepository.save(lmCopy);
            }
        }
    }

    // ===== HELPERS =====

    public NodeLevelForm toForm(NodeLevel level) {
        NodeLevelForm form = new NodeLevelForm();
        form.setId(level.getId());
        form.setNodeId(level.getLearningNode().getId());
        form.setLevelNumber(level.getLevelNumber());
        form.setTitle(level.getTitle());
        form.setMaxScore(level.getMaxScore());
        form.setPassingScore(level.getPassingScore());
        form.setMaxAttempts(level.getMaxAttempts());
        form.setDurationMinutes(level.getDurationMinutes());
        return form;
    }

    private void applyForm(NodeLevel level, NodeLevelForm form) {
        if (form.getLevelNumber() != null) level.setLevelNumber(form.getLevelNumber());
        level.setTitle(form.getTitle() == null || form.getTitle().isBlank()
                ? null : form.getTitle().trim());
        level.setMaxScore(form.getMaxScore());
        level.setPassingScore(form.getPassingScore());
        level.setMaxAttempts(form.getMaxAttempts());
        level.setDurationMinutes(form.getDurationMinutes());
    }

    private void validateScoreFields(BigDecimal maxScore, BigDecimal passingScore) {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Điểm tối đa phải lớn hơn 0.");
        }
        if (passingScore == null
                || passingScore.compareTo(BigDecimal.ZERO) <= 0
                || passingScore.compareTo(maxScore) > 0) {
            throw new IllegalArgumentException(
                    "Ngưỡng qua level phải nằm trong phổ điểm của level đó.");
        }
    }

    private void validateMaxAttempts(Integer maxAttempts) {
        if (maxAttempts != null && maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Số lần thử phải ≥ 1 hoặc để trống (không giới hạn).");
        }
    }

    private LearningNode requireNode(Integer nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy node."));
    }

    private LearningNode requireNodeForOwner(Integer nodeId, User requester) {
        LearningNode node = requireNode(nodeId);
        if (!node.getLearningPath().getCreatedBy().getId().equals(requester.getId())) {
            throw new IllegalStateException("Bạn không có quyền chỉnh sửa node này.");
        }
        return node;
    }

    private NodeLevel requireLevelInNode(Integer levelId, Integer nodeId) {
        NodeLevel level = findById(levelId);
        if (!level.getLearningNode().getId().equals(nodeId)) {
            throw new IllegalArgumentException("Level không thuộc node này.");
        }
        return level;
    }
}