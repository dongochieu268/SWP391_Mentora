package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.assessment.AttemptStatus;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomStatus;
import com.edunac.mentora.domain.level.AttemptQuestion;
import com.edunac.mentora.domain.level.AttemptQuestionOption;
import com.edunac.mentora.domain.level.LevelMaterial;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.domain.level.NodeLevelAttempt;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import com.edunac.mentora.repository.level.AttemptGrantRepository;
import com.edunac.mentora.repository.level.AttemptQuestionRepository;
import com.edunac.mentora.repository.level.LevelMaterialRepository;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import com.edunac.mentora.repository.UserRepository;
import com.edunac.mentora.service.learning.NodeProgressService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NodeLevelAttemptService {

    private final NodeLevelRepository nodeLevelRepository;
    private final NodeLevelAttemptRepository nodeLevelAttemptRepository;
    private final ClassroomRepository classroomRepository;
    private final AttemptGrantRepository attemptGrantRepository;
    private final LevelMaterialRepository levelMaterialRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final UserRepository userRepository;
    private final NodeLevelQuestionService nodeLevelQuestionService;
    private final NodeProgressService nodeProgressService;


    /**
     * Method unlock dùng chung (S4 §5, rev 06d — ratchet đã gỡ): mở khóa level
     * kế tiếp ⟺ có attempt SUBMITTED của level hiện tại với passed = 1.
     * Roadmap chỉ gọi method này, không tự tính lại (work-plan §3 điểm 2).
     */
    public boolean isNextLevelUnlocked(Integer levelId, Integer studentId, Integer classroomId) {
        NodeLevel currentLevel = nodeLevelRepository.findById(levelId)
                .orElseThrow(() -> new EntityNotFoundException("NodeLevel not found: " + levelId));

        Integer nodeId = currentLevel.getLearningNode().getId();

        Optional<NodeLevel> previousLevel = nodeLevelRepository
                .findTopByLearningNode_IdAndLevelNumberLessThanOrderByLevelNumberDesc(
                        nodeId, currentLevel.getLevelNumber());

        if (previousLevel.isEmpty()) {
            return true;
        }

        return nodeLevelAttemptRepository.existsByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatusAndPassedTrue(
                previousLevel.get().getId(), studentId, classroomId, AttemptStatus.SUBMITTED.name());
    }


    /**
     * S3: bắt đầu (hoặc resume) một lượt làm bài level test.
     * S3 §1/§2 (active member, canAccessNode) được kiểm tra ở phía controller
     * trước khi gọi method này (những check đó thuộc phạm vi classroom/roadmap,
     * cần session/current-user context mà service layer này không giữ).
     * S3 §4/§4b: chặn hết lượt (kể cả attempt_grants) + chặn start mới khi lớp COMPLETED.
     * S3 §6: nếu đề sinh ra 0 câu hỏi (material rỗng/chưa gắn), từ chối start.
     */
    @Transactional
    public NodeLevelAttempt startAttempt(Integer levelId, Integer studentId, Integer classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found: " + classroomId));

        // S3 §3 phải được kiểm tra TRƯỚC §4b: resume một attempt IN_PROGRESS luôn
        // được phép kể cả khi lớp đã COMPLETED ("ending the class never destroys
        // work in flight" — spec S3 §4b). Chỉ chặn khi thực sự tạo attempt MỚI.
        Optional<NodeLevelAttempt> inProgress = nodeLevelAttemptRepository
                .findFirstByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatusOrderByAttemptNumberDesc(
                        levelId, studentId, classroomId, AttemptStatus.IN_PROGRESS.name());
        if (inProgress.isPresent()) {
            return inProgress.get();
        }

        if (ClassroomStatus.COMPLETED.name().equals(classroom.getStatus())) {
            throw new IllegalStateException("Lớp học đã kết thúc.");
        }

        NodeLevel nodeLevel = nodeLevelRepository.findById(levelId)
                .orElseThrow(() -> new EntityNotFoundException("NodeLevel not found: " + levelId));

        long currentCount = nodeLevelAttemptRepository
                .countByNodeLevel_IdAndStudent_IdAndClassroom_Id(levelId, studentId, classroomId);

        if (nodeLevel.getMaxAttempts() != null) {
            int extraAttempts = attemptGrantRepository
                    .sumExtraAttempts(levelId, studentId, classroomId);
            long limit = nodeLevel.getMaxAttempts() + extraAttempts;
            if (currentCount >= limit) {
                throw new IllegalStateException("Bạn đã hết số lần thử cho level này.");
            }
        }

        NodeLevelAttempt attempt = new NodeLevelAttempt();
        attempt.setNodeLevel(nodeLevel);
        attempt.setStudent(userRepository.getReferenceById(studentId));
        attempt.setClassroom(classroom);
        attempt.setAttemptNumber((int) currentCount + 1);
        attempt.setStatus(AttemptStatus.IN_PROGRESS.name());
        attempt.setStartedAt(LocalDateTime.now());

        attempt = nodeLevelAttemptRepository.save(attempt);

        List<LevelMaterial> levelMaterials = levelMaterialRepository.findByNodeLevel_Id(levelId);
        nodeLevelQuestionService.generateQuestionSnapshot(attempt, levelMaterials);

        List<AttemptQuestion> generatedQuestions = attemptQuestionRepository.findWithOptionsByAttemptId(attempt.getId());
        if (generatedQuestions.isEmpty()) {
            throw new IllegalStateException("Level này chưa có câu hỏi trong ngân hàng.");
        }

        return attempt;
    }


    /**
     * S4: nộp bài + chấm điểm.
     * S4 §1/§2: chỉ chủ sở hữu attempt, đúng classroom mới được nộp.
     * S4 §3: idempotent — attempt đã SUBMITTED thì trả về nguyên trạng.
     * S4 §4: chấm điểm từ snapshot, score = pct × maxScore (sàn 0, làm tròn 2 số),
     * ghi selected 1 lần lúc submit, ghi passed = (score >= passingScore),
     * upsert NodeProgress.bestScore/bestLevelNumber (hòa điểm giữ nguyên bestLevelNumber).
     * S4 §4e: guard 0 câu hỏi (unreachable qua happy path vì S3 §6 đã chặn ở start;
     * chỉ còn ý nghĩa phòng thủ) → điểm 0, không chia cho 0.
     */
    @Transactional
    public NodeLevelAttempt submitAttempt(Integer attemptId, Integer studentId, Integer classroomId,
                                          Map<Integer, Integer> selectedOptionIds) {
        NodeLevelAttempt attempt = nodeLevelAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("NodeLevelAttempt not found: " + attemptId));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new IllegalStateException("Bạn không có quyền nộp lượt làm bài này.");
        }
        if (!attempt.getClassroom().getId().equals(classroomId)) {
            throw new IllegalArgumentException("Lượt làm bài không thuộc lớp học này.");
        }

        if (attempt.isSubmitted()) {
            return attempt;
        }

        List<AttemptQuestion> questions = attemptQuestionRepository.findWithOptionsByAttemptId(attemptId);

        int total = questions.size();
        int correctCount = 0;

        for (AttemptQuestion question : questions) {
            Integer chosenOptionId = selectedOptionIds.get(question.getId());
            boolean questionCorrect = false;

            for (AttemptQuestionOption option : question.getOptions()) {
                boolean isChosen = chosenOptionId != null && chosenOptionId.equals(option.getId());
                option.setSelected(isChosen);
                if (isChosen && option.isCorrect()) {
                    questionCorrect = true;
                }
            }

            if (questionCorrect) {
                correctCount++;
            }
        }

        NodeLevel nodeLevel = attempt.getNodeLevel();
        BigDecimal score;
        if (total == 0) {
            score = BigDecimal.ZERO;
            log.warn("Attempt {} có snapshot 0 câu hỏi lúc submit — chấm điểm 0.", attemptId);
        } else {
            score = nodeLevel.getMaxScore()
                    .multiply(BigDecimal.valueOf(correctCount))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        boolean passed = total > 0 && score.compareTo(nodeLevel.getPassingScore()) >= 0;

        attempt.setScore(score);
        attempt.setPassed(passed);
        attempt.setStatus(AttemptStatus.SUBMITTED.name());
        attempt.setSubmittedAt(LocalDateTime.now());

        attempt = nodeLevelAttemptRepository.save(attempt);


        nodeProgressService.updateOnAttemptSubmitted(
                nodeLevel.getLearningNode().getId(),
                attempt.getStudent().getId(),
                attempt.getClassroom().getId(),
                nodeLevel.getLevelNumber(),
                score);

        return attempt;
    }
}