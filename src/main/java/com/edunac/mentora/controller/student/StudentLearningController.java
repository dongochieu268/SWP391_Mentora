package com.edunac.mentora.controller.student;

import com.edunac.mentora.domain.branching.BranchRule;
import com.edunac.mentora.domain.assessment.AttemptStatus;
import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.level.LevelMaterial;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.domain.level.NodeLevelAttempt;
import com.edunac.mentora.dto.NodeProgressResponse;
import com.edunac.mentora.dto.StudentNodeLevelHistoryView;
import com.edunac.mentora.dto.StudentNodeLevelStatusView;
import com.edunac.mentora.dto.StudentLevelMaterialView;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.repository.level.AttemptGrantRepository;
import com.edunac.mentora.repository.level.LevelMaterialRepository;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import com.edunac.mentora.service.branching.BranchRuleService;
import com.edunac.mentora.service.learning.NodeContentService;
import com.edunac.mentora.service.learning.NodeProgressService;
import com.edunac.mentora.service.level.NodeLevelAttemptService;
import com.edunac.mentora.service.level.StudentLevelReviewService;
import com.edunac.mentora.service.student.StudentRoadmapService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentLearningController {

    private final NodeProgressService nodeProgressService;
    private final NodeContentService nodeContentService;
    private final LearningNodeRepository learningNodeRepository;
    private final StudentRoadmapService roadmapService;
    private final BranchRuleService branchRuleService;
    private final NodeLevelRepository nodeLevelRepository;
    private final NodeLevelAttemptRepository nodeLevelAttemptRepository;
    private final LevelMaterialRepository levelMaterialRepository;
    private final AttemptGrantRepository attemptGrantRepository;
    private final NodeLevelAttemptService nodeLevelAttemptService;
    private final StudentLevelReviewService studentLevelReviewService;

    @GetMapping("/classrooms/{classroomId}/nodes")
    public String viewLearningPath(@PathVariable Integer classroomId) {
        return "redirect:/student/classrooms/" + classroomId + "/roadmap";
    }

    @GetMapping("/classrooms/{classroomId}/nodes/{nodeId}")
    public String viewNodeDetail(
            @PathVariable Integer classroomId,
            @PathVariable Integer nodeId,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!roadmapService.canAccessNode(nodeId, currentUser.getId(), classroomId)) {
            ra.addFlashAttribute("error", "Bạn không có quyền truy cập bài học này.");
            return "redirect:/student/classrooms/" + classroomId + "/roadmap";
        }

        LearningNode node = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node không tồn tại"));

        List<NodeContent> contents = nodeContentService.getByNodeId(nodeId);

        NodeProgressResponse progress = nodeProgressService
                .buildProgressResponse(currentUser.getId(), classroomId, nodeId);

        List<LearningNode> allNodes = learningNodeRepository.findVisibleNodesByClassroom(classroomId);
        int currentIndex = -1;
        for (int i = 0; i < allNodes.size(); i++) {
            if (allNodes.get(i).getId().equals(nodeId)) {
                currentIndex = i;
                break;
            }
        }
        Integer prevNodeId = currentIndex > 0 ? allNodes.get(currentIndex - 1).getId() : null;
        Integer nextNodeId = currentIndex < allNodes.size() - 1
                ? allNodes.get(currentIndex + 1).getId() : null;

        List<StudentNodeLevelHistoryView> levelHistory =
                buildLevelHistory(nodeId, currentUser.getId(), classroomId);

        List<StudentNodeLevelStatusView> levelStatus =
                buildLevelStatusList(nodeId, currentUser.getId(), classroomId);
        List<StudentLevelMaterialView> levelMaterials =
                buildLevelMaterials(nodeId, currentUser.getId(), classroomId);
        boolean hasStartableLevel = levelStatus.stream()
                .anyMatch(StudentNodeLevelStatusView::isHasQuestions);

        model.addAttribute("user", currentUser);
        model.addAttribute("activePage", "classrooms");
        model.addAttribute("node", node);
        model.addAttribute("contents", contents);
        model.addAttribute("progress", progress);
        model.addAttribute("classroomId", classroomId);
        model.addAttribute("prevNodeId", prevNodeId);
        model.addAttribute("nextNodeId", nextNodeId);
        model.addAttribute("levelHistory", levelHistory);
        model.addAttribute("levelStatus", levelStatus);
        model.addAttribute("levelMaterials", levelMaterials);
        model.addAttribute("hasStartableLevel", hasStartableLevel);

        // Fix: nếu node là BRANCH_TEST thì truyền thêm branchRule vào model
        // để node-detail.html hiển thị nút "Làm bài test" thay vì nội dung thông thường
        if (node.isBranchTest()) {
            Optional<BranchRule> branchRule = branchRuleService.findByNodeId(nodeId);
            model.addAttribute("branchRule", branchRule.orElse(null));
            model.addAttribute("isBranchTest", true);
        } else {
            model.addAttribute("isBranchTest", false);
        }

        return "student/learning/node-detail";
    }

    @PostMapping("/classrooms/{classroomId}/nodes/{nodeId}/complete")
    @ResponseBody
    public ResponseEntity<?> completeNode(
            @PathVariable Integer classroomId,
            @PathVariable Integer nodeId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
        }

        if (!roadmapService.canAccessNode(nodeId, currentUser.getId(), classroomId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền truy cập bài học này.");
        }

        try {
            NodeProgressResponse result = nodeProgressService
                    .markNodeCompleted(currentUser.getId(), nodeId, classroomId, null);
            return ResponseEntity.ok(result);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/classrooms/{classroomId}/nodes/{nodeId}/levels/{levelId}/reviewed")
    public String markLevelReviewed(@PathVariable Integer classroomId, @PathVariable Integer nodeId,
                                    @PathVariable Integer levelId, HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        try {
            if (!roadmapService.canAccessNode(nodeId, user.getId(), classroomId))
                throw new IllegalStateException("Bạn không có quyền truy cập bài học này.");
            studentLevelReviewService.markReviewed(levelId, nodeId, user.getId(), classroomId);
            ra.addFlashAttribute("success", "Đã xác nhận ôn tập. Level tiếp theo đã được mở.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/classrooms/" + classroomId + "/nodes/" + nodeId;
    }

    private List<StudentNodeLevelHistoryView> buildLevelHistory(
            Integer nodeId, Integer studentId, Integer classroomId) {

        List<NodeLevel> levels = nodeLevelRepository.findByLearningNode_IdOrderByLevelNumberAsc(nodeId);
        if (levels.isEmpty()) return List.of();

        Map<Integer, List<NodeLevelAttempt>> attemptsByLevel = new HashMap<>();
        for (NodeLevelAttempt attempt : nodeLevelAttemptRepository
                .findByNodeLevel_LearningNode_IdAndStudent_IdAndClassroom_IdOrderByNodeLevel_LevelNumberAscAttemptNumberAsc(
                        nodeId, studentId, classroomId)) {
            attemptsByLevel
                    .computeIfAbsent(attempt.getNodeLevel().getId(), k -> new ArrayList<>())
                    .add(attempt);
        }

        List<StudentNodeLevelHistoryView> result = new ArrayList<>();
        for (NodeLevel level : levels) {
            List<NodeLevelAttempt> levelAttempts =
                    attemptsByLevel.getOrDefault(level.getId(), List.of());

            BigDecimal bestScore = null;
            boolean passed = false;
            for (NodeLevelAttempt attempt : levelAttempts) {
                if (!attempt.isSubmitted()) continue;
                if (bestScore == null || attempt.getScore().compareTo(bestScore) > 0) {
                    bestScore = attempt.getScore();
                }
                if (attempt.isPassed()) passed = true;
            }

            result.add(new StudentNodeLevelHistoryView(
                    level.getLevelNumber(), level.getTitle(), level.getMaxScore(), level.getPassingScore(),
                    levelAttempts.size(), bestScore, passed));
        }
        return result;
    }

    /**
     * S2: trạng thái nút "Start Test" mỗi level — khóa theo level trước chưa
     * qua (isNextLevelUnlocked, S4 §5), hết lượt (maxAttempts + attempt_grants),
     * hoặc chưa có câu hỏi (OQ2: node mà mọi level đều 0 câu hỏi = lesson
     * thường, hoàn thành tay — hasQuestions cho phép caller phát hiện case đó).
     */
    private List<StudentNodeLevelStatusView> buildLevelStatusList(
            Integer nodeId, Integer studentId, Integer classroomId) {

        List<NodeLevel> levels = nodeLevelRepository.findByLearningNode_IdOrderByLevelNumberAsc(nodeId);
        if (levels.isEmpty()) return List.of();

        List<Integer> levelIds = levels.stream().map(NodeLevel::getId).toList();
        Map<Integer, Integer> questionCountByLevel = new HashMap<>();
        for (LevelMaterial lm : levelMaterialRepository.findByNodeLevel_IdIn(levelIds)) {
            questionCountByLevel.merge(lm.getNodeLevel().getId(), lm.getQuestionCount(), Integer::sum);
        }

        List<StudentNodeLevelStatusView> result = new ArrayList<>();
        for (NodeLevel level : levels) {
            boolean hasQuestions = questionCountByLevel.getOrDefault(level.getId(), 0) >= 1;
            boolean prerequisitePassed = nodeLevelAttemptService.isNextLevelUnlocked(level.getId(), studentId, classroomId);
            boolean reviewCompleted = !studentLevelReviewService.requiresReview(level)
                    || studentLevelReviewService.isReviewed(level.getId(), studentId, classroomId);
            boolean unlocked = prerequisitePassed && reviewCompleted;

            boolean hasAttemptsLeft = true;
            if (level.getMaxAttempts() != null) {
                long currentCount = nodeLevelAttemptRepository
                        .countByNodeLevel_IdAndStudent_IdAndClassroom_Id(level.getId(), studentId, classroomId);
                int extraAttempts = attemptGrantRepository
                        .sumExtraAttempts(level.getId(), studentId, classroomId);
                hasAttemptsLeft = currentCount < level.getMaxAttempts() + extraAttempts;
            }

            result.add(new StudentNodeLevelStatusView(
                    level.getId(), level.getLevelNumber(), level.getTitle(),
                    level.getMaxScore(), level.getPassingScore(),
                    hasQuestions, unlocked, prerequisitePassed, reviewCompleted, hasAttemptsLeft));
        }
        return result;
    }

    private List<StudentLevelMaterialView> buildLevelMaterials(
            Integer nodeId, Integer studentId, Integer classroomId) {

        List<NodeLevel> levels = nodeLevelRepository.findByLearningNode_IdOrderByLevelNumberAsc(nodeId);
        List<StudentLevelMaterialView> result = new ArrayList<>();
        for (NodeLevel level : levels) {
            if (level.getLevelNumber() == 1) continue;
            boolean available = nodeLevelAttemptService
                    .isNextLevelUnlocked(level.getId(), studentId, classroomId);
            boolean reviewed = studentLevelReviewService
                    .isReviewed(level.getId(), studentId, classroomId);
            Set<Integer> includedMaterialIds = new HashSet<>();

            for (LevelMaterial levelMaterial : levelMaterialRepository.findByNodeLevel_Id(level.getId())) {
                Integer materialId = levelMaterial.getMaterial().getId();
                // A question pool may be reused by multiple levels. Present its
                // learning content once, owned by the lowest configured level.
                if (!includedMaterialIds.add(materialId)) continue;
                result.add(new StudentLevelMaterialView(
                        level.getId(),
                        level.getLevelNumber(),
                        level.getTitle(),
                        levelMaterial.getMaterial(),
                        available
                                ? nodeContentService.getLearningContentsByMaterialId(materialId)
                                : List.of(),
                        available,
                        reviewed));
            }
        }
        return result;
    }
}
