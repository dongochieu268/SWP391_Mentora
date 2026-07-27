package com.edunac.mentora.service.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import com.edunac.mentora.domain.classroom.NodeVisibilityStatus;
import com.edunac.mentora.domain.learning.NodeProgress;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.level.LevelMaterial;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.dto.StudentRoadmapNodeState;
import com.edunac.mentora.dto.StudentRoadmapNodeView;
import com.edunac.mentora.dto.StudentRoadmapView;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.repository.level.LevelMaterialRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import com.edunac.mentora.service.level.NodeLevelProgressionPolicy;
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
    private final LevelMaterialRepository levelMaterialRepository;
    private final NodeLevelAttemptRepository nodeLevelAttemptRepository;
    private final NodeLevelProgressionPolicy progressionPolicy;

    public StudentRoadmapService(
            ClassroomMemberService memberService,
            LearningNodeRepository nodeRepository,
            ClassroomNodeStatusRepository nodeStatusRepository,
            NodeProgressRepository progressRepository,
            NodeLevelRepository nodeLevelRepository,
            LevelMaterialRepository levelMaterialRepository,
            NodeLevelAttemptRepository nodeLevelAttemptRepository,
            NodeLevelProgressionPolicy progressionPolicy
    ) {
        this.memberService = memberService;
        this.nodeRepository = nodeRepository;
        this.nodeStatusRepository = nodeStatusRepository;
        this.progressRepository = progressRepository;
        this.nodeLevelRepository = nodeLevelRepository;
        this.levelMaterialRepository = levelMaterialRepository;
        this.nodeLevelAttemptRepository = nodeLevelAttemptRepository;
        this.progressionPolicy = progressionPolicy;
    }

    public StudentRoadmapView buildRoadmap(Integer classroomId, User student) {
        Classroom classroom = memberService.requireActiveMember(classroomId, student);
        Integer pathId = classroom.getLearningPath().getId();

        List<LearningNode> pathNodes = nodeRepository
                .findByLearningPathIdOrderByNodeOrderAsc(pathId);

        List<NodeLevel> levels = nodeLevelRepository.findByLearningNode_IdInOrderByLevelNumberAsc(
                pathNodes.stream().map(LearningNode::getId).toList());

        Map<Integer, String>       visibilityMap  = loadVisibilityMap(classroomId);
        Map<Integer, NodeProgress> progressMap    = loadProgressMap(classroomId, student.getId());
        Map<Integer, Long>         levelCountMap  = buildLevelCountMap(levels);
        Map<Integer, Integer>      passedLevelMap = buildHighestPassedLevelMap(classroomId, student.getId());

        int visibleCount          = 0;
        int completedVisibleCount = 0;

        List<StudentRoadmapNodeView> nodeViews = pathNodes.stream()
                .map(node -> {
                    boolean visible   = isVisible(node.getId(), visibilityMap);
                    boolean prereqMet = isPrerequisiteMet(
                            node, progressMap, student.getId(), classroomId);
                    NodeProgress progress = progressMap.get(node.getId());
                    boolean completed = progress != null && progress.isCompleted();

                    StudentRoadmapNodeState state = resolveState(visible, prereqMet, completed);

                    String prereqTitle = node.getPrerequisite() != null
                            ? node.getPrerequisite().getTitle() : null;

                    int totalLevels = levelCountMap.getOrDefault(node.getId(), 0L).intValue();
                    Integer bestLevelNumber = passedLevelMap.get(node.getId());

                    return new StudentRoadmapNodeView(
                            node, state, prereqTitle,
                            totalLevels, bestLevelNumber,
                            progress != null ? progress.getBestScore() : null);
                })
                .toList();

        for (StudentRoadmapNodeView view : nodeViews) {
            Integer nodeId = view.getNode().getId();
            if (isVisible(nodeId, visibilityMap)) {
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
        if (!isPrerequisiteMet(node, progressMap, studentId, classroomId)) return false;

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

    private Map<Integer, Long> buildLevelCountMap(List<NodeLevel> levels) {
        if (levels.isEmpty()) return Map.of();

        List<Integer> levelIds = levels.stream().map(NodeLevel::getId).toList();
        Map<Integer, Integer> questionCountByLevel = new HashMap<>();
        for (LevelMaterial levelMaterial : levelMaterialRepository.findByNodeLevel_IdIn(levelIds)) {
            questionCountByLevel.merge(
                    levelMaterial.getNodeLevel().getId(),
                    levelMaterial.getQuestionCount(),
                    Integer::sum);
        }

        Map<Integer, Long> map = new HashMap<>();
        for (NodeLevel level : levels) {
            if (questionCountByLevel.getOrDefault(level.getId(), 0) < 1) continue;
            Integer nodeId = level.getLearningNode().getId();
            map.merge(nodeId, 1L, Long::sum);
        }
        return map;
    }
    
    private Map<Integer, Integer> buildHighestPassedLevelMap(Integer classroomId, Integer studentId) {
        Map<Integer, Integer> map = new HashMap<>();
        nodeLevelAttemptRepository
                .findByClassroom_IdAndStudent_IdAndStatusAndPassedTrue(classroomId, studentId, "SUBMITTED")
                .forEach(attempt -> map.merge(
                        attempt.getNodeLevel().getLearningNode().getId(),
                        attempt.getNodeLevel().getLevelNumber(),
                        Math::max));
        return map;
    }

    private boolean isVisible(Integer nodeId, Map<Integer, String> visibilityMap) {
        return NodeVisibilityStatus.VISIBLE.name().equals(visibilityMap.get(nodeId));
    }

    private boolean isPrerequisiteMet(
            LearningNode node,
            Map<Integer, NodeProgress> progressMap,
            Integer studentId,
            Integer classroomId) {
        if (node.getPrerequisite() == null) return true;
        NodeProgress prereqProgress = progressMap.get(node.getPrerequisite().getId());
        if (prereqProgress != null && prereqProgress.isCompleted()) return true;

        return progressionPolicy.canProgressBeyondNode(
                node.getPrerequisite().getId(), studentId, classroomId);
    }

    private StudentRoadmapNodeState resolveState(
            boolean visible, boolean prereqMet, boolean completed) {
        if (!visible) return StudentRoadmapNodeState.HIDDEN;
        if (!prereqMet) return StudentRoadmapNodeState.LOCKED;
        if (completed) return StudentRoadmapNodeState.COMPLETED;
        return StudentRoadmapNodeState.ACCESSIBLE;
    }
}
