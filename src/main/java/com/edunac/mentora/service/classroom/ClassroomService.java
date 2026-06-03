package com.edunac.mentora.service.classroom;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomStatus;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.domain.learningpath.LearningPathStatus;
import com.edunac.mentora.domain.semester.Semester;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.dto.ClassroomForm;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import com.edunac.mentora.repository.learningpath.LearningPathRepository;
import com.edunac.mentora.repository.semester.SemesterRepository;
import com.edunac.mentora.repository.subject.SubjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@Transactional
public class ClassroomService {

    private static final List<String> ALLOWED_STATUSES = List.of(
            ClassroomStatus.OPEN.name(),
            ClassroomStatus.CLOSE.name()
    );

    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_LENGTH = 8;

    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final LearningPathRepository learningPathRepository;
    private final SemesterRepository semesterRepository;
    private final SecureRandom random = new SecureRandom();

    public ClassroomService(
            ClassroomRepository classroomRepository,
            SubjectRepository subjectRepository,
            LearningPathRepository learningPathRepository,
            SemesterRepository semesterRepository
    ) {
        this.classroomRepository = classroomRepository;
        this.subjectRepository = subjectRepository;
        this.learningPathRepository = learningPathRepository;
        this.semesterRepository = semesterRepository;
    }

    public List<Classroom> findByTeacher(User teacher) {
        return classroomRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId());
    }

    public List<LearningPath> findPathsForTeacherAndSubject(User teacher, Integer subjectId) {
        String active = LearningPathStatus.ACTIVE.name();
        if (subjectId == null) {
            return learningPathRepository.findByCreatedByIdAndStatusOrderByCreatedAtDesc(teacher.getId(), active);
        }
        return learningPathRepository.findByCreatedByIdAndSubjectIdAndStatusOrderByCreatedAtDesc(
                teacher.getId(), subjectId, active);
    }

    public Classroom findForTeacher(Integer id, User teacher) {
        return classroomRepository.findByIdAndTeacherId(id, teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy lớp học hoặc bạn không có quyền truy cập."));
    }

    public ClassroomForm toForm(Classroom classroom) {
        ClassroomForm form = new ClassroomForm();
        form.setId(classroom.getId());
        form.setName(classroom.getName());
        form.setSubjectId(classroom.getSubject().getId());
        form.setLearningPathId(classroom.getLearningPath().getId());
        form.setSemesterId(classroom.getSemester().getId());
        form.setStatus(classroom.getStatus());
        return form;
    }

    public Classroom create(ClassroomForm form, User teacher) {
        validateName(form.getName());
        String status = normalizeStatus(form.getStatus());

        Subject subject = loadActiveSubject(form.getSubjectId());
        LearningPath path = loadOwnedPath(form.getLearningPathId(), teacher, subject.getId());
        Semester semester = loadActiveSemester(form.getSemesterId());

        Classroom classroom = new Classroom();
        classroom.setSubject(subject);
        classroom.setLearningPath(path);
        classroom.setTeacher(teacher);
        classroom.setCreatedBy(teacher);
        classroom.setName(form.getName().trim());
        classroom.setSemester(semester);
        classroom.setStatus(status);
        classroom.setInviteCode(generateUniqueInviteCode());

        return classroomRepository.save(classroom);
    }

    public Classroom updateStatus(Integer id, String status, User teacher) {
        Classroom classroom = findForTeacher(id, teacher);
        String normalized = normalizeStatus(status);
        classroom.setStatus(normalized);
        return classroomRepository.save(classroom);
    }

    public void delete(Integer id, User teacher) {
        Classroom classroom = findForTeacher(id, teacher);
        try {
            classroomRepository.delete(classroom);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Không thể xóa lớp đang có sinh viên hoặc dữ liệu liên quan.");
        }
    }

    private Subject loadActiveSubject(Integer subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học."));
        if (!subject.isActive()) {
            throw new IllegalArgumentException("Môn học phải đang ở trạng thái ACTIVE.");
        }
        return subject;
    }

    private LearningPath loadOwnedPath(Integer pathId, User teacher, Integer subjectId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình học."));
        if (!path.getCreatedBy().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Lộ trình phải do bạn tạo.");
        }
        if (!path.getSubject().getId().equals(subjectId)) {
            throw new IllegalArgumentException("Lộ trình không thuộc môn học đã chọn.");
        }
        if (LearningPathStatus.ARCHIVED.name().equals(path.getStatus())) {
            throw new IllegalArgumentException("Lộ trình đã được lưu trữ, không thể dùng cho lớp mới.");
        }
        return path;
    }

    private Semester loadActiveSemester(Integer semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học kỳ."));
        if (!"ACTIVE".equals(semester.getStatus())) {
            throw new IllegalArgumentException("Học kỳ phải đang ở trạng thái ACTIVE.");
        }
        return semester;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên lớp không được để trống.");
        }
        if (name.trim().length() > 200) {
            throw new IllegalArgumentException("Tên lớp tối đa 200 ký tự.");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return ClassroomStatus.OPEN.name();
        }
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái lớp phải là OPEN hoặc CLOSE.");
        }
        return normalized;
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomInviteCode();
            if (!classroomRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể tạo mã mời lớp học, vui lòng thử lại.");
    }

    private String randomInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_LENGTH);
        for (int i = 0; i < INVITE_LENGTH; i++) {
            sb.append(INVITE_ALPHABET.charAt(random.nextInt(INVITE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
