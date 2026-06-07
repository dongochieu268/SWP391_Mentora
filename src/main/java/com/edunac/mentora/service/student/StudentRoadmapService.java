package com.edunac.mentora.service.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.branching.AssignedBranch;
import com.edunac.mentora.domain.branching.BranchTag;
import com.edunac.mentora.domain.branching.StudentBranchAssignment;
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
import com.edunac.mentora.service.branching.StudentBranchService;
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
    private final StudentBranchService branchService;

    public StudentRoadmapService(
            ClassroomMemberService memberService,
            LearningNodeRepository nodeRepository,
            ClassroomNodeStatusRepository nodeStatusRepository,
            NodeProgressRepository progressRepository,
            StudentBranchService branchService
    ) {
        this.memberService = memberService;
        this.nodeRepository = nodeRepository;
        this.nodeStatusRepository = nodeStatusRepository;
        this.progressRepository = progressRepository;
        this.branchService = branchService;
    }

    public StudentRoadmapView buildRoadmap(Integer classroomId, User student) {
        Classroom classroom = memberService.requireActiveMember(classroomId, student);
        Integer pathId = classroom.getLearningPath().getId();

        List<LearningNode> pathNodes = nodeRepository
                .findByLearningPathIdOrderByNodeOrderAsc(pathId);

        Map<Integer, String> visibilityMap = loadVisibilityMap(classroomId);
        Map<Integer, Boolean> completedMap = loadCompletionMap(classroomId, student.getId());
        Map<Integer, AssignedBranch> branchMap = loadBranchMap(student.getId(), classroomId);

        int visibleCount = 0;
        int completedVisibleCount = 0;

        List<StudentRoadmapNodeView> nodeViews = pathNodes.stream()
                .map(node -> {
                    boolean visible = isVisible(node.getId(), visibilityMap);
                    boolean prereqMet = isPrerequisiteMet(node, completedMap);
                    boolean completed = Boolean.TRUE.equals(completedMap.get(node.getId()));
                    boolean onCorrectBranch = isOnCorrectBranch(node, branchMap);

                    StudentRoadmapNodeState state = resolveState(
                            visible, prereqMet, completed, onCorrectBranch);

                    String prereqTitle = node.getPrerequisite() != null
                            ? node.getPrerequisite().getTitle() : null;

                    return new StudentRoadmapNodeView(node, state, prereqTitle);
                })
                .toList();

        for (StudentRoadmapNodeView view : nodeViews) {
            if (isVisible(view.getNode().getId(), visibilityMap)
                    && isOnCorrectBranch(view.getNode(), branchMap)) {
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

        Map<Integer, AssignedBranch> branchMap = loadBranchMap(studentId, classroomId);
        if (!isOnCorrectBranch(node, branchMap)) return false;

        Map<Integer, Boolean> completedMap = loadCompletionMap(classroomId, studentId);
        if (!isPrerequisiteMet(node, completedMap)) return false;

        return true;
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
        for (NodeProgress p : progressRepository.findByClassroom_IdAndStudent_Id(classroomId, studentId)) {
            map.put(p.getLearningNode().getId(), p.isCompleted());
        }
        return map;
    }

    private Map<Integer, AssignedBranch> loadBranchMap(Integer studentId, Integer classroomId) {
        Map<Integer, AssignedBranch> map = new HashMap<>();
        for (StudentBranchAssignment a : branchService.getAssignments(studentId, classroomId)) {
            map.put(a.getBranchNode().getId(), a.getAssignedBranch());
        }
        return map;
    }


    private boolean isOnCorrectBranch(LearningNode node, Map<Integer, AssignedBranch> branchMap) {
        String tag = node.getBranchTag();
        if (tag == null || BranchTag.MAIN.name().equals(tag)) {
            return true;
        }

        LearningNode owner = node.getBranchOwnerNode();
        if (owner == null) return true;

        AssignedBranch assigned = branchMap.get(owner.getId());
        if (assigned == null) {
            return false;
        }

        if (BranchTag.PASS.name().equals(tag)) return assigned == AssignedBranch.PASS;
        if (BranchTag.FAIL.name().equals(tag)) return assigned == AssignedBranch.FAIL;
        return true;
    }

    private boolean isVisible(Integer nodeId, Map<Integer, String> visibilityMap) {
        return NodeVisibilityStatus.VISIBLE.name().equals(visibilityMap.get(nodeId));
    }

    private boolean isPrerequisiteMet(LearningNode node, Map<Integer, Boolean> completedMap) {
        if (node.getPrerequisite() == null) return true;
        return Boolean.TRUE.equals(completedMap.get(node.getPrerequisite().getId()));
    }

    private StudentRoadmapNodeState resolveState(
            boolean visible, boolean prereqMet, boolean completed, boolean onCorrectBranch) {
        if (!visible || !onCorrectBranch) return StudentRoadmapNodeState.HIDDEN;
        if (!prereqMet) return StudentRoadmapNodeState.LOCKED;
        if (completed) return StudentRoadmapNodeState.COMPLETED;
        return StudentRoadmapNodeState.ACCESSIBLE;
    }
}