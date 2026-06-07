package com.edunac.mentora.repository.semester;

import com.edunac.mentora.domain.semester.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SemesterRepository extends JpaRepository<Semester, Integer> {

    List<Semester> findAllByOrderByStartDateDesc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    @Query("""
            SELECT COUNT(s) > 0 FROM Semester s
            WHERE s.startDate <= :endDate AND s.endDate >= :startDate
            AND (:excludeId IS NULL OR s.id <> :excludeId)
            """)
    boolean existsOverlappingPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Integer excludeId
    );
}