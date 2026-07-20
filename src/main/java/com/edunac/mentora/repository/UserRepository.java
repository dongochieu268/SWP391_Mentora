package com.edunac.mentora.repository;

import com.edunac.mentora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole_Name(String roleName);
    long countByStatus(String status);

    @Query("SELECT u FROM User u WHERE " +
            "(:role IS NULL OR u.role.name = :role) AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "                 OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> findAllFiltered(@Param("role") String role,
                               @Param("status") String status,
                               @Param("search") String search);

    @Query("SELECT u FROM User u WHERE u.role.name = 'STUDENT' AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "                 OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> findStudentsFiltered(@Param("status") String status,
                                    @Param("search") String search);
}
