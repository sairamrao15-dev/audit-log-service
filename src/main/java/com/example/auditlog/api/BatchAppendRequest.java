package com.example.auditlog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchAppendRequest(@NotEmpty List<@Valid BatchEventRequest> events) {
}
