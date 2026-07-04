package com.edunac.mentora.domain.level;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Bridge between a NodeLevel and a Material: draw exactly questionCount
 * questions at random from the material's pool at test-generation time.
 * If the material has fewer questions, take however many exist —
 * no cross-material borrowing (spec OQ7).
 */
@Entity
@Table(name = "level_materials",
        uniqueConstraints = @UniqueConstraint(name = "uq_level_material",
                columnNames = {"node_level_id", "material_id"}))
@Getter
@Setter
public class LevelMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_level_id", nullable = false)
    private NodeLevel nodeLevel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;
}
