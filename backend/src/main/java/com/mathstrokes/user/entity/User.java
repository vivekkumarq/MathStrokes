package com.mathstrokes.user.entity;

import java.util.HashSet;
import java.util.Set;

import com.mathstrokes.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A platform account. Login identity is the phone number.
 *
 * Neither {@code passwordHash} nor {@code securityAnswerHash} ever leaves the service layer:
 * no DTO in this project maps them, so they cannot be serialised into a response by accident.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "security_question", length = 255)
    private String securityQuestion;

    @Column(name = "security_answer_hash", length = 100)
    private String securityAnswerHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_login_at")
    private java.time.Instant lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public boolean hasRole(com.mathstrokes.common.enums.RoleName roleName) {
        return roles.stream().anyMatch(r -> r.getName() == roleName);
    }
}
