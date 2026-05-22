package com.edunac.mentora.repository.semester;

import com.edunac.mentora.domain.semester.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SemesterRepository extends JpaRepository<Semester, Integer> {

    List<Semester> findAllByOrderByStartDateDesc();
}