package com.mathstrokes.auth.dto;

/** Step 1 of recovery: the account's own security question, and nothing else about the account. */
public record ForgotPasswordQuestionResponse(String phoneNumber, String securityQuestion) {
}
