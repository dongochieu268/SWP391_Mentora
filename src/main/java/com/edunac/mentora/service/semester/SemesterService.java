package com.edunac.mentora.service.semester;

import com.edunac.mentora.domain.semester.Semester;
import com.edunac.mentora.repository.semester.SemesterRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SemesterService {

    private static final List<String> ALLOWED_STATUSES = List.of("ACTIVE", "HIDDEN");

    private final SemesterRepository semesterRepository;

    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    public List<Semester> findAll() {
        return semesterRepository.findAllByOrderByStartDateDesc();
    }

    public Optional<Semester> findById(Integer id) {
        return semesterRepository.findById(id);
    }

    public Semester create(String name, LocalDate startDate, LocalDate endDate, String status) {
        validate(name, startDate, endDate, status);

        Semester semester = new Semester();
        semester.setName(name.trim());
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semester.setStatus(status);

        return semesterRepository.save(semester);
    }

    public Semester update(Integer id, String name, LocalDate startDate, LocalDate endDate, String status) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học kỳ."));

        validate(name, startDate, endDate, status);

        semester.setName(name.trim());
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semester.setStatus(status);

        return semesterRepository.save(semester);
    }

    public void delete(Integer id) {
        if (!semesterRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy học kỳ.");
        }

        try {
            semesterRepository.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Không thể xóa học kỳ đang được sử dụng bởi lớp học.");
        }
    }

    private void validate(String name, LocalDate startDate, LocalDate endDate, String status) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên học kỳ không được để trống.");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc là bắt buộc.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }

        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
    }
}
