package com.mathstrokes.user.repository;

import java.util.Optional;

import com.mathstrokes.common.enums.RoleName;
import com.mathstrokes.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
