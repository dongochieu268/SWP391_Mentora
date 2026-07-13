package com.edunac.mentora.domain.level;

import com.edunac.mentora.domain.learningpath.LearningNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * An ordered difficulty tier within a LearningNode.
 * Score rule: score = correctPercent x maxScore (floor is always 0).
 * The level's total question count is derived as
 * sum(LevelMaterial.questionCount) — deliberately not stored here.
 */
@Entity
@Table(name = "node_levels",
        uniqueConstraints = @UniqueConstraint(name = "uq_node_level",
                columnNames = {"learning_node_id", "level_number"}))
@Getter
@Setter
public class NodeLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_node_id", nullable = false)
    private LearningNode learningNode;

    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;

    @Column(length = 255)
    private String title;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "passing_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal passingScore;

    @Column(name = "max_attempts")
    private Integer maxAttempts;   // NULL = unlimited

    @Column(name = "duration_minutes")
    private Integer durationMinutes;   // NULL = no time limit; enforced server-side

    @OneToMany(mappedBy = "nodeLevel", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LevelMaterial> levelMaterials = new ArrayList<>();
}
