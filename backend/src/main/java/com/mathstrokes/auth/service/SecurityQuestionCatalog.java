package com.mathstrokes.auth.service;

import java.util.List;

import com.mathstrokes.auth.dto.SecurityQuestionResponse;
import org.springframework.stereotype.Component;

/**
 * The canonical security questions offered at registration.
 *
 * Served rather than hardcoded in the client so the list can change in one place. Registration
 * does not enforce membership: accounts created against an older list must keep working, and the
 * stored question text is what the recovery flow reads back.
 */
@Component
public class SecurityQuestionCatalog {

    private static final List<SecurityQuestionResponse> QUESTIONS = List.of(
            new SecurityQuestionResponse("FIRST_SCHOOL", "What was the name of your first school?"),
            new SecurityQuestionResponse("BIRTH_CITY", "In which city were you born?"),
            new SecurityQuestionResponse("FAVOURITE_TEACHER",
                    "What is the name of your favourite teacher?"),
            new SecurityQuestionResponse("CHILDHOOD_NICKNAME", "What was your childhood nickname?"),
            new SecurityQuestionResponse("FAVOURITE_SUBJECT",
                    "Which subject did you enjoy most in school?"),
            new SecurityQuestionResponse("MOTHER_MAIDEN_NAME",
                    "What is your mother's maiden name?"));

    public List<SecurityQuestionResponse> all() {
        return QUESTIONS;
    }
}
