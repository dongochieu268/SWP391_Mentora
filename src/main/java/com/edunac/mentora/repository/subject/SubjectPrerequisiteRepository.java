package com.edunac.mentora.repository.subject;

import com.edunac.mentora.domain.subject.SubjectPrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectPrerequisiteRepository extends JpaRepository<SubjectPrerequisite, Integer> {

    List<SubjectPrerequisite> findBySubjectId(Integer subjectId);

    boolean existsByPrerequisiteId(Integer prerequisiteId);

    boolean existsBySubjectIdAndPrerequisiteId(Integer subjectId, Integer prerequisiteId);

    void deleteBySubjectId(Integer subjectId);
}