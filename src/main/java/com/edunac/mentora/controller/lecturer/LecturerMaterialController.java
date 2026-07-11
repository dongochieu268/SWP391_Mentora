package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.BankQuestion;
import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.domain.level.Material;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.dto.MaterialForm;
import com.edunac.mentora.dto.NodeContentForm;
import com.edunac.mentora.service.assessment.QuestionBankService;
import com.edunac.mentora.service.learning.NodeContentService;
import com.edunac.mentora.service.level.MaterialService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/lecturer/materials")
public class LecturerMaterialController {

    private final MaterialService materialService;
    private final QuestionBankService questionBankService;
    private final SubjectService subjectService;
    private final NodeContentService nodeContentService;

    public LecturerMaterialController(MaterialService materialService,
                                      QuestionBankService questionBankService,
                                      SubjectService subjectService,
                                      NodeContentService nodeContentService) {
        this.materialService = materialService;
        this.questionBankService = questionBankService;
        this.subjectService = subjectService;
        this.nodeContentService = nodeContentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer subjectId,
                       @RequestParam(required = false) Integer materialId,
                       @RequestParam(required = false) Integer contentEditId,
                       HttpSession session,
                       Model model) {
        User user = currentUser(session);
        List<Subject> subjects = subjectService.getActiveSubjects();
        Integer selectedSubjectId = selectedSubjectId(subjectId, subjects);
        List<Material> materials = materialService.findBySubject(selectedSubjectId);
        Material selectedMaterial = selectMaterial(materials, materialId);
        List<BankQuestion> materialQuestions = selectedMaterial == null
                ? List.of()
                : materialService.findQuestionsByMaterial(selectedMaterial.getId(), user);
        List<NodeContent> materialContents = selectedMaterial == null
                ? List.of()
                : nodeContentService.getByMaterialId(selectedMaterial.getId(), user);
        List<BankQuestion> bankQuestions = questionBankService.findActiveBySubject(
                selectedSubjectId, null, null, null);

        if (!model.containsAttribute("form")) {
            MaterialForm form = selectedMaterial == null ? new MaterialForm() : toForm(selectedMaterial);
            form.setSubjectId(selectedSubjectId);
            model.addAttribute("form", form);
        }
        if (!model.containsAttribute("contentForm")) {
            NodeContentForm contentForm = new NodeContentForm();
            if (selectedMaterial != null) {
                if (contentEditId != null) {
                    contentForm = nodeContentService.toForm(
                            nodeContentService.findByIdAndMaterialId(contentEditId, selectedMaterial.getId(), user));
                } else {
                    contentForm.setMaterialId(selectedMaterial.getId());
                }
            }
            model.addAttribute("contentForm", contentForm);
        }

        model.addAttribute("user", user);
        model.addAttribute("activePage", "materials");
        model.addAttribute("activeSubjects", subjects);
        model.addAttribute("selectedSubjectId", selectedSubjectId);
        model.addAttribute("materials", materials);
        model.addAttribute("selectedMaterial", selectedMaterial);
        model.addAttribute("materialContents", materialContents);
        model.addAttribute("materialQuestions", materialQuestions);
        model.addAttribute("bankQuestions", bankQuestions);
        model.addAttribute("assignedQuestionIds", materialQuestions.stream().map(BankQuestion::getId).toList());
        return "lecturer/material/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") MaterialForm form,
                         BindingResult bindingResult,
                         HttpSession session,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", firstValidationError(bindingResult));
            ra.addFlashAttribute("form", form);
            return redirectToSubject(form.getSubjectId());
        }
        try {
            materialService.create(form.getSubjectId(), form.getTitle(), form.getDescription(), currentUser(session));
            ra.addFlashAttribute("success", "Đã tạo material.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
        }
        return redirectToSubject(form.getSubjectId());
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("form") MaterialForm form,
                         BindingResult bindingResult,
                         HttpSession session,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", firstValidationError(bindingResult));
            ra.addFlashAttribute("form", form);
            return redirectToMaterial(form.getSubjectId(), id);
        }
        try {
            materialService.update(id, form.getTitle(), form.getDescription(), currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật material.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
        }
        return redirectToMaterial(form.getSubjectId(), id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id,
                         @RequestParam Integer subjectId,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            materialService.delete(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa material.");
            return redirectToSubject(subjectId);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return redirectToMaterial(subjectId, id);
        }
    }

    @PostMapping("/{id}/questions/assign")
    public String assignQuestions(@PathVariable Integer id,
                                  @RequestParam Integer subjectId,
                                  @RequestParam(name = "questionIds", required = false) List<Integer> questionIds,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        try {
            int count = materialService.assignQuestions(id, questionIds, currentUser(session));
            ra.addFlashAttribute("success", "Đã gắn " + count + " câu hỏi vào material.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectToMaterial(subjectId, id);
    }

    @PostMapping("/{id}/contents/save")
    public String saveContent(@PathVariable Integer id,
                              @RequestParam Integer subjectId,
                              @Valid @ModelAttribute("contentForm") NodeContentForm form,
                              BindingResult bindingResult,
                              @RequestParam(value = "mediaFile", required = false) MultipartFile mediaFile,
                              HttpSession session,
                              RedirectAttributes ra) {
        form.setMaterialId(id);
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", firstValidationError(bindingResult));
            ra.addFlashAttribute("contentForm", form);
            return redirectToMaterial(subjectId, id);
        }
        try {
            nodeContentService.saveMaterialForm(form, mediaFile, currentUser(session));
            ra.addFlashAttribute("success", form.getId() == null ? "Đã thêm nội dung." : "Đã cập nhật nội dung.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("contentForm", form);
        }
        return redirectToMaterial(subjectId, id);
    }

    @PostMapping("/{id}/contents/delete/{contentId}")
    public String deleteContent(@PathVariable Integer id,
                                @PathVariable Integer contentId,
                                @RequestParam Integer subjectId,
                                HttpSession session,
                                RedirectAttributes ra) {
        try {
            nodeContentService.deleteMaterialContent(contentId, id, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa nội dung.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectToMaterial(subjectId, id);
    }

    @PostMapping("/{id}/questions/unassign")
    public String unassignQuestions(@PathVariable Integer id,
                                    @RequestParam Integer subjectId,
                                    @RequestParam(name = "questionIds", required = false) List<Integer> questionIds,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        try {
            int count = materialService.unassignQuestions(id, questionIds, currentUser(session));
            ra.addFlashAttribute("success", "Đã gỡ " + count + " câu hỏi khỏi material.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectToMaterial(subjectId, id);
    }

    private Integer selectedSubjectId(Integer requestedSubjectId, List<Subject> subjects) {
        if (requestedSubjectId != null) {
            return requestedSubjectId;
        }
        return subjects.stream().findFirst().map(Subject::getId).orElse(null);
    }

    private Material selectMaterial(List<Material> materials, Integer requestedMaterialId) {
        if (materials == null || materials.isEmpty()) {
            return null;
        }
        if (requestedMaterialId != null) {
            return materials.stream()
                    .filter(material -> requestedMaterialId.equals(material.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return materials.get(0);
    }

    private MaterialForm toForm(Material material) {
        MaterialForm form = new MaterialForm();
        form.setId(material.getId());
        form.setSubjectId(material.getSubject() != null ? material.getSubject().getId() : null);
        form.setTitle(material.getTitle());
        form.setDescription(material.getDescription());
        return form;
    }

    private String firstValidationError(BindingResult bindingResult) {
        if (!bindingResult.getFieldErrors().isEmpty()) {
            return bindingResult.getFieldErrors().get(0).getDefaultMessage();
        }
        return "Dữ liệu không hợp lệ.";
    }

    private String redirectToSubject(Integer subjectId) {
        return "redirect:/lecturer/materials?subjectId=" + (subjectId != null ? subjectId : "");
    }

    private String redirectToMaterial(Integer subjectId, Integer materialId) {
        return redirectToSubject(subjectId) + "&materialId=" + materialId;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
