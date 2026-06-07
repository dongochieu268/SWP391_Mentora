package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.Assessment;
import com.edunac.mentora.domain.branching.BranchRule;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.dto.LearningNodeForm;
import com.edunac.mentora.dto.LearningPathForm;
import com.edunac.mentora.service.assessment.AssessmentService;
import com.edunac.mentora.service.branching.BranchRuleService;
import com.edunac.mentora.service.learningpath.LearningPathService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/lecturer/learning-paths")
public class LecturerLearningPathController {

    private final LearningPathService pathService;
    private final SubjectService subjectService;
    private final AssessmentService assessmentService;
    private final BranchRuleService branchRuleService;

    public LecturerLearningPathController(LearningPathService pathService,
                                          SubjectService subjectService,
                                          AssessmentService assessmentService,
                                          BranchRuleService branchRuleService) {
        this.pathService = pathService;
        this.subjectService = subjectService;
        this.assessmentService = assessmentService;
        this.branchRuleService = branchRuleService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        List<LearningPath> paths = pathService.findByCreator(user);
        Set<Integer> withClassroom = paths.stream()
                .filter(p -> pathService.hasClassroom(p.getId()))
                .map(LearningPath::getId)
                .collect(Collectors.toSet());
        model.addAttribute("paths", paths);
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
    public String detail(@PathVariable Integer id,
                         @RequestParam(required = false) Integer addAfter,
                         @RequestParam(required = false) Boolean addEnd,
                         @RequestParam(required = false) Integer editNode,
                         HttpSession session, Model model) {
        User user = currentUser(session);
        LearningPath path = pathService.findByIdAndOwner(id, user);
        List<LearningNode> nodes = pathService.getNodes(id);

        model.addAttribute("path", path);
        model.addAttribute("nodes", nodes);
        model.addAttribute("hasClassroom", pathService.hasClassroom(id));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");


        List<LearningNode> branchTestNodes = nodes.stream()
                .filter(n -> "BRANCH_TEST".equals(n.getNodeType()))
                .collect(Collectors.toList());
        model.addAttribute("branchTestNodes", branchTestNodes);

        List<Assessment> publishedAssessments = assessmentService.findPublishedByCreator(user);
        model.addAttribute("assessments", publishedAssessments);

        if (!model.containsAttribute("nodeForm")) {
            if (editNode != null) {
                buildEditForm(editNode, nodes, model);
            } else if (addAfter != null || Boolean.TRUE.equals(addEnd)) {
                LearningNodeForm f = new LearningNodeForm();
                f.setAfterNodeId(addAfter);
                model.addAttribute("nodeForm", f);
                model.addAttribute("editMode", false);
                model.addAttribute("openNodeModal", true);
            }
        }

        if (!model.containsAttribute("nodeForm")) {
            model.addAttribute("nodeForm", new LearningNodeForm());
        }
        if (!model.containsAttribute("editMode")) {
            model.addAttribute("editMode", false);
        }
        if (!model.containsAttribute("openNodeModal")) {
            model.addAttribute("openNodeModal", false);
        }

        return "lecturer/learning-path/detail";
    }


    private void buildEditForm(Integer editNodeId, List<LearningNode> nodes, Model model) {
        for (LearningNode n : nodes) {
            if (!n.getId().equals(editNodeId)) continue;

            LearningNodeForm f = new LearningNodeForm();
            f.setId(n.getId());
            f.setTitle(n.getTitle());
            f.setDescription(n.getDescription());
            if (n.getPrerequisite() != null) {
                f.setPrerequisiteNodeId(n.getPrerequisite().getId());
            }

            f.setNodeType(n.getNodeType() != null ? n.getNodeType() : "LESSON");
            f.setBranchTag(n.getBranchTag() != null ? n.getBranchTag() : "MAIN");
            if (n.getBranchOwnerNode() != null) {
                f.setBranchOwnerNodeId(n.getBranchOwnerNode().getId());
            }

            if ("BRANCH_TEST".equals(f.getNodeType())) {
                branchRuleService.findByNodeId(n.getId()).ifPresent(rule -> {
                    f.setAssessmentId(rule.getAssessmentId());
                    f.setMinScore(rule.getMinScore() != null ? rule.getMinScore().doubleValue() : null);
                });
            }

            model.addAttribute("nodeForm", f);
            model.addAttribute("editMode", true);
            model.addAttribute("openNodeModal", true);
            return;
        }
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
            pathService.addNode(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã thêm node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id;
    }

    @PostMapping("/{id}/nodes/{nodeId}/edit")
    public String updateNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                             @ModelAttribute LearningNodeForm form,
                             HttpSession session, RedirectAttributes ra) {
        try {
            pathService.updateNode(id, nodeId, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id;
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

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            pathService.archive(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã lưu trữ lộ trình.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths";
    }

    @PostMapping("/{id}/unarchive")
    public String unarchive(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            pathService.unarchive(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã khôi phục lộ trình.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths";
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