package com.edunac.mentora.domain.subject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subject_prerequisites")
@IdClass(SubjectPrerequisiteKey.class)
@Getter
@Setter
public class SubjectPrerequisite {

    @Id
    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Id
    @Column(name = "prerequisite_subject_id", nullable = false)
    private Integer prerequisiteSubjectId;
}
