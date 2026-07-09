package com.edunac.mentora.controller.student;

import com.edunac.mentora.domain.branching.BranchRule;
import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.NodeProgressResponse;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.service.branching.BranchRuleService;
import com.edunac.mentora.service.learning.NodeContentService;
import com.edunac.mentora.service.learning.NodeProgressService;
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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentLearningController {

    private final NodeProgressService nodeProgressService;
    private final NodeContentService nodeContentService;
    private final LearningNodeRepository learningNodeRepository;
    private final StudentRoadmapService roadmapService;
    private final BranchRuleService branchRuleService;

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

        model.addAttribute("user", currentUser);
        model.addAttribute("activePage", "classrooms");
        model.addAttribute("node", node);
        model.addAttribute("contents", contents);
        model.addAttribute("progress", progress);
        model.addAttribute("classroomId", classroomId);
        model.addAttribute("prevNodeId", prevNodeId);
        model.addAttribute("nextNodeId", nextNodeId);

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
}