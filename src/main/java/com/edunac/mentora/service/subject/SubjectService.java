package com.edunac.mentora.service.subject;

import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.domain.subject.SubjectPrerequisite;
import com.edunac.mentora.domain.subject.SubjectPrerequisiteKey;
import com.edunac.mentora.domain.subject.SubjectStatus;
import com.edunac.mentora.dto.SubjectForm;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import com.edunac.mentora.repository.subject.SubjectPrerequisiteRepository;
import com.edunac.mentora.repository.subject.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectPrerequisiteRepository prerequisiteRepository;
    private final ClassroomRepository classroomRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          SubjectPrerequisiteRepository prerequisiteRepository,
                          ClassroomRepository classroomRepository) {
        this.subjectRepository = subjectRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.classroomRepository = classroomRepository;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public List<Subject> getActiveSubjects() {
        return subjectRepository.findByStatusOrderByNameAsc("ACTIVE");
    }

    public Subject findById(Integer id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy môn học"));
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

    @Transactional
    public void delete(Integer id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học");
        }
        if (prerequisiteRepository.existsByPrerequisiteSubjectId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa: Môn này đang là tiên quyết của môn khác!");
        }
        if (!classroomRepository.findByLearningPath_SubjectId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa: Môn học đang có lớp học sử dụng!");
        }
        prerequisiteRepository.deleteBySubjectId(id);
        subjectRepository.deleteById(id);
    }

    public List<Subject> getAvailablePrerequisites(Integer mainSubjectId) {
        List<Subject> all = subjectRepository.findAll();

        Set<Integer> alreadyPrereqIds = new HashSet<>();
        for (SubjectPrerequisite sp : prerequisiteRepository.findBySubjectId(mainSubjectId)) {
            alreadyPrereqIds.add(sp.getPrerequisiteSubjectId());
        }

        Map<Integer, List<Integer>> graph = buildPrerequisiteGraph();

        List<Subject> result = new ArrayList<>();
        for (Subject s : all) {
            if (s.getId().equals(mainSubjectId)) {
                continue;
            }
            if (alreadyPrereqIds.contains(s.getId())) {
                continue;
            }
            if (wouldCreateCycle(graph, mainSubjectId, s.getId())) {
                continue;
            }

            result.add(s);
        }
        return result;
    }

    @Transactional
    public void addPrerequisite(Integer subjectId, Integer prerequisiteSubjectId) {
        if (subjectId.equals(prerequisiteSubjectId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể chọn chính mình làm tiên quyết!");
        }

        if (prerequisiteRepository.existsBySubjectIdAndPrerequisiteSubjectId(
                subjectId, prerequisiteSubjectId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Môn này đã được thêm làm tiên quyết rồi!");
        }

        Map<Integer, List<Integer>> graph = buildPrerequisiteGraph();
        if (wouldCreateCycle(graph, subjectId, prerequisiteSubjectId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể thêm: sẽ tạo vòng lặp tiên quyết!");
        }

        SubjectPrerequisite sp = new SubjectPrerequisite();
        sp.setSubjectId(subjectId);
        sp.setPrerequisiteSubjectId(prerequisiteSubjectId);
        prerequisiteRepository.save(sp);
    }

    @Transactional
    public void removePrerequisite(Integer subjectId, Integer prerequisiteSubjectId) {
        SubjectPrerequisiteKey key = new SubjectPrerequisiteKey(subjectId, prerequisiteSubjectId);
        prerequisiteRepository.deleteById(key);
    }

    public List<SubjectPrerequisite> getPrerequisites(Integer subjectId) {
        return prerequisiteRepository.findBySubjectId(subjectId);
    }

    private Map<Integer, List<Integer>> buildPrerequisiteGraph() {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        for (SubjectPrerequisite sp : prerequisiteRepository.findAll()) {
            graph.computeIfAbsent(sp.getSubjectId(), k -> new ArrayList<>())
                    .add(sp.getPrerequisiteSubjectId());
        }
        return graph;
    }

    private boolean wouldCreateCycle(Map<Integer, List<Integer>> graph,
                                     Integer subjectId,
                                     Integer prerequisiteId) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(prerequisiteId);

        while (!stack.isEmpty()) {
            Integer current = stack.pop();
            if (current.equals(subjectId)) {
                return true;
            }
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            List<Integer> prereqs = graph.getOrDefault(current, Collections.emptyList());
            for (Integer prereq : prereqs) {
                if (!visited.contains(prereq)) {
                    stack.push(prereq);
                }
            }
        }
        return false;
    }

    private void validateCodeUnique(String code, Integer excludeId) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mã môn không được để trống");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trạng thái phải là ACTIVE hoặc HIDDEN");
        }
    }

    private String normalizeStatus(String status) {
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return SubjectStatus.HIDDEN.name();
        }
        return status;
    }
}
