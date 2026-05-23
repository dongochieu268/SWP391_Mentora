package com.edunac.mentora.service.subject;

import com.edunac.mentora.dto.SubjectForm;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.domain.subject.SubjectStatus;
import com.edunac.mentora.repository.subject.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public Subject findById(Integer id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học"));
    }

    public void saveForm(SubjectForm form) {
        Subject subject = new Subject();
        subject.setCode(form.getCode());
        subject.setName(form.getName());
        subject.setDescription(form.getDescription());
        subject.setStatus(form.getStatus());

        if (form.getId() == null) {
            create(subject);
        } else {
            update(form.getId(), subject);
        }
    }

    public SubjectForm toForm(Subject subject) {
        SubjectForm form = new SubjectForm();
        form.setId(subject.getId());
        form.setCode(subject.getCode());
        form.setName(subject.getName());
        form.setDescription(subject.getDescription());
        form.setStatus(subject.getStatus());
        return form;
    }

    public Subject create(Subject subject) {
        validateCodeUnique(subject.getCode(), null);
        subject.setId(null);
        subject.setCode(subject.getCode().trim());
        subject.setName(subject.getName().trim());
        if (subject.getDescription() != null && subject.getDescription().isBlank()) {
            subject.setDescription(null);
        }
        if (subject.getStatus() == null || subject.getStatus().isBlank()) {
            subject.setStatus(SubjectStatus.ACTIVE.name());
        }
        validateStatus(normalizeStatus(subject.getStatus()));
        subject.setStatus(normalizeStatus(subject.getStatus()));
        return subjectRepository.save(subject);
    }

    public Subject update(Integer id, Subject updated) {
        Subject existing = findById(id);
        validateCodeUnique(updated.getCode(), id);

        existing.setCode(updated.getCode().trim());
        existing.setName(updated.getName().trim());
        existing.setDescription(updated.getDescription());

        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            String status = normalizeStatus(updated.getStatus());
            validateStatus(status);
            existing.setStatus(status);
        }

        return subjectRepository.save(existing);
    }

    public Subject publish(Integer id) {
        Subject subject = findById(id);
        subject.setStatus(SubjectStatus.ACTIVE.name());
        return subjectRepository.save(subject);
    }

    public Subject unpublish(Integer id) {
        Subject subject = findById(id);
        subject.setStatus(SubjectStatus.HIDDEN.name());
        return subjectRepository.save(subject);
    }

    public void delete(Integer id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học");
        }
        subjectRepository.deleteById(id);
    }

    private void validateCodeUnique(String code, Integer excludeId) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã môn không được để trống");
        }
        boolean exists = excludeId == null
                ? subjectRepository.existsByCode(code.trim())
                : subjectRepository.existsByCodeAndIdNot(code.trim(), excludeId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã môn đã tồn tại");
        }
    }

    private void validateStatus(String status) {
        try {
            SubjectStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phải là ACTIVE hoặc HIDDEN");
        }
    }

    private String normalizeStatus(String status) {
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return SubjectStatus.HIDDEN.name();
        }
        return status;
    }
}
