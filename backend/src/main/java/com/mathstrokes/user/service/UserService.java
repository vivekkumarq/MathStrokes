package com.mathstrokes.user.service;

import com.mathstrokes.auth.dto.UserProfileResponse;
import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.common.enums.RoleName;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import com.mathstrokes.user.dto.StudentSummaryResponse;
import com.mathstrokes.user.entity.User;
import com.mathstrokes.user.mapper.UserMapper;
import com.mathstrokes.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    public UserService(UserRepository userRepository, UserMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    public UserProfileResponse profileOf(Long userId) {
        return mapper.toProfile(requireUser(userId));
    }

    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Loads a user and insists they are a student.
     *
     * The admin student routes are about students, so an id belonging to another administrator
     * should not quietly return that administrator's record through a path that says /students.
     */
    public User requireStudent(Long userId) {
        User user = requireUser(userId);
        if (!user.hasRole(RoleName.ROLE_STUDENT)) {
            throw new ResourceNotFoundException("Student", userId);
        }
        return user;
    }

    public PageResponse<StudentSummaryResponse> listStudents(String search, Pageable pageable) {
        String normalised = (search == null || search.isBlank()) ? null : search.trim();
        return PageResponse.from(userRepository.findStudentRows(normalised, pageable),
                row -> new StudentSummaryResponse(
                        row.getId(),
                        row.getFullName(),
                        row.getPhoneNumber(),
                        row.getEnabled(),
                        row.getLastLoginAt(),
                        row.getRegisteredAt(),
                        row.getAttemptCount()));
    }

    /**
     * Disabling keeps the account and all its history but blocks sign-in. Accounts are never
     * deleted: attempts and results reference them, and a teacher needs those records to stay
     * intact.
     */
    @Transactional
    public UserProfileResponse setEnabled(Long userId, boolean enabled) {
        User user = requireUser(userId);
        user.setEnabled(enabled);
        return mapper.toProfile(user);
    }
}
