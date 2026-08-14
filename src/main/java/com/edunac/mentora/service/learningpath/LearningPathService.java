package com.edunac.mentora.service.learningpath;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.branching.BranchRule;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import com.edunac.mentora.domain.classroom.NodeVisibilityStatus;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.dto.LearningNodeForm;
import com.edunac.mentora.domain.learningpath.LearningPathStatus;
import com.edunac.mentora.repository.branching.BranchRuleRepository;
import com.edunac.mentora.repository.assessment.AssessmentRepository;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import com.edunac.mentora.repository.learning.NodeContentRepository;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.repository.learningpath.LearningPathRepository;
import com.edunac.mentora.repository.subject.SubjectRepository;
import com.edunac.mentora.service.learning.NodeContentStorageService;
import com.edunac.mentora.service.learning.NodeLevelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ClassroomRepository classroomRepository;
    private final BranchRuleRepository branchRuleRepository;
    private final AssessmentRepository assessmentRepository;

    private final NodeLevelService nodeLevelService;

    public LearningPathService(LearningPathRepository pathRepository,
                               LearningNodeRepository nodeRepository,
                               SubjectRepository subjectRepository,
                               NodeContentRepository nodeContentRepository,
                               NodeContentStorageService storageService,
                               NodeProgressRepository nodeProgressRepository,
                               ClassroomNodeStatusRepository classroomNodeStatusRepository,
                               ClassroomRepository classroomRepository,
                               BranchRuleRepository branchRuleRepository,
                               AssessmentRepository assessmentRepository,
                               NodeLevelService nodeLevelService) {
        this.pathRepository = pathRepository;
        this.nodeRepository = nodeRepository;
        this.subjectRepository = subjectRepository;
        this.nodeContentRepository = nodeContentRepository;
        this.storageService = storageService;
        this.nodeProgressRepository = nodeProgressRepository;
        this.classroomNodeStatusRepository = classroomNodeStatusRepository;
        this.classroomRepository = classroomRepository;
        this.branchRuleRepository = branchRuleRepository;
        this.assessmentRepository = assessmentRepository;
        this.nodeLevelService = nodeLevelService;
    }


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
        validatePathDescription(description);

        LearningPath path = new LearningPath();
        path.setSubject(subject);
        path.setName(name.trim());
        path.setDescription(blankToNull(description));
        path.setCreatedBy(creator);
        path.setStatus(LearningPathStatus.ACTIVE.name());
        return pathRepository.save(path);
    }

    public LearningPath update(Integer id, String name, String description, User requester) {
        LearningPath path = findByIdAndOwner(id, requester);
        validatePathName(name);
        validatePathDescription(description);
        path.setName(name.trim());
        path.setDescription(blankToNull(description));
        return pathRepository.save(path);
    }

    public void delete(Integer id, User requester) {
        LearningPath path = findByIdAndOwner(id, requester);
        if (classroomRepository.existsByLearningPathId(id)) {
            throw new IllegalStateException("Không thể xóa lộ trình đang được lớp học sử dụng.");
        }

        List<LearningNode> nodes = nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(id);


        for (LearningNode n : nodes) {
            if (nodeProgressRepository.existsByLearningNodeId(n.getId())) {
                throw new IllegalStateException(
                        "Không thể xóa lộ trình vì vẫn còn dữ liệu tiến trình học liên quan.");
            }
            if (nodeLevelService.nodeHasAttempts(n.getId())) {
                throw new IllegalStateException(
                        "Không thể xóa lộ trình vì một số node đang có dữ liệu làm bài của học sinh.");
            }
        }

        for (LearningNode n : nodes) {
            n.setPrerequisite(null);
            n.setBranchOwnerNode(null);
        }
        nodeRepository.saveAll(nodes);
        for (LearningNode n : nodes) {
            branchRuleRepository.findByNodeId(n.getId()).ifPresent(branchRuleRepository::delete);
            classroomNodeStatusRepository.deleteByNodeId(n.getId());
            deleteContentsForNode(n.getId());
            nodeLevelService.deleteLevelsForNode(n.getId());
        }
        nodeRepository.deleteAll(nodes);
        pathRepository.delete(path);
    }


    public List<LearningNode> getNodes(Integer pathId) {
        return nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId);
    }

    public LearningNode addNode(Integer pathId, LearningNodeForm form, User requester) {
        LearningPath path = findByIdAndOwner(pathId, requester);
        validateNodeTitle(form.getTitle());
        validateNodeDescription(form.getDescription());

        List<LearningNode> nodes = nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId);
        BigDecimal order = computeOrder(nodes);

        LearningNode node = new LearningNode();
        node.setLearningPath(path);
        node.setTitle(form.getTitle().trim());
        node.setDescription(blankToNull(form.getDescription()));
        node.setNodeOrder(order);
        setPrerequisite(node, form.getPrerequisiteNodeId(), null, pathId);

        applyBranchingFields(node, form, pathId);

        LearningNode saved = nodeRepository.save(node);

        saveBranchRuleIfNeeded(saved, form);

        // L3 §7: tự sinh Level 1 mặc định khi tạo node mới
        nodeLevelService.createDefaultLevel(saved.getId());

        // A newly-created lesson is published to every classroom currently
        // using this path. Explicitly hidden existing lessons are untouched.
        createVisibleStatusForPathClassrooms(pathId, saved);

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
        validateNodeDescription(form.getDescription());

        node.setTitle(form.getTitle().trim());
        node.setDescription(blankToNull(form.getDescription()));
        setPrerequisite(node, form.getPrerequisiteNodeId(), nodeId, pathId);

        applyBranchingFields(node, form, pathId);

        LearningNode saved = nodeRepository.save(node);

        if ("BRANCH_TEST".equals(form.getNodeType())) {
            saveBranchRuleIfNeeded(saved, form);
        } else {
            branchRuleRepository.findByNodeId(nodeId).ifPresent(branchRuleRepository::delete);
        }

        return saved;
    }


    private void applyBranchingFields(LearningNode node, LearningNodeForm form, Integer pathId) {
        String nodeType = form.getNodeType() != null ? form.getNodeType() : "LESSON";
        node.setNodeType(nodeType);

        if ("BRANCH_TEST".equals(nodeType)) {
            node.setBranchTag("MAIN");
            node.setBranchOwnerNode(null);
        } else {
            String tag = form.getBranchTag() != null ? form.getBranchTag() : "MAIN";
            node.setBranchTag(tag);

            if (("PASS".equals(tag) || "FAIL".equals(tag)) && form.getBranchOwnerNodeId() != null) {
                LearningNode owner = nodeRepository.findById(form.getBranchOwnerNodeId())
                        .orElseThrow(() -> new IllegalArgumentException("Node gốc không tồn tại."));
                if (!owner.getLearningPath().getId().equals(pathId)) {
                    throw new IllegalArgumentException("Node gốc phải thuộc cùng lộ trình.");
                }
                if (!"BRANCH_TEST".equals(owner.getNodeType())) {
                    throw new IllegalArgumentException("Node gốc phải là loại BRANCH_TEST.");
                }
                node.setBranchOwnerNode(owner);
            } else {
                node.setBranchOwnerNode(null);
            }
        }
    }


    private void saveBranchRuleIfNeeded(LearningNode node, LearningNodeForm form) {
        if (!"BRANCH_TEST".equals(node.getNodeType())) return;
        if (form.getAssessmentId() == null) return;

        int minScore = (form.getMinScore() != null) ? form.getMinScore().intValue() : 0;

        BranchRule rule = branchRuleRepository.findByNodeId(node.getId())
                .orElseGet(BranchRule::new);
        rule.setNode(node);
        rule.setAssessmentId(form.getAssessmentId());
        rule.setMinScore(minScore);
        branchRuleRepository.save(rule);
    }

    public void deleteNode(Integer pathId, Integer nodeId, User requester) {
        findByIdAndOwner(pathId, requester);
        ensureStructureEditable(pathId);
        LearningNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy node."));
        if (!node.getLearningPath().getId().equals(pathId)) {
            throw new IllegalArgumentException("Node không thuộc lộ trình này.");
        }

        List<LearningNode> prereqDependents = new java.util.ArrayList<>();
        List<LearningNode> ownerDependents = new java.util.ArrayList<>();
        for (LearningNode n : nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId)) {
            if (n.getPrerequisite() != null && n.getPrerequisite().getId().equals(nodeId)) {
                prereqDependents.add(n);
            }
            if (n.getBranchOwnerNode() != null && n.getBranchOwnerNode().getId().equals(nodeId)) {
                ownerDependents.add(n);
            }
        }
        for (LearningNode n : prereqDependents) n.setPrerequisite(null);
        for (LearningNode n : ownerDependents) {
            n.setBranchOwnerNode(null);
            n.setBranchTag("MAIN");
        }
        nodeRepository.saveAll(prereqDependents);
        nodeRepository.saveAll(ownerDependents);

        if (nodeProgressRepository.existsByLearningNodeId(nodeId)) {
            throw new IllegalStateException("Không thể xóa node vì vẫn còn dữ liệu tiến trình học liên quan.");
        }
        // nodeLevelService.nodeHasAttempts(nodeId) không cần kiểm tra ở đây: attempt chỉ
        // phát sinh qua lớp học, mà ensureStructureEditable() phía trên đã chặn xóa node
        // ngay khi lộ trình đang được bất kỳ lớp học nào sử dụng.

        branchRuleRepository.findByNodeId(nodeId).ifPresent(branchRuleRepository::delete);
        classroomNodeStatusRepository.deleteByNodeId(nodeId);
        deleteContentsForNode(nodeId);
        // L3 §6 — cascade xóa NodeLevel + LevelMaterial trước khi xóa node (FK).
        nodeLevelService.deleteLevelsForNode(nodeId);
        nodeRepository.delete(node);
    }

    /** Di chuyển một node lên/xuống một bậc — đổi chỗ node_order với node liền kề. */
    public void moveNode(Integer pathId, Integer nodeId, String direction, User requester) {
        findByIdAndOwner(pathId, requester);
        if (!"up".equals(direction) && !"down".equals(direction)) {
            throw new IllegalArgumentException("Hướng di chuyển không hợp lệ.");
        }

        List<LearningNode> nodes = new java.util.ArrayList<>(
                nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId));
        int idx = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(nodeId)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            throw new IllegalArgumentException("Không tìm thấy node.");
        }

        int target = "up".equals(direction) ? idx - 1 : idx + 1;
        if (target < 0 || target >= nodes.size()) {
            throw new IllegalArgumentException("Node đã ở vị trí biên, không thể di chuyển thêm.");
        }

        java.util.Collections.swap(nodes, idx, target);
        saveWithUniqueOrders(nodes);
    }

    /** Gắn (hoặc cập nhật) bài test cho node BRANCH_TEST. [LEGACY] không còn đường click từ UI hiện tại. */
    public void attachAssessment(Integer pathId, Integer nodeId, Integer assessmentId, Integer minScore, User requester) {
        LearningPath path = findByIdAndOwner(pathId, requester);
        LearningNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy node."));
        if (!node.getLearningPath().getId().equals(pathId)) {
            throw new IllegalArgumentException("Node không thuộc lộ trình này.");
        }
        if (!"BRANCH_TEST".equals(node.getNodeType())) {
            throw new IllegalArgumentException("Chỉ node bài test rẽ nhánh mới gắn được bài test.");
        }
        var assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài test."));
        if (!assessment.getCreatedBy().getId().equals(requester.getId())) {
            throw new IllegalStateException("Bạn không có quyền gắn bài test này.");
        }
        if (assessment.getSubject() == null
                || !assessment.getSubject().getId().equals(path.getSubject().getId())) {
            throw new IllegalArgumentException("Bài test phải thuộc cùng môn học với lộ trình.");
        }
        BranchRule rule = branchRuleRepository.findByNodeId(nodeId).orElseGet(BranchRule::new);
        rule.setNode(node);
        rule.setAssessmentId(assessmentId);
        if (minScore != null) {
            rule.setMinScore(minScore);
        } else if (rule.getMinScore() == null) {
            rule.setMinScore(0);
        }
        branchRuleRepository.save(rule);
    }

    /** Số lượng nội dung học tập của từng node trong lộ trình (hiển thị ở trang chi tiết lộ trình). */
    public Map<Integer, Integer> contentCounts(Integer pathId) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (LearningNode n : nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId)) {
            counts.put(n.getId(), nodeContentRepository.findByNode_IdOrderByDisplayOrderAscIdAsc(n.getId()).size());
        }
        return counts;
    }

    // ===== PRIVATE HELPERS =====

    private void deleteContentsForNode(Integer nodeId) {
        List<NodeContent> contents = nodeContentRepository.findByNode_IdOrderByDisplayOrderAscIdAsc(nodeId);
        nodeContentRepository.deleteAll(contents);
        nodeContentRepository.flush();
        for (NodeContent c : contents) {
            storageService.deleteIfManaged(c.getContentUrl());
        }
    }

    private void createVisibleStatusForPathClassrooms(Integer pathId, LearningNode node) {
        for (var classroom : classroomRepository.findByLearningPathId(pathId)) {
            if (classroomNodeStatusRepository
                    .findByClassroomIdAndNodeId(classroom.getId(), node.getId()).isPresent()) {
                continue;
            }
            ClassroomNodeStatus status = new ClassroomNodeStatus();
            status.setClassroom(classroom);
            status.setNode(node);
            status.setStatus(NodeVisibilityStatus.VISIBLE.name());
            classroomNodeStatusRepository.save(status);
        }
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
        int len = name.trim().length();
        if (len < 3 || len > 150) {
            throw new IllegalArgumentException("Tên lộ trình phải có độ dài từ 3 đến 150 ký tự.");
        }
    }

    private void validatePathDescription(String description) {
        if (description != null && description.trim().length() > 1000) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 1000 ký tự.");
        }
    }

    private void validateNodeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tiêu đề node không được để trống.");
        }
        int len = title.trim().length();
        if (len < 3 || len > 200) {
            throw new IllegalArgumentException("Tiêu đề node phải có độ dài từ 3 đến 200 ký tự.");
        }
    }

    private void validateNodeDescription(String description) {
        if (description != null && description.trim().length() > 2000) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 2000 ký tự.");
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
        if (excludeId != null) {
            guardNoCycle(excludeId, prereqId);
        }
        node.setPrerequisite(prereq);
    }

    private void guardNoCycle(Integer nodeId, Integer proposedPrereqId) {
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        Integer cursor = proposedPrereqId;
        while (cursor != null && visited.add(cursor)) {
            if (cursor.equals(nodeId)) {
                throw new IllegalArgumentException("Không thể đặt tiên quyết vì sẽ tạo vòng lặp phụ thuộc.");
            }
            LearningNode n = nodeRepository.findById(cursor).orElse(null);
            if (n == null) break;
            cursor = (n.getPrerequisite() != null) ? n.getPrerequisite().getId() : null;
        }
    }

    /** Node mới luôn được thêm vào cuối lộ trình (không còn chèn giữa qua UI). */
    private BigDecimal computeOrder(List<LearningNode> nodes) {
        if (nodes.isEmpty()) return BigDecimal.ONE;
        return nodes.get(nodes.size() - 1).getNodeOrder().add(BigDecimal.ONE);
    }

    private void saveWithUniqueOrders(List<LearningNode> orderedNodes) {
        BigDecimal temporaryStart = orderedNodes.stream()
                .map(LearningNode::getNodeOrder)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                .add(BigDecimal.ONE);

        // Free every final order before assigning it to a different row.
        for (int i = 0; i < orderedNodes.size(); i++) {
            orderedNodes.get(i).setNodeOrder(temporaryStart.add(BigDecimal.valueOf(i)));
        }
        nodeRepository.saveAllAndFlush(orderedNodes);

        for (int i = 0; i < orderedNodes.size(); i++) {
            orderedNodes.get(i).setNodeOrder(BigDecimal.valueOf(i + 1L));
        }
        nodeRepository.saveAllAndFlush(orderedNodes);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }



    public boolean hasClassroom(Integer pathId) {
        return classroomRepository.existsByLearningPathId(pathId);
    }

    public LearningPath clonePath(Integer id, User requester) {
        LearningPath source = findByIdAndOwner(id, requester);

        LearningPath copy = new LearningPath();
        copy.setSubject(source.getSubject());
        copy.setName(source.getName() + " - Bản sao");
        copy.setDescription(source.getDescription());
        copy.setCreatedBy(requester);
        copy.setStatus(LearningPathStatus.ACTIVE.name());
        LearningPath savedPath = pathRepository.save(copy);

        List<LearningNode> sourceNodes = nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(id);
        Map<Integer, LearningNode> nodeMap = new HashMap<>();

        for (LearningNode oldNode : sourceNodes) {
            LearningNode newNode = new LearningNode();
            newNode.setLearningPath(savedPath);
            newNode.setTitle(oldNode.getTitle());
            newNode.setDescription(oldNode.getDescription());
            newNode.setNodeOrder(oldNode.getNodeOrder());
            newNode.setNodeType(oldNode.getNodeType());
            newNode.setBranchTag(oldNode.getBranchTag());
            nodeMap.put(oldNode.getId(), nodeRepository.save(newNode));
        }

        for (LearningNode oldNode : sourceNodes) {
            LearningNode clonedNode = nodeMap.get(oldNode.getId());
            boolean changed = false;
            if (oldNode.getPrerequisite() != null) {
                clonedNode.setPrerequisite(nodeMap.get(oldNode.getPrerequisite().getId()));
                changed = true;
            }
            if (oldNode.getBranchOwnerNode() != null) {
                clonedNode.setBranchOwnerNode(nodeMap.get(oldNode.getBranchOwnerNode().getId()));
                changed = true;
            }
            if (changed) nodeRepository.save(clonedNode);
        }

        for (LearningNode oldNode : sourceNodes) {
            branchRuleRepository.findByNodeId(oldNode.getId()).ifPresent(oldRule -> {
                LearningNode clonedNode = nodeMap.get(oldNode.getId());
                BranchRule newRule = new BranchRule();
                newRule.setNode(clonedNode);
                newRule.setAssessmentId(oldRule.getAssessmentId());
                newRule.setMinScore(oldRule.getMinScore());
                branchRuleRepository.save(newRule);
            });
        }

        for (LearningNode oldNode : sourceNodes) {
            LearningNode clonedNode = nodeMap.get(oldNode.getId());
            nodeLevelService.cloneLevels(oldNode.getId(), clonedNode.getId());
        }

        return savedPath;
    }

    private void ensureStructureEditable(Integer pathId) {
        if (classroomRepository.existsByLearningPathId(pathId)) {
            throw new IllegalStateException("Cấu trúc lộ trình đã bị khóa vì đang được lớp học sử dụng.");
        }
    }
}