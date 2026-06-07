package com.edunac.mentora.repository.subject;

import com.edunac.mentora.domain.subject.SubjectPrerequisite;
import com.edunac.mentora.domain.subject.SubjectPrerequisiteKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectPrerequisiteRepository extends JpaRepository<SubjectPrerequisite, SubjectPrerequisiteKey> {

    List<SubjectPrerequisite> findBySubjectId(Integer subjectId);

    boolean existsByPrerequisiteSubjectId(Integer prerequisiteSubjectId);

    boolean existsBySubjectIdAndPrerequisiteSubjectId(Integer subjectId, Integer prerequisiteSubjectId);

    void deleteBySubjectId(Integer subjectId);
}
