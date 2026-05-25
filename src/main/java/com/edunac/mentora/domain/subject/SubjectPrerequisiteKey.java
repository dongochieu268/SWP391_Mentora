package com.edunac.mentora.domain.subject;

import java.io.Serializable;
import java.util.Objects;

public class SubjectPrerequisiteKey implements Serializable {

    private Integer subjectId;
    private Integer prerequisiteSubjectId;

    public SubjectPrerequisiteKey() {}

    public SubjectPrerequisiteKey(Integer subjectId, Integer prerequisiteSubjectId) {
        this.subjectId = subjectId;
        this.prerequisiteSubjectId = prerequisiteSubjectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectPrerequisiteKey k)) return false;
        return Objects.equals(subjectId, k.subjectId)
                && Objects.equals(prerequisiteSubjectId, k.prerequisiteSubjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjectId, prerequisiteSubjectId);
    }
}
