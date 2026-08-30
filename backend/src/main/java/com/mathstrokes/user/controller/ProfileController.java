package com.mathstrokes.user.controller;

import com.mathstrokes.auth.dto.UserProfileResponse;
import com.mathstrokes.security.service.SecurityUtils;
import com.mathstrokes.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own profile, whatever their role. */
@RestController
@RequestMapping("/profile")
@Tag(name = "Profile", description = "The signed-in account")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Who am I",
            description = "Resolved from the access token, never from a supplied id.")
    public UserProfileResponse me() {
        return userService.profileOf(SecurityUtils.requireUserId());
    }
}
