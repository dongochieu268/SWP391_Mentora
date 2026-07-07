package com.edunac.mentora.service.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import com.edunac.mentora.domain.classroom.NodeVisibilityStatus;
import com.edunac.mentora.domain.learning.NodeProgress;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.dto.StudentRoadmapNodeState;
import com.edunac.mentora.dto.StudentRoadmapNodeView;
import com.edunac.mentora.dto.StudentRoadmapView;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StudentRoadmapService {

    private final ClassroomMemberService memberService;
    private final LearningNodeRepository nodeRepository;
    private final ClassroomNodeStatusRepository nodeStatusRepository;
    private final NodeProgressRepository progressRepository;
    private final NodeLevelRepository nodeLevelRepository;

    public StudentRoadmapService(
            ClassroomMemberService memberService,
            LearningNodeRepository nodeRepository,
            ClassroomNodeStatusRepository nodeStatusRepository,
            NodeProgressRepository progressRepository,
            NodeLevelRepository nodeLevelRepository
    ) {
        this.memberService = memberService;
        this.nodeRepository = nodeRepository;
        this.nodeStatusRepository = nodeStatusRepository;
        this.progressRepository = progressRepository;
        this.nodeLevelRepository = nodeLevelRepository;
    }

    public StudentRoadmapView buildRoadmap(Integer classroomId, User student) {
        Classroom classroom = memberService.requireActiveMember(classroomId, student);
        Integer pathId = classroom.getLearningPath().getId();

        List<LearningNode> pathNodes = nodeRepository
                .findByLearningPathIdOrderByNodeOrderAsc(pathId);

        Map<Integer, String>       visibilityMap = loadVisibilityMap(classroomId);
        Map<Integer, NodeProgress> progressMap   = loadProgressMap(classroomId, student.getId());
        Map<Integer, Long>         levelCountMap = loadLevelCountMap(pathNodes);

        int visibleCount          = 0;
        int completedVisibleCount = 0;

        List<StudentRoadmapNodeView> nodeViews = pathNodes.stream()
                .map(node -> {
                    boolean visible   = isVisible(node.getId(), visibilityMap);
                    boolean prereqMet = isPrerequisiteMet(node, progressMap);
                    NodeProgress progress = progressMap.get(node.getId());
                    boolean completed = progress != null && progress.isCompleted();

                    StudentRoadmapNodeState state = resolveState(visible, prereqMet, completed);

                    String prereqTitle = node.getPrerequisite() != null
                            ? node.getPrerequisite().getTitle() : null;

                    int totalLevels = levelCountMap.getOrDefault(node.getId(), 0L).intValue();
                    Integer bestLevelNumber = progress != null ? progress.getBestLevelNumber() : null;

                    return new StudentRoadmapNodeView(
                            node, state, prereqTitle,
                            totalLevels, bestLevelNumber,
                            progress != null ? progress.getBestScore() : null);
                })
                .toList();

        for (StudentRoadmapNodeView view : nodeViews) {
            if (isVisible(view.getNode().getId(), visibilityMap)) {
                visibleCount++;
                if (view.getState() == StudentRoadmapNodeState.COMPLETED) {
                    completedVisibleCount++;
                }
            }
        }

        int completionPercent = visibleCount == 0
                ? 0 : (completedVisibleCount * 100) / visibleCount;

        return new StudentRoadmapView(
                classroom, nodeViews, completionPercent, visibleCount, completedVisibleCount);
    }

    public boolean canAccessNode(Integer nodeId, Integer studentId, Integer classroomId) {
        LearningNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return false;

        Map<Integer, String> visibilityMap = loadVisibilityMap(classroomId);
        if (!isVisible(nodeId, visibilityMap)) return false;

        Map<Integer, NodeProgress> progressMap = loadProgressMap(classroomId, studentId);
        if (!isPrerequisiteMet(node, progressMap)) return false;

        return true;
    }

    private Map<Integer, String> loadVisibilityMap(Integer classroomId) {
        Map<Integer, String> map = new HashMap<>();
        for (ClassroomNodeStatus row : nodeStatusRepository.findByClassroomId(classroomId)) {
            map.put(row.getNode().getId(), row.getStatus());
        }
        return map;
    }

    private Map<Integer, NodeProgress> loadProgressMap(Integer classroomId, Integer studentId) {
        Map<Integer, NodeProgress> map = new HashMap<>();
        for (NodeProgress p : progressRepository.findByClassroom_IdAndStudent_Id(classroomId, studentId)) {
            map.put(p.getLearningNode().getId(), p);
        }
        return map;
    }

    private Map<Integer, Long> loadLevelCountMap(List<LearningNode> pathNodes) {
        List<Integer> nodeIds = pathNodes.stream().map(LearningNode::getId).toList();
        Map<Integer, Long> map = new HashMap<>();
        for (NodeLevel level : nodeLevelRepository.findByLearningNode_IdInOrderByLevelNumberAsc(nodeIds)) {
            Integer nodeId = level.getLearningNode().getId();
            map.merge(nodeId, 1L, Long::sum);
        }
        return map;
    }

    private boolean isVisible(Integer nodeId, Map<Integer, String> visibilityMap) {
        return NodeVisibilityStatus.VISIBLE.name().equals(visibilityMap.get(nodeId));
    }

    private boolean isPrerequisiteMet(LearningNode node, Map<Integer, NodeProgress> progressMap) {
        if (node.getPrerequisite() == null) return true;
        NodeProgress prereqProgress = progressMap.get(node.getPrerequisite().getId());
        return prereqProgress != null && prereqProgress.isCompleted();
    }

    private StudentRoadmapNodeState resolveState(
            boolean visible, boolean prereqMet, boolean completed) {
        if (!visible) return StudentRoadmapNodeState.HIDDEN;
        if (!prereqMet) return StudentRoadmapNodeState.LOCKED;
        if (completed) return StudentRoadmapNodeState.COMPLETED;
        return StudentRoadmapNodeState.ACCESSIBLE;
    }
}