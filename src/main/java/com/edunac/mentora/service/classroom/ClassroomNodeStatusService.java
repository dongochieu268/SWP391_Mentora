package com.edunac.mentora.service.classroom;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import com.edunac.mentora.domain.classroom.NodeVisibilityStatus;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.ClassroomNodeView;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassroomNodeStatusService {

    private static final List<String> ALLOWED_STATUSES = List.of(
            NodeVisibilityStatus.HIDDEN.name(),
            NodeVisibilityStatus.VISIBLE.name()
    );

    private final ClassroomNodeStatusRepository statusRepository;
    private final ClassroomService classroomService;
    private final LearningNodeRepository nodeRepository;

    public ClassroomNodeStatusService(ClassroomNodeStatusRepository statusRepository,
                                      ClassroomService classroomService,
                                      LearningNodeRepository nodeRepository) {
        this.statusRepository = statusRepository;
        this.classroomService = classroomService;
        this.nodeRepository = nodeRepository;
    }

    public List<ClassroomNodeView> getNodesWithStatus(Integer classroomId, User teacher) {
        Classroom classroom = classroomService.findForTeacher(classroomId, teacher);
        List<LearningNode> nodes = nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(
                classroom.getLearningPath().getId());

        Map<Integer, String> statusByNodeId = statusRepository.findByClassroomId(classroomId).stream()
                .collect(Collectors.toMap(s -> s.getNode().getId(), ClassroomNodeStatus::getStatus));

        return nodes.stream()
                .map(node -> toView(node, statusByNodeId.getOrDefault(
                        node.getId(), NodeVisibilityStatus.HIDDEN.name())))
                .toList();
    }

    public void toggleVisibility(Integer classroomId, Integer nodeId, User teacher) {
        Classroom classroom = classroomService.findForTeacher(classroomId, teacher);
        LearningNode node = loadNodeInPath(nodeId, classroom);

        ClassroomNodeStatus status = statusRepository
                .findByClassroomIdAndNodeId(classroomId, nodeId)
                .orElseGet(() -> createStatus(classroom, node, NodeVisibilityStatus.HIDDEN.name()));

        String nextStatus = NodeVisibilityStatus.VISIBLE.name().equals(status.getStatus())
                ? NodeVisibilityStatus.HIDDEN.name()
                : NodeVisibilityStatus.VISIBLE.name();
        status.setStatus(nextStatus);
        statusRepository.save(status);
    }

    public void setVisibility(Integer classroomId, Integer nodeId, String newStatus, User teacher) {
        Classroom classroom = classroomService.findForTeacher(classroomId, teacher);
        LearningNode node = loadNodeInPath(nodeId, classroom);
        String normalized = normalizeStatus(newStatus);

        ClassroomNodeStatus status = statusRepository
                .findByClassroomIdAndNodeId(classroomId, nodeId)
                .orElseGet(() -> createStatus(classroom, node, NodeVisibilityStatus.HIDDEN.name()));
        status.setStatus(normalized);
        statusRepository.save(status);
    }

    public boolean isVisible(Integer classroomId, Integer nodeId) {
        return statusRepository.findByClassroomIdAndNodeId(classroomId, nodeId)
                .map(s -> NodeVisibilityStatus.VISIBLE.name().equals(s.getStatus()))
                .orElse(false);
    }

    private LearningNode loadNodeInPath(Integer nodeId, Classroom classroom) {
        LearningNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy node."));
        if (!node.getLearningPath().getId().equals(classroom.getLearningPath().getId())) {
            throw new IllegalArgumentException("Node không thuộc lộ trình của lớp học này.");
        }
        return node;
    }

    private ClassroomNodeStatus createStatus(Classroom classroom, LearningNode node, String status) {
        ClassroomNodeStatus row = new ClassroomNodeStatus();
        row.setClassroom(classroom);
        row.setNode(node);
        row.setStatus(status);
        return row;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái node không được để trống.");
        }
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái node phải là HIDDEN hoặc VISIBLE.");
        }
        return normalized;
    }

    private ClassroomNodeView toView(LearningNode node, String status) {
        String prereqTitle = node.getPrerequisite() != null ? node.getPrerequisite().getTitle() : null;
        return new ClassroomNodeView(
                node.getId(),
                node.getTitle(),
                node.getDescription(),
                node.getNodeOrder().stripTrailingZeros().toPlainString(),
                prereqTitle,
                status
        );
    }
}
