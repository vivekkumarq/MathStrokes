package com.mathstrokes.user.repository;

import java.util.Optional;

import com.mathstrokes.common.enums.RoleName;
import com.mathstrokes.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("select count(distinct u) from User u join u.roles r where r.name = :roleName")
    long countByRole(@Param("roleName") RoleName roleName);

    @Query("""
            select count(distinct u) from User u join u.roles r
            where r.name = :roleName and u.lastLoginAt >= :since
            """)
    long countActiveSince(@Param("roleName") RoleName roleName,
                          @Param("since") java.time.Instant since);

    /**
     * The admin student grid, with each student's attempt count folded into the same query.
     * A LEFT JOIN onto a grouped subquery keeps this one indexed pass instead of N+1 counts.
     */
    @Query(value = """
            SELECT u.id                        AS id,
                   u.full_name                 AS fullName,
                   u.phone_number              AS phoneNumber,
                   u.enabled                   AS enabled,
                   u.last_login_at             AS lastLoginAt,
                   u.created_at                AS registeredAt,
                   COALESCE(tally.attempts, 0) AS attemptCount
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id
            JOIN roles r       ON r.id = ur.role_id AND r.name = 'ROLE_STUDENT'
            LEFT JOIN (
                SELECT student_id, COUNT(*) AS attempts
                FROM test_attempts
                GROUP BY student_id
            ) tally ON tally.student_id = u.id
            WHERE (:search IS NULL
                   OR u.full_name ILIKE ('%' || :search || '%')
                   OR u.phone_number LIKE ('%' || :search || '%'))
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id
            JOIN roles r       ON r.id = ur.role_id AND r.name = 'ROLE_STUDENT'
            WHERE (:search IS NULL
                   OR u.full_name ILIKE ('%' || :search || '%')
                   OR u.phone_number LIKE ('%' || :search || '%'))
            """,
            nativeQuery = true)
    Page<StudentListRow> findStudentRows(@Param("search") String search, Pageable pageable);
}
