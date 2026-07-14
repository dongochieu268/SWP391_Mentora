package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.AttemptStatus;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.level.AttemptQuestion;
import com.edunac.mentora.domain.level.AttemptQuestionOption;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.domain.level.NodeLevelAttempt;
import com.edunac.mentora.repository.UserRepository;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import com.edunac.mentora.repository.level.AttemptGrantRepository;
import com.edunac.mentora.repository.level.AttemptQuestionRepository;
import com.edunac.mentora.repository.level.LevelMaterialRepository;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import com.edunac.mentora.service.learning.NodeProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S4 §4: công thức chấm điểm score = correctPercent x maxScore (làm tròn 2
 * số, sàn 0), ghi passed = (score >= passingScore), idempotent khi đã
 * SUBMITTED, và guard 0 câu hỏi (S4 §4e) không chia cho 0.
 */
class NodeLevelAttemptServiceScoringTest {

    private static final int STUDENT_ID = 1;
    private static final int CLASSROOM_ID = 10;
    private static final int ATTEMPT_ID = 100;

    private NodeLevelAttemptRepository nodeLevelAttemptRepository;
    private AttemptQuestionRepository attemptQuestionRepository;
    private NodeProgressService nodeProgressService;
    private NodeLevelAttemptService service;

    @BeforeEach
    void setUp() {
        NodeLevelRepository nodeLevelRepository = mock(NodeLevelRepository.class);
        nodeLevelAttemptRepository = mock(NodeLevelAttemptRepository.class);
        ClassroomRepository classroomRepository = mock(ClassroomRepository.class);
        AttemptGrantRepository attemptGrantRepository = mock(AttemptGrantRepository.class);
        LevelMaterialRepository levelMaterialRepository = mock(LevelMaterialRepository.class);
        attemptQuestionRepository = mock(AttemptQuestionRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        NodeLevelQuestionService nodeLevelQuestionService = mock(NodeLevelQuestionService.class);
        nodeProgressService = mock(NodeProgressService.class);

        service = new NodeLevelAttemptService(
                nodeLevelRepository, nodeLevelAttemptRepository, classroomRepository,
                attemptGrantRepository, levelMaterialRepository, attemptQuestionRepository,
                userRepository, nodeLevelQuestionService, nodeProgressService);

        // save() trả về chính đối tượng được truyền vào (giả lập JPA save).
        when(nodeLevelAttemptRepository.save(any(NodeLevelAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(nodeLevelRepository
                .findTopByLearningNode_IdAndLevelNumberGreaterThanOrderByLevelNumberAsc(any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void diem_toi_da_khi_tra_loi_dung_het() {
        NodeLevelAttempt attempt = attempt(nodeLevel(BigDecimal.valueOf(10), BigDecimal.valueOf(5)));
        List<AttemptQuestion> questions = questions(2);
        when(attemptQuestionRepository.findWithOptionsByAttemptId(ATTEMPT_ID)).thenReturn(questions);
        when(nodeLevelAttemptRepository.findById(ATTEMPT_ID)).thenReturn(java.util.Optional.of(attempt));

        Map<Integer, Integer> answers = new HashMap<>();
        for (AttemptQuestion q : questions) {
            answers.put(q.getId(), correctOptionId(q));
        }

        NodeLevelAttempt result = service.submitAttempt(ATTEMPT_ID, STUDENT_ID, CLASSROOM_ID, answers);

        assertEquals(0, BigDecimal.valueOf(10).compareTo(result.getScore()));
        assertTrue(result.isPassed());
        assertEquals(AttemptStatus.SUBMITTED.name(), result.getStatus());
        verify(nodeProgressService).updateOnAttemptSubmitted(
                any(), eq(STUDENT_ID), eq(CLASSROOM_ID), any(), eq(result.getScore()), anyBoolean());
    }

    @Test
    void diem_lam_tron_2_chu_so_khi_khong_chia_het() {
        // 1/3 đúng, maxScore = 10 -> 3.333... -> làm tròn 2 số = 3.33
        NodeLevelAttempt attempt = attempt(nodeLevel(BigDecimal.valueOf(10), BigDecimal.valueOf(5)));
        List<AttemptQuestion> questions = questions(3);
        when(attemptQuestionRepository.findWithOptionsByAttemptId(ATTEMPT_ID)).thenReturn(questions);
        when(nodeLevelAttemptRepository.findById(ATTEMPT_ID)).thenReturn(java.util.Optional.of(attempt));

        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(questions.get(0).getId(), correctOptionId(questions.get(0)));
        // 2 câu còn lại không trả lời đúng (không nằm trong map -> tính sai)

        NodeLevelAttempt result = service.submitAttempt(ATTEMPT_ID, STUDENT_ID, CLASSROOM_ID, answers);

        assertEquals(0, new BigDecimal("3.33").compareTo(result.getScore()));
        assertFalse(result.isPassed(), "3.33 < passingScore 5 nên phải KHÔNG đạt");
    }

    @Test
    void guard_0_cau_hoi_khong_chia_cho_0() {
        NodeLevelAttempt attempt = attempt(nodeLevel(BigDecimal.valueOf(10), BigDecimal.valueOf(5)));
        when(attemptQuestionRepository.findWithOptionsByAttemptId(ATTEMPT_ID)).thenReturn(List.of());
        when(nodeLevelAttemptRepository.findById(ATTEMPT_ID)).thenReturn(java.util.Optional.of(attempt));

        NodeLevelAttempt result = service.submitAttempt(ATTEMPT_ID, STUDENT_ID, CLASSROOM_ID, Map.of());

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getScore()));
        assertFalse(result.isPassed());
    }

    @Test
    void nop_bai_da_submitted_thi_idempotent_khong_cham_lai() {
        NodeLevelAttempt attempt = attempt(nodeLevel(BigDecimal.valueOf(10), BigDecimal.valueOf(5)));
        attempt.setStatus(AttemptStatus.SUBMITTED.name());
        attempt.setScore(BigDecimal.valueOf(7));
        when(nodeLevelAttemptRepository.findById(ATTEMPT_ID)).thenReturn(java.util.Optional.of(attempt));

        NodeLevelAttempt result = service.submitAttempt(ATTEMPT_ID, STUDENT_ID, CLASSROOM_ID, Map.of());

        assertEquals(0, BigDecimal.valueOf(7).compareTo(result.getScore()));
        verify(nodeProgressService, never()).updateOnAttemptSubmitted(
                any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void khong_phai_chu_so_huu_attempt_thi_bi_tu_choi() {
        NodeLevelAttempt attempt = attempt(nodeLevel(BigDecimal.valueOf(10), BigDecimal.valueOf(5)));
        when(nodeLevelAttemptRepository.findById(ATTEMPT_ID)).thenReturn(java.util.Optional.of(attempt));

        int someoneElseId = STUDENT_ID + 999;
        assertThrows(IllegalStateException.class,
                () -> service.submitAttempt(ATTEMPT_ID, someoneElseId, CLASSROOM_ID, Map.of()));
    }

    // ---- helpers ----

    private NodeLevel nodeLevel(BigDecimal maxScore, BigDecimal passingScore) {
        NodeLevel nodeLevel = new NodeLevel();
        nodeLevel.setId(50);
        nodeLevel.setLevelNumber(1);
        nodeLevel.setMaxScore(maxScore);
        nodeLevel.setPassingScore(passingScore);
        com.edunac.mentora.domain.learningpath.LearningNode learningNode =
                new com.edunac.mentora.domain.learningpath.LearningNode();
        learningNode.setId(200);
        nodeLevel.setLearningNode(learningNode);
        return nodeLevel;
    }

    private NodeLevelAttempt attempt(NodeLevel nodeLevel) {
        NodeLevelAttempt attempt = new NodeLevelAttempt();
        attempt.setId(ATTEMPT_ID);
        attempt.setNodeLevel(nodeLevel);
        User student = new User();
        student.setId(STUDENT_ID);
        attempt.setStudent(student);
        Classroom classroom = new Classroom();
        classroom.setId(CLASSROOM_ID);
        attempt.setClassroom(classroom);
        attempt.setStatus(AttemptStatus.IN_PROGRESS.name());
        attempt.setAttemptNumber(1);
        return attempt;
    }

    private List<AttemptQuestion> questions(int count) {
        List<AttemptQuestion> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AttemptQuestion q = new AttemptQuestion();
            q.setId(1000 + i);
            q.setDisplayOrder(i + 1);

            AttemptQuestionOption correct = new AttemptQuestionOption();
            correct.setId(2000 + i * 2);
            correct.setCorrect(true);
            correct.setDisplayOrder(1);

            AttemptQuestionOption wrong = new AttemptQuestionOption();
            wrong.setId(2000 + i * 2 + 1);
            wrong.setCorrect(false);
            wrong.setDisplayOrder(2);

            List<AttemptQuestionOption> options = new ArrayList<>();
            options.add(correct);
            options.add(wrong);
            q.setOptions(options);
            list.add(q);
        }
        return list;
    }

    private Integer correctOptionId(AttemptQuestion question) {
        return question.getOptions().stream()
                .filter(AttemptQuestionOption::isCorrect)
                .findFirst()
                .orElseThrow()
                .getId();
    }
}
