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

    @Query("""
            select distinct u from User u join u.roles r
            where r.name = :roleName
              and (:search is null or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or u.phoneNumber like concat('%', :search, '%'))
            """)
    Page<User> findByRole(@Param("roleName") RoleName roleName,
                          @Param("search") String search,
                          Pageable pageable);

    @Query("select count(distinct u) from User u join u.roles r where r.name = :roleName")
    long countByRole(@Param("roleName") RoleName roleName);

    @Query("""
            select count(distinct u) from User u join u.roles r
            where r.name = :roleName and u.lastLoginAt >= :since
            """)
    long countActiveSince(@Param("roleName") RoleName roleName,
                          @Param("since") java.time.Instant since);
}
