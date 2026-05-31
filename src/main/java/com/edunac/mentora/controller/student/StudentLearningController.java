package com.edunac.mentora.controller.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.NodeProgressResponse;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.service.learning.NodeProgressService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/student/classrooms")
@RequiredArgsConstructor
public class StudentLearningController {

    private final NodeProgressService nodeProgressService;
    private final LearningNodeRepository learningNodeRepository;

    @GetMapping("/{classroomId}/nodes/{nodeId}")
    public String viewNodeDetail(
            @PathVariable Integer classroomId,
            @PathVariable Integer nodeId,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        LearningNode node = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

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
        Integer nextNodeId = currentIndex < allNodes.size() - 1 ? allNodes.get(currentIndex + 1).getId() : null;

        model.addAttribute("user", currentUser);
        model.addAttribute("activePage", "classrooms"); // Giữ sáng menu Lớp học ở Sidebar
        model.addAttribute("node", node);
        model.addAttribute("progress", progress);
        model.addAttribute("classroomId", classroomId);
        model.addAttribute("prevNodeId", prevNodeId);
        model.addAttribute("nextNodeId", nextNodeId);

        return "student/learning/node-detail";
    }

    @PostMapping("/{classroomId}/nodes/{nodeId}/complete")
    @ResponseBody
    public ResponseEntity<?> completeNode(
            @PathVariable Integer classroomId,
            @PathVariable Integer nodeId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
        }

        NodeProgressResponse result = nodeProgressService
                .markNodeCompleted(currentUser.getId(), nodeId, classroomId);

        return ResponseEntity.ok(result);
    }
}