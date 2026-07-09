package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomMember;
import com.edunac.mentora.domain.learning.NodeProgress;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.level.AttemptQuestion;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.domain.level.NodeLevelAttempt;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.repository.level.AttemptQuestionRepository;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only view of student level-test results for the lecturer (L5).
 * Snapshots are immutable by design — there is no edit/regrade path here.
 */
@Service
@Transactional(readOnly = true)
public class LecturerResultService {

    private final LearningNodeRepository nodeRepository;
    private final NodeLevelRepository nodeLevelRepository;
    private final NodeProgressRepository nodeProgressRepository;
    private final NodeLevelAttemptRepository attemptRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final ClassroomMemberService memberService;

    public LecturerResultService(LearningNodeRepository nodeRepository,
                                 NodeLevelRepository nodeLevelRepository,
                                 NodeProgressRepository nodeProgressRepository,
                                 NodeLevelAttemptRepository attemptRepository,
                                 AttemptQuestionRepository attemptQuestionRepository,
                                 ClassroomMemberService memberService) {
        this.nodeRepository = nodeRepository;
        this.nodeLevelRepository = nodeLevelRepository;
        this.nodeProgressRepository = nodeProgressRepository;
        this.attemptRepository = attemptRepository;
        this.attemptQuestionRepository = attemptQuestionRepository;
        this.memberService = memberService;
    }

    /* ------------------------------------------------------------------
       View models
       ------------------------------------------------------------------ */

    @Getter
    @AllArgsConstructor
    public static class StudentRow {
        private final ClassroomMember member;
        private final long completedCount;
        private final BigDecimal totalBestScore;   // null when no scored attempt yet
    }

    @Getter
    @AllArgsConstructor
    public static class NodeAggregate {
        private final LearningNode node;
        private final BigDecimal averageBestScore;   // null khi chưa ai có điểm
        private final long passedCount;
        private final int passedPercent;
    }

    @Getter
    @AllArgsConstructor
    public static class ClassResults {
        private final List<StudentRow> rows;
        private final int nodeCount;
        private final List<NodeAggregate> nodeAggregates;
    }

    @Getter
    @AllArgsConstructor
    public static class LevelResult {
        private final NodeLevel level;
        private final List<NodeLevelAttempt> attempts;
    }

    @Getter
    @AllArgsConstructor
    public static class NodeResult {
        private final LearningNode node;
        private final List<LevelResult> levels;
        private final NodeProgress progress;   // null when the student never touched the node
    }

    @Getter
    @AllArgsConstructor
    public static class StudentDetail {
        private final ClassroomMember member;
        private final List<NodeResult> nodes;
    }

    @Getter
    @AllArgsConstructor
    public static class AttemptDetail {
        private final NodeLevelAttempt attempt;
        private final List<AttemptQuestion> questions;
    }

    /* ------------------------------------------------------------------
       L5 §1 — class table: one row per student, whole row opens detail
       ------------------------------------------------------------------ */
    public ClassResults getClassResults(Classroom classroom) {
        Integer classroomId = classroom.getId();
        List<LearningNode> nodes = nodeRepository
                .findByLearningPathIdOrderByNodeOrderAsc(classroom.getLearningPath().getId());
        List<ClassroomMember> members = memberService.getActiveMembers(classroomId);

        Map<Integer, List<NodeProgress>> progressByStudent =
                nodeProgressRepository.findByClassroom_Id(classroomId).stream()
                        .collect(Collectors.groupingBy(NodeProgress::getStudentId));

        List<StudentRow> rows = new ArrayList<>();
        for (ClassroomMember member : members) {
            List<NodeProgress> progress =
                    progressByStudent.getOrDefault(member.getUser().getId(), List.of());
            long completedCount = progress.stream().filter(NodeProgress::isCompleted).count();
            List<BigDecimal> scores = progress.stream()
                    .map(NodeProgress::getBestScore)
                    .filter(Objects::nonNull)
                    .toList();
            BigDecimal totalBestScore = scores.isEmpty() ? null
                    : scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            rows.add(new StudentRow(member, completedCount, totalBestScore));
        }

        return new ClassResults(rows, nodes.size(), buildNodeAggregates(classroomId, nodes, members));
    }

    /* ------------------------------------------------------------------
       L5 §5 — tổng hợp theo node: điểm trung bình + tỉ lệ đã đạt
       ------------------------------------------------------------------ */
    private List<NodeAggregate> buildNodeAggregates(Integer classroomId,
                                                    List<LearningNode> nodes,
                                                    List<ClassroomMember> members) {
        Map<Integer, List<NodeProgress>> progressByNode =
                nodeProgressRepository.findByClassroom_Id(classroomId).stream()
                        .collect(Collectors.groupingBy(NodeProgress::getNodeId));

        Map<Integer, Set<Integer>> passedStudentsByNode = new HashMap<>();
        for (NodeLevelAttempt attempt : attemptRepository.findByClassroom_Id(classroomId)) {
            if (!attempt.isPassed()) continue;
            Integer nodeId = attempt.getNodeLevel().getLearningNode().getId();
            passedStudentsByNode.computeIfAbsent(nodeId, k -> new HashSet<>())
                    .add(attempt.getStudent().getId());
        }

        int activeCount = members.size();
        List<NodeAggregate> aggregates = new ArrayList<>();
        for (LearningNode node : nodes) {
            List<BigDecimal> scores = progressByNode.getOrDefault(node.getId(), List.of()).stream()
                    .map(NodeProgress::getBestScore)
                    .filter(Objects::nonNull)
                    .toList();
            BigDecimal average = scores.isEmpty() ? null
                    : scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
            long passedCount = passedStudentsByNode.getOrDefault(node.getId(), Set.of()).size();
            int passedPercent = activeCount == 0 ? 0
                    : (int) Math.round(passedCount * 100.0 / activeCount);
            aggregates.add(new NodeAggregate(node, average, passedCount, passedPercent));
        }
        return aggregates;
    }

    /* ------------------------------------------------------------------
       L6 §3 — xuất CSV: mỗi dòng một (học sinh × node)
       ------------------------------------------------------------------ */
    public String buildResultsCsv(Classroom classroom) {
        Integer classroomId = classroom.getId();
        List<LearningNode> nodes = nodeRepository
                .findByLearningPathIdOrderByNodeOrderAsc(classroom.getLearningPath().getId());
        List<ClassroomMember> members = memberService.getActiveMembers(classroomId);

        Map<Integer, Map<Integer, NodeProgress>> progressByStudentAndNode =
                nodeProgressRepository.findByClassroom_Id(classroomId).stream()
                        .collect(Collectors.groupingBy(NodeProgress::getStudentId,
                                Collectors.toMap(NodeProgress::getNodeId, p -> p, (a, b) -> a)));

        Map<Integer, Map<Integer, Long>> submittedCountByStudentAndNode =
                attemptRepository.findByClassroom_Id(classroomId).stream()
                        .filter(NodeLevelAttempt::isSubmitted)
                        .collect(Collectors.groupingBy(a -> a.getStudent().getId(),
                                Collectors.groupingBy(a -> a.getNodeLevel().getLearningNode().getId(),
                                        Collectors.counting())));

        StringBuilder csv = new StringBuilder();
        csv.append("Học sinh,Email,Node,Level tốt nhất,Điểm tốt nhất,Số lượt đã nộp\n");
        for (ClassroomMember member : members) {
            Integer studentId = member.getUser().getId();
            Map<Integer, NodeProgress> progressByNode =
                    progressByStudentAndNode.getOrDefault(studentId, Map.of());
            Map<Integer, Long> countByNode =
                    submittedCountByStudentAndNode.getOrDefault(studentId, Map.of());
            for (LearningNode node : nodes) {
                NodeProgress progress = progressByNode.get(node.getId());
                csv.append(csvField(member.getUser().getFullName())).append(',')
                        .append(csvField(member.getUser().getEmail())).append(',')
                        .append(csvField(node.getTitle())).append(',')
                        .append(progress != null && progress.getBestLevelNumber() != null
                                ? progress.getBestLevelNumber().toString() : "").append(',')
                        .append(progress != null && progress.getBestScore() != null
                                ? progress.getBestScore().toPlainString() : "").append(',')
                        .append(countByNode.getOrDefault(node.getId(), 0L)).append('\n');
            }
        }
        return csv.toString();
    }

    private String csvField(String value) {
        if (value == null) return "";
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /* ------------------------------------------------------------------
       L5 §2 — one student's attempt history per level of each node
       ------------------------------------------------------------------ */
    public StudentDetail getStudentDetail(Classroom classroom, Integer studentId) {
        ClassroomMember member = memberService.getActiveMembers(classroom.getId()).stream()
                .filter(m -> m.getUser().getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy học sinh trong lớp này."));

        List<LearningNode> nodes = nodeRepository
                .findByLearningPathIdOrderByNodeOrderAsc(classroom.getLearningPath().getId());

        List<NodeResult> nodeResults = new ArrayList<>();
        for (LearningNode node : nodes) {
            List<NodeLevel> levels =
                    nodeLevelRepository.findByLearningNode_IdOrderByLevelNumberAsc(node.getId());
            if (levels.isEmpty()) continue;   // plain lesson — no attempt history to show

            Map<Integer, List<NodeLevelAttempt>> attemptsByLevel = attemptRepository
                    .findByNodeLevel_LearningNode_IdAndStudent_IdAndClassroom_IdOrderByNodeLevel_LevelNumberAscAttemptNumberAsc(
                            node.getId(), studentId, classroom.getId())
                    .stream()
                    .collect(Collectors.groupingBy(a -> a.getNodeLevel().getId()));

            List<LevelResult> levelResults = levels.stream()
                    .map(level -> new LevelResult(level,
                            attemptsByLevel.getOrDefault(level.getId(), List.of())))
                    .toList();

            NodeProgress progress = nodeProgressRepository
                    .findByStudent_IdAndLearningNode_IdAndClassroom_Id(
                            studentId, node.getId(), classroom.getId())
                    .orElse(null);

            nodeResults.add(new NodeResult(node, levelResults, progress));
        }

        return new StudentDetail(member, nodeResults);
    }

    /* ------------------------------------------------------------------
       L5 §3 — read-only snapshot of one submitted attempt
       ------------------------------------------------------------------ */
    public AttemptDetail getAttemptDetail(Classroom classroom, Integer attemptId) {
        NodeLevelAttempt attempt = attemptRepository.findById(attemptId)
                .filter(a -> a.getClassroom().getId().equals(classroom.getId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy lượt làm bài trong lớp này."));
        if (!attempt.isSubmitted()) {
            throw new IllegalStateException("Lượt làm bài chưa được nộp.");
        }
        return new AttemptDetail(attempt,
                attemptQuestionRepository.findWithOptionsByAttemptId(attemptId));
    }
}
