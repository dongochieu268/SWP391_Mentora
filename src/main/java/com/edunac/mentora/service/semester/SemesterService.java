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
        String trimmedName = validateAndNormalize(name, startDate, endDate, status, null);

        Semester semester = new Semester();
        semester.setName(trimmedName);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semester.setStatus(status);

        return semesterRepository.save(semester);
    }

    public Semester update(Integer id, String name, LocalDate startDate, LocalDate endDate, String status) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học kỳ."));

        String trimmedName = validateAndNormalize(name, startDate, endDate, status, id);

        semester.setName(trimmedName);
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

    private String validateAndNormalize(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Integer excludeId
    ) {
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

        String trimmedName = name.trim();

        boolean duplicateName = excludeId == null
                ? semesterRepository.existsByNameIgnoreCase(trimmedName)
                : semesterRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, excludeId);
        if (duplicateName) {
            throw new IllegalArgumentException("Đã tồn tại học kỳ với tên \"" + trimmedName + "\".");
        }

        if (semesterRepository.existsOverlappingPeriod(startDate, endDate, excludeId)) {
            throw new IllegalArgumentException("Khoảng thời gian trùng với học kỳ khác.");
        }

        return trimmedName;
    }
}
