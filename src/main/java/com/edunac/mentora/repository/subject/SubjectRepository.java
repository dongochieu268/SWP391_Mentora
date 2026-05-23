package com.edunac.mentora.repository.subject;

import com.edunac.mentora.domain.subject.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Integer id);

    List<Subject> findByStatusOrderByNameAsc(String status);
}