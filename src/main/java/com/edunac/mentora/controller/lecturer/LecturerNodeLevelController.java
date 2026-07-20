package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.level.Material;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.dto.LevelMaterialForm;
import com.edunac.mentora.dto.NodeLevelForm;
import com.edunac.mentora.service.learning.LearningNodeService;
import com.edunac.mentora.service.learning.NodeLevelService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Quản lý cấu hình level cho giảng viên.
 * Route: /lecturer/nodes/{nodeId}/levels
 */
@Controller
@RequestMapping("/lecturer/nodes/{nodeId}/levels")
public class LecturerNodeLevelController {

    private final LearningNodeService learningNodeService;
    private final NodeLevelService nodeLevelService;

    public LecturerNodeLevelController(LearningNodeService learningNodeService,
                                       NodeLevelService nodeLevelService) {
        this.learningNodeService = learningNodeService;
        this.nodeLevelService = nodeLevelService;
    }

    // ===== GET: trang chính =====

    @GetMapping
    public String levelsPage(
            @PathVariable Integer nodeId,
            @RequestParam(required = false) Boolean addLevel,
            @RequestParam(required = false) Integer editLevel,
            @RequestParam(required = false) Integer addMaterial,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User user = currentUser(session);
        LearningNode node = learningNodeService.findByIdForOwner(nodeId, user);

        List<NodeLevel> levels = nodeLevelService.findByNode(nodeId);
        List<Material> availableMaterials = nodeLevelService.availableMaterials(nodeId);
        Map<Integer, Long> bankCounts = nodeLevelService.bankQuestionCounts(availableMaterials);

        // Tổng câu hỏi per level (sum of levelMaterial.questionCount)
        Map<Integer, Integer> totalQuestionsByLevel = new java.util.HashMap<>();
        Map<Integer, Boolean> lastMaterialWithAttempts = new java.util.HashMap<>();
        for (NodeLevel lvl : levels) {
            int total = lvl.getLevelMaterials().stream()
                    .mapToInt(com.edunac.mentora.domain.level.LevelMaterial::getQuestionCount)
                    .sum();
            totalQuestionsByLevel.put(lvl.getId(), total);
            lastMaterialWithAttempts.put(
                    lvl.getId(),
                    lvl.getLevelMaterials().size() == 1
                            && nodeLevelService.levelHasAttempts(lvl.getId()));
        }

        populateCommon(session, model, node, returnTo);
        model.addAttribute("levels", levels);
        model.addAttribute("availableMaterials", availableMaterials);
        model.addAttribute("bankCounts", bankCounts);
        model.addAttribute("totalQuestionsByLevel", totalQuestionsByLevel);
        model.addAttribute("lastMaterialWithAttempts", lastMaterialWithAttempts);

        // Modal trạng thái mặc định
        model.addAttribute("openLevelModal", false);
        model.addAttribute("openMaterialModal", false);
        model.addAttribute("materialModalLevelId", null);

        if (!model.containsAttribute("levelForm")) {
            if (editLevel != null) {
                try {
                    NodeLevel lvl = nodeLevelService.findByIdForOwner(editLevel, user);
                    model.addAttribute("levelForm", nodeLevelService.toForm(lvl));
                    model.addAttribute("openLevelModal", true);
                } catch (IllegalArgumentException e) {
                    ra.addFlashAttribute("errorMessage", e.getMessage());
                    return "redirect:/lecturer/nodes/" + nodeId + "/levels" + returnToQuery(returnTo);
                }
            } else if (Boolean.TRUE.equals(addLevel)) {
                NodeLevelForm form = new NodeLevelForm();
                form.setNodeId(nodeId);
                model.addAttribute("levelForm", form);
                model.addAttribute("openLevelModal", true);
            } else {
                NodeLevelForm form = new NodeLevelForm();
                form.setNodeId(nodeId);
                model.addAttribute("levelForm", form);
            }
        }

        if (!model.containsAttribute("materialForm")) {
            if (addMaterial != null) {
                LevelMaterialForm form = new LevelMaterialForm();
                form.setLevelId(addMaterial);
                model.addAttribute("materialForm", form);
                model.addAttribute("openMaterialModal", true);
                model.addAttribute("materialModalLevelId", addMaterial);
            } else {
                model.addAttribute("materialForm", new LevelMaterialForm());
            }
        }

        return "lecturer/learning/node-levels";
    }

    // ===== POST: lưu level (tạo mới hoặc cập nhật) =====

    @PostMapping("/save")
    public String saveLevel(
            @PathVariable Integer nodeId,
            @ModelAttribute NodeLevelForm form,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes ra) {

        User user = currentUser(session);
        form.setNodeId(nodeId);
        try {
            if (form.getId() == null) {
                nodeLevelService.createLevel(nodeId, form, user);
                ra.addFlashAttribute("successMessage", "Đã thêm level mới.");
            } else {
                nodeLevelService.updateLevel(nodeId, form.getId(), form, user);
                ra.addFlashAttribute("successMessage", "Đã cập nhật level.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("levelForm", form);
            ra.addFlashAttribute("openLevelModal", true);
        }
        return "redirect:/lecturer/nodes/" + nodeId + "/levels" + returnToQuery(returnTo);
    }

    // ===== POST: xóa level =====

    @PostMapping("/{levelId}/delete")
    public String deleteLevel(
            @PathVariable Integer nodeId,
            @PathVariable Integer levelId,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes ra) {

        User user = currentUser(session);
        try {
            nodeLevelService.deleteLevel(nodeId, levelId, user);
            ra.addFlashAttribute("successMessage", "Đã xóa level.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lecturer/nodes/" + nodeId + "/levels" + returnToQuery(returnTo);
    }

    // ===== POST: thêm material vào level =====

    @PostMapping("/{levelId}/materials")
    public String addMaterial(
            @PathVariable Integer nodeId,
            @PathVariable Integer levelId,
            @ModelAttribute LevelMaterialForm form,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes ra) {

        User user = currentUser(session);
        form.setLevelId(levelId);
        try {
            nodeLevelService.addMaterial(nodeId, levelId, form, user);
            ra.addFlashAttribute("successMessage", "Đã thêm tài liệu vào level.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("materialForm", form);
            ra.addFlashAttribute("openMaterialModal", true);
            ra.addFlashAttribute("materialModalLevelId", levelId);
        }
        return "redirect:/lecturer/nodes/" + nodeId + "/levels" + returnToQuery(returnTo);
    }

    // ===== POST: gỡ material khỏi level =====

    @PostMapping("/{levelId}/materials/{levelMaterialId}/remove")
    public String removeMaterial(
            @PathVariable Integer nodeId,
            @PathVariable Integer levelId,
            @PathVariable Integer levelMaterialId,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes ra) {

        User user = currentUser(session);
        try {
            nodeLevelService.removeMaterial(nodeId, levelId, levelMaterialId, user);
            ra.addFlashAttribute("successMessage", "Đã gỡ tài liệu khỏi level.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lecturer/nodes/" + nodeId + "/levels" + returnToQuery(returnTo);
    }

    // ===== HELPERS =====

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    private void populateCommon(HttpSession session, Model model, LearningNode node, String returnTo) {
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "learning-paths");
        model.addAttribute("node", node);
        model.addAttribute("pageTitle", "Cấu hình level — " + node.getTitle());
        String defaultBackUrl = node.getLearningPath() != null
                ? "/lecturer/learning-paths/" + node.getLearningPath().getId()
                : "/lecturer/learning-paths";
        boolean fromWizard = returnTo != null && returnTo.startsWith("/lecturer/");
        model.addAttribute("pathBackUrl", fromWizard ? returnTo : defaultBackUrl);
        model.addAttribute("returnTo", fromWizard ? returnTo : null);
        model.addAttribute("fromWizard", fromWizard);
    }

    /** Chỉ chấp nhận returnTo nội bộ khu lecturer (chống open redirect). */
    private String returnToQuery(String returnTo) {
        if (returnTo == null || !returnTo.startsWith("/lecturer/")) {
            return "";
        }
        return "?returnTo=" + java.net.URLEncoder.encode(returnTo, java.nio.charset.StandardCharsets.UTF_8);
    }
}
