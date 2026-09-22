package com.example.auditlog.api;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RedactEventRequest(@NotEmpty List<String> paths) {
}
