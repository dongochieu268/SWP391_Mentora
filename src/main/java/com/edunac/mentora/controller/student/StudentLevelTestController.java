package com.edunac.mentora.controller.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.level.AttemptQuestion;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.domain.level.NodeLevelAttempt;
import com.edunac.mentora.repository.classroom.ClassroomMemberRepository;
import com.edunac.mentora.repository.level.AttemptGrantRepository;
import com.edunac.mentora.repository.level.AttemptQuestionRepository;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import com.edunac.mentora.service.level.NodeLevelAttemptService;
import com.edunac.mentora.service.level.StudentLevelReviewService;
import com.edunac.mentora.service.student.StudentRoadmapService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Luồng làm bài Level Test (S3/S4/S5).
 */
@Controller
@RequestMapping("/student/classrooms/{classroomId}/levels/{levelId}/take")
public class StudentLevelTestController {

    private final NodeLevelAttemptService nodeLevelAttemptService;
    private final NodeLevelAttemptRepository nodeLevelAttemptRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final NodeLevelRepository nodeLevelRepository;
    private final AttemptGrantRepository attemptGrantRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final StudentRoadmapService studentRoadmapService;
    private final StudentLevelReviewService studentLevelReviewService;

    public StudentLevelTestController(
            NodeLevelAttemptService nodeLevelAttemptService,
            NodeLevelAttemptRepository nodeLevelAttemptRepository,
            AttemptQuestionRepository attemptQuestionRepository,
            NodeLevelRepository nodeLevelRepository,
            AttemptGrantRepository attemptGrantRepository,
            ClassroomMemberRepository classroomMemberRepository,
            StudentRoadmapService studentRoadmapService,
            StudentLevelReviewService studentLevelReviewService
    ) {
        this.nodeLevelAttemptService = nodeLevelAttemptService;
        this.nodeLevelAttemptRepository = nodeLevelAttemptRepository;
        this.attemptQuestionRepository = attemptQuestionRepository;
        this.nodeLevelRepository = nodeLevelRepository;
        this.attemptGrantRepository = attemptGrantRepository;
        this.classroomMemberRepository = classroomMemberRepository;
        this.studentRoadmapService = studentRoadmapService;
        this.studentLevelReviewService = studentLevelReviewService;
    }

    @GetMapping
    public String take(
            @PathVariable Integer classroomId,
            @PathVariable Integer levelId,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            User user = currentUser(session);

            // S3 §1: phải là thành viên ACTIVE của lớp
            boolean isActiveMember = classroomMemberRepository
                    .existsByClassroomIdAndUserIdAndStatus(classroomId, user.getId(), "ACTIVE");
            if (!isActiveMember) {
                throw new IllegalStateException("Bạn chưa được duyệt vào lớp.");
            }

            // S3 §2: phải có quyền truy cập node chứa level này
            NodeLevel nodeLevel = nodeLevelRepository.findById(levelId)
                    .orElseThrow(() -> new EntityNotFoundException("NodeLevel not found: " + levelId));
            Integer nodeId = nodeLevel.getLearningNode().getId();

            if (!studentRoadmapService.canAccessNode(nodeId, user.getId(), classroomId)) {
                throw new IllegalStateException("Bạn không có quyền truy cập node này.");
            }

            if (studentLevelReviewService.requiresReview(nodeLevel)
                    && !studentLevelReviewService.isReviewed(levelId, user.getId(), classroomId)) {
                throw new IllegalStateException(
                        "Bạn cần ôn material và bấm 'Đã ôn xong' trước khi làm level này.");
            }

            NodeLevelAttempt attempt = nodeLevelAttemptService.startAttempt(levelId, user.getId(), classroomId);

            List<AttemptQuestion> questions = attemptQuestionRepository.findWithOptionsByAttemptId(attempt.getId());

            // Tính số giây còn lại cho countdown timer
            long secondsRemaining = 0;
            if (attempt.getDeadlineAt() != null) {
                secondsRemaining = Math.max(0,
                        ChronoUnit.SECONDS.between(LocalDateTime.now(), attempt.getDeadlineAt()));
            } else if (attempt.getStartedAt() != null
                    && attempt.getNodeLevel().getDurationMinutes() != null) {
                LocalDateTime computed = attempt.getStartedAt()
                        .plusMinutes(attempt.getNodeLevel().getDurationMinutes());
                secondsRemaining = Math.max(0,
                        ChronoUnit.SECONDS.between(LocalDateTime.now(), computed));
            }

            model.addAttribute("attempt", attempt);
            model.addAttribute("questions", questions);
            model.addAttribute("secondsRemaining", secondsRemaining);
            model.addAttribute("classroomId", classroomId);
            model.addAttribute("levelId", levelId);
            model.addAttribute("nodeId", nodeId);
            model.addAttribute("user", user);
            model.addAttribute("activePage", "classrooms");
            return "student/level/take";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/classrooms/" + classroomId + "/roadmap";
        }
    }

    @PostMapping
    public String submit(
            @PathVariable Integer classroomId,
            @PathVariable Integer levelId,
            @RequestParam Integer attemptId,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            User user = currentUser(session);

            // S3 §1: phải là thành viên ACTIVE của lớp
            boolean isActiveMember = classroomMemberRepository
                    .existsByClassroomIdAndUserIdAndStatus(classroomId, user.getId(), "ACTIVE");
            if (!isActiveMember) {
                throw new IllegalStateException("Bạn chưa được duyệt vào lớp.");
            }

            Map<Integer, Integer> answers = parseAnswers(request);
            // S4 §1/§2: quyền sở hữu attempt được kiểm tra bên trong service
            nodeLevelAttemptService.submitAttempt(attemptId, user.getId(), classroomId, answers);
            return "redirect:/student/classrooms/" + classroomId + "/levels/" + levelId
                    + "/take/result?attemptId=" + attemptId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/classrooms/" + classroomId + "/levels/" + levelId + "/take";
        }
    }

    @GetMapping("/result")
    public String result(
            @PathVariable Integer classroomId,
            @PathVariable Integer levelId,
            @RequestParam Integer attemptId,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            User user = currentUser(session);

            NodeLevelAttempt attempt = nodeLevelAttemptRepository.findById(attemptId)
                    .orElseThrow(() -> new EntityNotFoundException("Attempt not found: " + attemptId));

            if (!attempt.getStudent().getId().equals(user.getId())
                    || !attempt.getClassroom().getId().equals(classroomId)
                    || !attempt.getNodeLevel().getId().equals(levelId)) {
                throw new IllegalStateException("Bạn không có quyền xem kết quả này.");
            }

            if (!attempt.isSubmitted()) {
                return "redirect:/student/classrooms/" + classroomId + "/levels/" + levelId + "/take";
            }

            List<AttemptQuestion> questions = attemptQuestionRepository.findWithOptionsByAttemptId(attemptId);

            NodeLevel nodeLevel = attempt.getNodeLevel();
            Integer nodeId = nodeLevel.getLearningNode().getId();

            // S5 §3/§4/§5: một biến "còn lượt hay không" chi phối cả 2 CTA.
            boolean attemptsRemain = true;
            if (nodeLevel.getMaxAttempts() != null) {
                long currentCount = nodeLevelAttemptRepository.countByNodeLevel_IdAndStudent_IdAndClassroom_Id(
                        levelId, user.getId(), classroomId);
                int extraAttempts = attemptGrantRepository.sumExtraAttempts(levelId, user.getId(), classroomId);
                long limit = nodeLevel.getMaxAttempts() + extraAttempts;
                attemptsRemain = currentCount < limit;
            }

            Optional<NodeLevel> nextLevel = nodeLevelRepository
                    .findTopByLearningNode_IdAndLevelNumberGreaterThanOrderByLevelNumberAsc(
                            nodeId, nodeLevel.getLevelNumber());

            // S5 §5: hết lượt → cả 2 CTA ẩn, chỉ còn thông báo hết lượt.
            boolean noAttemptsLeft = !attemptsRemain;
            // S5 §3: đạt + có level kế tiếp + còn lượt (đọc cờ passed đã lưu — bất biến).
            boolean canGoNext = attempt.isPassed() && nextLevel.isPresent() && attemptsRemain;
            // S5 §4: chưa đạt + còn lượt.
            boolean canRetry = !attempt.isPassed() && attemptsRemain;

            model.addAttribute("attempt", attempt);
            model.addAttribute("questions", questions);
            model.addAttribute("nodeLevel", nodeLevel);
            model.addAttribute("classroomId", classroomId);
            model.addAttribute("levelId", levelId);
            model.addAttribute("user", user);
            model.addAttribute("canRetry", canRetry);
            model.addAttribute("canGoNext", canGoNext);
            model.addAttribute("noAttemptsLeft", noAttemptsLeft);
            model.addAttribute("nextLevelId", nextLevel.map(NodeLevel::getId).orElse(null));
            model.addAttribute("activePage", "classrooms");
            return "student/level/result";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/classrooms/" + classroomId + "/roadmap";
        }
    }

    private Map<Integer, Integer> parseAnswers(HttpServletRequest request) {
        Map<Integer, Integer> answers = new HashMap<>();
        for (String paramName : request.getParameterMap().keySet()) {
            if (!paramName.startsWith("answers_")) {
                continue;
            }
            try {
                Integer questionId = Integer.valueOf(paramName.substring("answers_".length()));
                Integer optionId = Integer.valueOf(request.getParameter(paramName));
                answers.put(questionId, optionId);
            } catch (NumberFormatException ignored) {
                // Bỏ qua key sai định dạng; chấm điểm chỉ dựa vào option thuộc đúng question snapshot.
            }
        }
        return answers;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
