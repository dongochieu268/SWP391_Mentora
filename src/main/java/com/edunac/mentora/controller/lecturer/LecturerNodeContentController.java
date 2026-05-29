package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.NodeContentForm;
import com.edunac.mentora.service.learning.LearningNodeService;
import com.edunac.mentora.service.learning.NodeContentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lecturer")
public class LecturerNodeContentController {

    private final LearningNodeService learningNodeService;
    private final NodeContentService nodeContentService;

    public LecturerNodeContentController(
            LearningNodeService learningNodeService,
            NodeContentService nodeContentService
    ) {
        this.learningNodeService = learningNodeService;
        this.nodeContentService = nodeContentService;
    }

    @GetMapping("/nodes/{nodeId}/contents")
    public String contentsPage(
            @PathVariable Integer nodeId,
            @RequestParam(required = false) Integer editId,
            @RequestParam(required = false) Boolean create,
            HttpSession session,
            Model model
    ) {
        LearningNode node = learningNodeService.findById(nodeId);
        String baseUrl = "/lecturer/nodes/" + nodeId + "/contents";
        populateCommon(session, model, "node-contents");
        model.addAttribute("pageTitle", "Nội dung node");
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("node", node);
        model.addAttribute("contents", nodeContentService.getByNodeId(nodeId));

        if (!model.containsAttribute("openModal")) {
            if (editId != null) {
                model.addAttribute("contentForm",
                        nodeContentService.toForm(nodeContentService.findByIdAndNodeId(editId, nodeId)));
                model.addAttribute("openModal", true);
            } else if (Boolean.TRUE.equals(create)) {
                NodeContentForm form = new NodeContentForm();
                form.setNodeId(nodeId);
                model.addAttribute("contentForm", form);
                model.addAttribute("openModal", true);
            }
        }

        if (!model.containsAttribute("contentForm")) {
            NodeContentForm form = new NodeContentForm();
            form.setNodeId(nodeId);
            model.addAttribute("contentForm", form);
        }

        return "lecturer/learning/node-contents";
    }

    @PostMapping("/nodes/{nodeId}/contents/save")
    public String saveContent(
            @PathVariable Integer nodeId,
            @Valid @ModelAttribute("contentForm") NodeContentForm form,
            BindingResult bindingResult,
            @RequestParam(value = "mediaFile", required = false) MultipartFile mediaFile,
            RedirectAttributes redirectAttributes
    ) {
        form.setNodeId(nodeId);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstValidationError(bindingResult));
            return "redirect:/lecturer/nodes/" + nodeId + "/contents";
        }

        try {
            nodeContentService.saveForm(form, mediaFile);
            redirectAttributes.addFlashAttribute("successMessage",
                    form.getId() == null ? "Đã thêm nội dung!" : "Đã cập nhật nội dung!");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() != null ? ex.getReason() : "Không thể lưu nội dung.");
        }

        return "redirect:/lecturer/nodes/" + nodeId + "/contents";
    }

    @PostMapping("/nodes/{nodeId}/contents/delete/{contentId}")
    public String deleteContent(
            @PathVariable Integer nodeId,
            @PathVariable Integer contentId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            nodeContentService.delete(contentId, nodeId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa nội dung.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }
        return "redirect:/lecturer/nodes/" + nodeId + "/contents";
    }

    private String firstValidationError(BindingResult bindingResult) {
        if (!bindingResult.getFieldErrors().isEmpty()) {
            return bindingResult.getFieldErrors().get(0).getDefaultMessage();
        }
        return "Dữ liệu không hợp lệ.";
    }

    private void populateCommon(HttpSession session, Model model, String activePage) {
        model.addAttribute("activePage", activePage);
        model.addAttribute("user", session.getAttribute("loggedInUser"));
    }
}
