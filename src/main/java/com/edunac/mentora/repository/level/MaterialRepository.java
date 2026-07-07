package com.edunac.mentora.repository.level;

import com.edunac.mentora.domain.level.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {

    List<Material> findBySubjectIdOrderByCreatedAtDesc(Integer subjectId);

    List<Material> findBySubjectId(Integer subjectId);

    List<Material> findByCreatedBy_IdOrderByCreatedAtDesc(Integer lecturerId);
}
