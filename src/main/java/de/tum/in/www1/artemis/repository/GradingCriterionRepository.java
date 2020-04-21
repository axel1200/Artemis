package de.tum.in.www1.artemis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.GradingCriterion;

/**
 * Spring Data JPA repository for the GradingCriteria entity.
 */
@Repository
public interface GradingCriterionRepository extends JpaRepository<GradingCriterion, Long> {

    List<GradingCriterion> findAllByExerciseId(long exerciseId);

    @Query("SELECT DISTINCT criterion FROM GradingCriterion criterion LEFT JOIN FETCH criterion.structuredGradingInstructions WHERE criterion.exercise.id = :#{#exerciseId}")
    List<GradingCriterion> findAllByExerciseIdWithEagerGradingCriteria(@Param("exerciseId") long exerciseId);
}
