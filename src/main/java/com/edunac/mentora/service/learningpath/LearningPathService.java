    package com.edunac.mentora.service.learningpath;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.dto.LearningNodeForm;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.learning.NodeContentRepository;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.repository.learningpath.LearningPathRepository;
import com.edunac.mentora.repository.subject.SubjectRepository;
import com.edunac.mentora.service.learning.NodeContentStorageService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class LearningPathService {

    private static final BigDecimal NORMALIZE_THRESHOLD = new BigDecimal("0.001");

    private final LearningPathRepository pathRepository;
    private final LearningNodeRepository nodeRepository;
    private final SubjectRepository subjectRepository;
    private final NodeContentRepository nodeContentRepository;
    private final NodeContentStorageService storageService;
    private final NodeProgressRepository nodeProgressRepository;
    private final ClassroomNodeStatusRepository classroomNodeStatusRepository;

    public LearningPathService(LearningPathRepository pathRepository,
                                LearningNodeRepository nodeRepository,
                                SubjectRepository subjectRepository,
                                NodeContentRepository nodeContentRepository,
                                NodeContentStorageService storageService,
                                NodeProgressRepository nodeProgressRepository,
                                ClassroomNodeStatusRepository classroomNodeStatusRepository) {
        this.pathRepository = pathRepository;
        this.nodeRepository = nodeRepository;
        this.subjectRepository = subjectRepository;
        this.nodeContentRepository = nodeContentRepository;
        this.storageService = storageService;
        this.nodeProgressRepository = nodeProgressRepository;
        this.classroomNodeStatusRepository = classroomNodeStatusRepository;
    }

    // ===== LEARNING PATH =====

    public List<LearningPath> findByCreator(User creator) {
        return pathRepository.findByCreatedByIdOrderByCreatedAtDesc(creator.getId());
    }

    public LearningPath findById(Integer id) {
        return pathRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình."));
    }

    public LearningPath create(Integer subjectId, String name, String description, User creator) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học."));
        if (!"ACTIVE".equals(subject.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể tạo lộ trình cho môn học đang ACTIVE.");
        }
        validatePathName(name);

        LearningPath path = new LearningPath();
        path.setSubject(subject);
        path.setName(name.trim());
        path.setDescription(blankToNull(description));
        path.setCreatedBy(creator);
        return pathRepository.save(path);
    }

    public LearningPath update(Integer id, String name, String description, User requester) {
        LearningPath path = findByIdAndOwner(id, requester);
        validatePathName(name);
        path.setName(name.trim());
        path.setDescription(blankToNull(description));
        return pathRepository.save(path);
    }

    public void delete(Integer id, User requester) {
        LearningPath path = findByIdAndOwner(id, requester);

        List<LearningNode> nodes = nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(id);
        for (LearningNode n : nodes) {
            n.setPrerequisite(null);
        }
        nodeRepository.saveAll(nodes);
        for (LearningNode n : nodes) {
            deleteContentsForNode(n.getId());
        }
        nodeRepository.deleteAll(nodes);

        try {
            pathRepository.delete(path);
            pathRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Không thể xóa lộ trình đang được sử dụng bởi lớp học.");
        }
    }

    // ===== LEARNING NODE =====

    public List<LearningNode> getNodes(Integer pathId) {
        return nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId);
    }

    public LearningNode addNode(Integer pathId, LearningNodeForm form, User requester) {
        LearningPath path = findByIdAndOwner(pathId, requester);
        validateNodeTitle(form.getTitle());

        List<LearningNode> nodes = nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId);
        BigDecimal order = computeOrder(nodes, form.getAfterNodeId());

        LearningNode node = new LearningNode();
        node.setLearningPath(path);
        node.setTitle(form.getTitle().trim());
        node.setDescription(blankToNull(form.getDescription()));
        node.setNodeOrder(order);
        setPrerequisite(node, form.getPrerequisiteNodeId(), null, pathId);

        LearningNode saved = nodeRepository.save(node);
        normalizeIfNeeded(pathId);
        return saved;
    }

    public LearningNode updateNode(Integer pathId, Integer nodeId, LearningNodeForm form, User requester) {
        findByIdAndOwner(pathId, requester);
        LearningNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy node."));
        if (!node.getLearningPath().getId().equals(pathId)) {
            throw new IllegalArgumentException("Node không thuộc lộ trình này.");
        }
        validateNodeTitle(form.getTitle());

        node.setTitle(form.getTitle().trim());
        node.setDescription(blankToNull(form.getDescription()));
        setPrerequisite(node, form.getPrerequisiteNodeId(), nodeId, pathId);
        return nodeRepository.save(node);
    }

    public void deleteNode(Integer pathId, Integer nodeId, User requester) {
        findByIdAndOwner(pathId, requester);
        LearningNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy node."));
        if (!node.getLearningPath().getId().equals(pathId)) {
            throw new IllegalArgumentException("Node không thuộc lộ trình này.");
        }

        List<LearningNode> dependents = new java.util.ArrayList<>();
        for (LearningNode n : nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId)) {
            if (n.getPrerequisite() != null && n.getPrerequisite().getId().equals(nodeId)) {
                dependents.add(n);
            }
        }
        for (LearningNode n : dependents) {
            n.setPrerequisite(null);
        }
        nodeRepository.saveAll(dependents);

        if (nodeProgressRepository.existsByLearningNodeId(nodeId)) {
            throw new IllegalStateException("Không thể xóa node vì vẫn còn dữ liệu tiến trình học liên quan.");
        }
        classroomNodeStatusRepository.deleteByNodeId(nodeId);
        deleteContentsForNode(nodeId);
        nodeRepository.delete(node);
    }

    // ===== PRIVATE HELPERS =====

    private void deleteContentsForNode(Integer nodeId) {
        List<NodeContent> contents = nodeContentRepository.findByNode_IdOrderByDisplayOrderAscIdAsc(nodeId);
        for (NodeContent c : contents) {
            storageService.deleteIfManaged(c.getContentUrl());
        }
        nodeContentRepository.deleteAll(contents);
    }

    public LearningPath findByIdAndOwner(Integer id, User requester) {
        LearningPath path = pathRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình."));
        if (!path.getCreatedBy().getId().equals(requester.getId())) {
            throw new IllegalStateException("Bạn không có quyền chỉnh sửa lộ trình này.");
        }
        return path;
    }

    private void validatePathName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên lộ trình không được để trống.");
        }
    }

    private void validateNodeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tiêu đề node không được để trống.");
        }
    }

    private void setPrerequisite(LearningNode node, Integer prereqId, Integer excludeId, Integer pathId) {
        if (prereqId == null) {
            node.setPrerequisite(null);
            return;
        }
        if (prereqId.equals(excludeId)) {
            throw new IllegalArgumentException("Node không thể là tiên quyết của chính nó.");
        }
        LearningNode prereq = nodeRepository.findById(prereqId)
                .orElseThrow(() -> new IllegalArgumentException("Node tiên quyết không tồn tại."));
        if (!prereq.getLearningPath().getId().equals(pathId)) {
            throw new IllegalArgumentException("Node tiên quyết phải thuộc cùng lộ trình.");
        }
        node.setPrerequisite(prereq);
    }

    /**
     * Tính node_order cho node mới.
     * afterNodeId=null → append cuối; afterNodeId=X → chèn sau node X.
     * Nếu chèn giữa: order = (prev + next) / 2.
     */
    private BigDecimal computeOrder(List<LearningNode> nodes, Integer afterNodeId) {
        if (nodes.isEmpty()) return BigDecimal.ONE;

        if (afterNodeId == null) {
            return nodes.get(nodes.size() - 1).getNodeOrder().add(BigDecimal.ONE);
        }

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(afterNodeId)) {
                if (i == nodes.size() - 1) {
                    return nodes.get(i).getNodeOrder().add(BigDecimal.ONE);
                }
                BigDecimal prev = nodes.get(i).getNodeOrder();
                BigDecimal next = nodes.get(i + 1).getNodeOrder();
                return prev.add(next).divide(BigDecimal.valueOf(2), 9, RoundingMode.HALF_UP);
            }
        }
        throw new IllegalArgumentException("Vị trí chèn không hợp lệ.");
    }

    /**
     * Nếu khoảng cách nhỏ nhất giữa các node < 0.001 → gán lại 1.0, 2.0, 3.0, ...
     */
    private void normalizeIfNeeded(Integer pathId) {
        List<LearningNode> nodes = nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId);
        if (nodes.size() < 2) return;

        BigDecimal minGap = null;
        for (int i = 1; i < nodes.size(); i++) {
            BigDecimal gap = nodes.get(i).getNodeOrder().subtract(nodes.get(i - 1).getNodeOrder()).abs();
            if (minGap == null || gap.compareTo(minGap) < 0) minGap = gap;
        }

        if (minGap != null && minGap.compareTo(NORMALIZE_THRESHOLD) < 0) {
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).setNodeOrder(BigDecimal.valueOf(i + 1));
            }
            nodeRepository.saveAll(nodes);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
