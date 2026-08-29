package com.mathstrokes.auth.dto;

/**
 * One entry of the canonical security-question list.
 *
 * The client submits the {@code text} at registration, not the id: the text is what gets stored
 * and shown back during recovery, so the list can be extended later without stranding accounts
 * created against an earlier version of it.
 */
public record SecurityQuestionResponse(String id, String text) {
}
