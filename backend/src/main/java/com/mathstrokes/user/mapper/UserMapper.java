package com.mathstrokes.user.mapper;

import com.mathstrokes.auth.dto.UserProfileResponse;
import com.mathstrokes.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRoles().stream().map(role -> role.getName().name()).sorted().toList(),
                user.isEnabled(),
                user.getCreatedAt());
    }
}
