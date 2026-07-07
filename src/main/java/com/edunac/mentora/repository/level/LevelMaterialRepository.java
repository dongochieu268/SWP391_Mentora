package com.edunac.mentora.repository.level;

import com.edunac.mentora.domain.level.LevelMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LevelMaterialRepository extends JpaRepository<LevelMaterial, Integer> {

    List<LevelMaterial> findByNodeLevel_Id(Integer nodeLevelId);

    boolean existsByNodeLevel_IdAndMaterial_Id(Integer nodeLevelId, Integer materialId);

    // Blocks material deletion while any level still uses it (L2 §4).
    boolean existsByMaterial_Id(Integer materialId);

    void deleteByNodeLevel_Id(Integer nodeLevelId);
}
