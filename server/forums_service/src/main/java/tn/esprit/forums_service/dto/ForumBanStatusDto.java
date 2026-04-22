package tn.esprit.forums_service.dto;

import java.time.LocalDateTime;

public record ForumBanStatusDto(boolean banned, LocalDateTime bannedUntil) {}
