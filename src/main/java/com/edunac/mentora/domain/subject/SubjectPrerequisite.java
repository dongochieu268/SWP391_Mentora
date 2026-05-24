package com.edunac.mentora.domain.subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subject_prerequisites")
@Getter @Setter
public class SubjectPrerequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Column(name = "prerequisite_id", nullable = false)
    private Integer prerequisiteId;

    @Column(name = "requirement_type", length = 50)
    private String requirementType = "REQUIRED";

    @Column(name = "created_by", nullable = false)
    private Integer createdBy = 1;
}