package com.example.logistics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Flags (or, via the resolve endpoint, clears) a delay on a transport task's
 * delivery status without changing its current lifecycle status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DelayFlagRequest {

    @NotBlank(message = "reason is required")
    private String reason;

    @NotNull(message = "updatedBy is required")
    private UUID updatedBy;
}
