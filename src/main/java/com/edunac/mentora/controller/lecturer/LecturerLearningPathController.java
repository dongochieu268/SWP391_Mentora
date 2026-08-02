package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.dto.LearningNodeForm;
import com.edunac.mentora.dto.LearningPathForm;
import com.edunac.mentora.service.learningpath.LearningPathService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/lecturer/learning-paths")
public class LecturerLearningPathController {

    private final LearningPathService pathService;
    private final SubjectService subjectService;

    public LecturerLearningPathController(LearningPathService pathService,
                                          SubjectService subjectService) {
        this.pathService = pathService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        List<LearningPath> paths = pathService.findByCreator(user);

        //NHững lộ trình gắn với lớp học rồi
        Set<Integer> withClassroom = new java.util.HashSet<>();
        for (LearningPath p : paths) {
            if (pathService.hasClassroom(p.getId())) {
                withClassroom.add(p.getId());
            }
        }

        //sắp xếp theo mã môn
        List<LearningPath> sortedPaths = new java.util.ArrayList<>(paths);
        for (int i = 0; i < sortedPaths.size() - 1; i++) {
            for (int j = 0; j < sortedPaths.size() - 1 - i; j++) {
                String codeA = sortedPaths.get(j).getSubject().getCode();
                String codeB = sortedPaths.get(j + 1).getSubject().getCode();
                if (codeA.compareTo(codeB) > 0) {
                    LearningPath tmp = sortedPaths.get(j);
                    sortedPaths.set(j, sortedPaths.get(j + 1));
                    sortedPaths.set(j + 1, tmp);
                }
            }
        }
        
        //gom theo mã môn
        java.util.Map<com.edunac.mentora.domain.subject.Subject, List<LearningPath>> pathsBySubject =
                new java.util.LinkedHashMap<>();
        for (LearningPath p : sortedPaths) {
            com.edunac.mentora.domain.subject.Subject subject = p.getSubject();
            List<LearningPath> group = pathsBySubject.get(subject);
            if (group == null) {
                group = new java.util.ArrayList<>();
                pathsBySubject.put(subject, group);
            }
            group.add(p);
        }

        model.addAttribute("paths", paths);
        model.addAttribute("pathsBySubject", pathsBySubject);
        model.addAttribute("pathsWithClassroom", withClassroom);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");
        return "lecturer/learning-path/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        User user = currentUser(session);
        model.addAttribute("form", new LearningPathForm());
        model.addAttribute("activeSubjects", subjectService.getActiveSubjects());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");
        return "lecturer/learning-path/form";
    }

    @PostMapping
    public String create(HttpSession session,
                         @ModelAttribute LearningPathForm form,
                         RedirectAttributes ra) {
        User user = currentUser(session);
        try {
            LearningPath path = pathService.create(form.getSubjectId(), form.getName(), form.getDescription(), user);
            ra.addFlashAttribute("success", "Đã tạo lộ trình thành công.");
            return "redirect:/lecturer/learning-paths/" + path.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/learning-paths/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        User user = currentUser(session);
        LearningPath path = pathService.findByIdAndOwner(id, user);
        List<LearningNode> nodes = pathService.getNodes(id);

        model.addAttribute("path", path);
        model.addAttribute("nodes", nodes);
        model.addAttribute("hasClassroom", pathService.hasClassroom(id));
        model.addAttribute("contentCounts", pathService.contentCounts(id));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");

        return "lecturer/learning-path/detail";
    }

    @GetMapping("/{id}/nodes/new")
    public String newNodeForm(@PathVariable Integer id, HttpSession session, Model model) {
        User user = currentUser(session);
        LearningPath path = pathService.findByIdAndOwner(id, user);

        model.addAttribute("path", path);
        model.addAttribute("nodes", pathService.getNodes(id));
        model.addAttribute("isEdit", false);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new LearningNodeForm());
        }
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");
        return "lecturer/learning-path/node-form";
    }

    @GetMapping("/{id}/nodes/{nodeId}/edit")
    public String editNodeForm(@PathVariable Integer id, @PathVariable Integer nodeId,
                               HttpSession session, Model model) {
        User user = currentUser(session);
        LearningPath path = pathService.findByIdAndOwner(id, user);
        List<LearningNode> nodes = pathService.getNodes(id);
        LearningNode node = nodes.stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy node."));

        if (!model.containsAttribute("form")) {
            LearningNodeForm form = new LearningNodeForm();
            form.setId(node.getId());
            form.setTitle(node.getTitle());
            form.setDescription(node.getDescription());
            if (node.getPrerequisite() != null) {
                form.setPrerequisiteNodeId(node.getPrerequisite().getId());
            }
            model.addAttribute("form", form);
        }
        model.addAttribute("path", path);
        model.addAttribute("nodes", nodes);
        model.addAttribute("nodeId", nodeId);
        model.addAttribute("isEdit", true);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");
        return "lecturer/learning-path/node-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                         @ModelAttribute LearningPathForm form,
                         HttpSession session, RedirectAttributes ra) {
        try {
            pathService.update(id, form.getName(), form.getDescription(), currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật lộ trình.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            pathService.delete(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa lộ trình.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths";
    }

    @PostMapping("/{id}/nodes")
    public String addNode(@PathVariable Integer id,
                          @ModelAttribute LearningNodeForm form,
                          HttpSession session, RedirectAttributes ra) {
        try {
            LearningNode saved = pathService.addNode(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã thêm node.");
            return "redirect:/lecturer/learning-paths/" + id + "#node-" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
            return "redirect:/lecturer/learning-paths/" + id + "/nodes/new";
        }
    }

    @PostMapping("/{id}/nodes/{nodeId}/edit")
    public String updateNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                             @ModelAttribute LearningNodeForm form,
                             HttpSession session, RedirectAttributes ra) {
        try {
            pathService.updateNode(id, nodeId, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật node.");
            return "redirect:/lecturer/learning-paths/" + id + "#node-" + nodeId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
            return "redirect:/lecturer/learning-paths/" + id + "/nodes/" + nodeId + "/edit";
        }
    }

    @PostMapping("/{id}/nodes/{nodeId}/move")
    public String moveNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                           @RequestParam String dir,
                           HttpSession session, RedirectAttributes ra) {
        try {
            pathService.moveNode(id, nodeId, dir, currentUser(session));
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id + "#node-" + nodeId;
    }

    @PostMapping("/{id}/nodes/{nodeId}/delete")
    public String deleteNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                             HttpSession session, RedirectAttributes ra) {
        try {
            pathService.deleteNode(id, nodeId, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id;
    }

    @PostMapping("/{id}/clone")
    public String clone(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            LearningPath cloned = pathService.clonePath(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã clone lộ trình thành công.");
            return "redirect:/lecturer/learning-paths/" + cloned.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/learning-paths";
        }
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
