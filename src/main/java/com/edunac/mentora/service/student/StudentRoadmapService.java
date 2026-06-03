package com.edunac.mentora.service.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import com.edunac.mentora.domain.classroom.NodeVisibilityStatus;
import com.edunac.mentora.domain.learning.NodeProgress;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.StudentRoadmapNodeState;
import com.edunac.mentora.dto.StudentRoadmapNodeView;
import com.edunac.mentora.dto.StudentRoadmapView;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class    StudentRoadmapService {

    private final ClassroomMemberService memberService;
    private final LearningNodeRepository nodeRepository;
    private final ClassroomNodeStatusRepository nodeStatusRepository;
    private final NodeProgressRepository progressRepository;

    public StudentRoadmapService(
            ClassroomMemberService memberService,
            LearningNodeRepository nodeRepository,
            ClassroomNodeStatusRepository nodeStatusRepository,
            NodeProgressRepository progressRepository
    ) {
        this.memberService = memberService;
        this.nodeRepository = nodeRepository;
        this.nodeStatusRepository = nodeStatusRepository;
        this.progressRepository = progressRepository;
    }

    /**
     * UC33 – Xem lộ trình: lọc node VISIBLE và kiểm tra tiên quyết từ node_progress.
     */
    public StudentRoadmapView buildRoadmap(Integer classroomId, User student) {
        Classroom classroom = memberService.requireActiveMember(classroomId, student);
        Integer pathId = classroom.getLearningPath().getId();

        List<LearningNode> pathNodes = nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(pathId);
        Map<Integer, String> visibilityByNodeId = loadVisibilityMap(classroomId);
        Map<Integer, Boolean> completedByNodeId = loadCompletionMap(classroomId, student.getId());

        int visibleCount = 0;
        int completedVisibleCount = 0;

        List<StudentRoadmapNodeView> nodeViews = pathNodes.stream()
                .map(node -> {
                    boolean visible = isVisible(node.getId(), visibilityByNodeId);
                    boolean prereqMet = isPrerequisiteMet(node, completedByNodeId);
                    boolean completed = Boolean.TRUE.equals(completedByNodeId.get(node.getId()));

                    StudentRoadmapNodeState state = resolveState(visible, prereqMet, completed);
                    String prereqTitle = node.getPrerequisite() != null
                            ? node.getPrerequisite().getTitle()
                            : null;
                    return new StudentRoadmapNodeView(node, state, prereqTitle);
                })
                .toList();

        for (StudentRoadmapNodeView view : nodeViews) {
            if (isVisible(view.getNode().getId(), visibilityByNodeId)) {
                visibleCount++;
                if (view.getState() == StudentRoadmapNodeState.COMPLETED) {
                    completedVisibleCount++;
                }
            }
        }

        int completionPercent = visibleCount == 0
                ? 0
                : (completedVisibleCount * 100) / visibleCount;

        return new StudentRoadmapView(
                classroom, nodeViews, completionPercent, visibleCount, completedVisibleCount);
    }

    private Map<Integer, String> loadVisibilityMap(Integer classroomId) {
        Map<Integer, String> map = new HashMap<>();
        for (ClassroomNodeStatus row : nodeStatusRepository.findByClassroomId(classroomId)) {
            map.put(row.getNode().getId(), row.getStatus());
        }
        return map;
    }

    private Map<Integer, Boolean> loadCompletionMap(Integer classroomId, Integer studentId) {
        Map<Integer, Boolean> map = new HashMap<>();
        for (NodeProgress progress : progressRepository.findByClassroomIdAndStudentId(classroomId, studentId)) {
            map.put(progress.getLearningNode().getId(), progress.isCompleted());
        }
        return map;
    }

    private boolean isVisible(Integer nodeId, Map<Integer, String> visibilityByNodeId) {
        String status = visibilityByNodeId.get(nodeId);
        return NodeVisibilityStatus.VISIBLE.name().equals(status);
    }

    private boolean isPrerequisiteMet(LearningNode node, Map<Integer, Boolean> completedByNodeId) {
        if (node.getPrerequisite() == null) {
            return true;
        }
        return Boolean.TRUE.equals(completedByNodeId.get(node.getPrerequisite().getId()));
    }

    private StudentRoadmapNodeState resolveState(boolean visible, boolean prereqMet, boolean completed) {
        if (!visible) {
            return StudentRoadmapNodeState.HIDDEN;
        }
        if (!prereqMet) {
            return StudentRoadmapNodeState.LOCKED;
        }
        if (completed) {
            return StudentRoadmapNodeState.COMPLETED;
        }
        return StudentRoadmapNodeState.ACCESSIBLE;
    }
}
