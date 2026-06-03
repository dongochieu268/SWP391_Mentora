package com.edunac.mentora.service.assessment;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.*;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.repository.assessment.AssessmentAttemptRepository;
import com.edunac.mentora.repository.assessment.AssessmentRepository;
import com.edunac.mentora.repository.assessment.QuestionOptionRepository;
import com.edunac.mentora.repository.assessment.QuestionRepository;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentAssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final ClassroomMemberService memberService;

    public StudentAssessmentService(
            AssessmentRepository assessmentRepository,
            AssessmentAttemptRepository attemptRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository,
            ClassroomMemberService memberService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.memberService = memberService;
    }

    @Transactional(readOnly = true)
    public Optional<AssessmentAttempt> findSubmittedAttempt(Integer assessmentId, Integer classroomId, User student) {
        return attemptRepository.findFirstByAssessment_IdAndClassroom_IdAndStudent_IdAndStatusOrderBySubmittedAtDesc(
                assessmentId, classroomId, student.getId(), AttemptStatus.SUBMITTED.name());
    }

    public AssessmentAttempt startAttempt(Integer assessmentId, Integer classroomId, User student) {
        Classroom classroom = memberService.requireActiveMember(classroomId, student);
        Assessment assessment = loadSelfPacedPublishedAssessment(assessmentId);

        Optional<AssessmentAttempt> submitted = findSubmittedAttempt(assessmentId, classroomId, student);
        if (submitted.isPresent()) {
            return submitted.get();
        }

        Optional<AssessmentAttempt> inProgress = attemptRepository
                .findFirstByAssessment_IdAndClassroom_IdAndStudent_IdAndStatusOrderByStartedAtDesc(
                        assessmentId, classroomId, student.getId(), AttemptStatus.IN_PROGRESS.name());
        if (inProgress.isPresent()) {
            return inProgress.get();
        }

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessment(assessment);
        attempt.setClassroom(classroom);
        attempt.setStudent(student);
        attempt.setStatus(AttemptStatus.IN_PROGRESS.name());
        attempt.setStartedAt(LocalDateTime.now());
        return attemptRepository.save(attempt);
    }

    public AssessmentAttempt submitAttempt(
            Integer classroomId,
            Integer attemptId,
            User student,
            Map<Integer, Integer> selectedOptionByQuestionId
    ) {
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt làm bài."));
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new IllegalStateException("Bạn không có quyền nộp lượt làm bài này.");
        }
        if (!attempt.getClassroom().getId().equals(classroomId)) {
            throw new IllegalArgumentException("Lượt làm bài không thuộc lớp học này.");
        }
        memberService.requireActiveMember(classroomId, student);
        if (AttemptStatus.SUBMITTED.name().equals(attempt.getStatus())) {
            return attempt;
        }
        if (!AttemptStatus.IN_PROGRESS.name().equals(attempt.getStatus())) {
            throw new IllegalStateException("Lượt làm bài không ở trạng thái có thể nộp.");
        }
        if (!AssessmentStatus.PUBLISHED.name().equals(attempt.getAssessment().getStatus())) {
            throw new IllegalStateException("Bài test chưa được publish.");
        }

        BigDecimal score = grade(attempt.getAssessment(), selectedOptionByQuestionId);
        attempt.setScore(score);
        attempt.setStatus(AttemptStatus.SUBMITTED.name());
        attempt.setSubmittedAt(LocalDateTime.now());
        return attemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public AssessmentAttempt findResult(Integer classroomId, Integer attemptId, User student) {
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả bài test."));
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new IllegalStateException("Bạn không có quyền xem kết quả này.");
        }
        if (!attempt.getClassroom().getId().equals(classroomId)) {
            throw new IllegalArgumentException("Kết quả không thuộc lớp học này.");
        }
        memberService.requireActiveMember(classroomId, student);
        return attempt;
    }

    private Assessment loadSelfPacedPublishedAssessment(Integer assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài test."));
        if (!AssessmentStatus.PUBLISHED.name().equals(assessment.getStatus())) {
            throw new IllegalStateException("Chỉ bài test đã publish mới được làm.");
        }
        if (!DeliveryMode.SELF_PACED.name().equals(assessment.getDeliveryMode())) {
            throw new IllegalStateException("Phần này chỉ hỗ trợ bài test SELF_PACED.");
        }
        return assessment;
    }

    private BigDecimal grade(Assessment assessment, Map<Integer, Integer> selectedOptionByQuestionId) {
        List<Question> questions = questionRepository.findByAssessment_IdOrderByIdAsc(assessment.getId());
        if (questions.isEmpty()) {
            throw new IllegalStateException("Bài test chưa có câu hỏi.");
        }

        List<Integer> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        Map<Integer, QuestionOption> optionById = optionRepository
                .findByQuestion_IdInOrderByQuestion_IdAscIdAsc(questionIds)
                .stream()
                .collect(Collectors.toMap(QuestionOption::getId, Function.identity()));

        Map<Integer, Integer> answers = selectedOptionByQuestionId == null
                ? Collections.emptyMap()
                : selectedOptionByQuestionId;

        BigDecimal score = BigDecimal.ZERO;
        for (Question question : questions) {
            Integer selectedOptionId = answers.get(question.getId());
            if (selectedOptionId == null) {
                continue;
            }

            QuestionOption selectedOption = optionById.get(selectedOptionId);
            if (selectedOption != null
                    && selectedOption.getQuestion().getId().equals(question.getId())
                    && selectedOption.isCorrect()) {
                score = score.add(question.getScore());
            }
        }
        return score;
    }
}
