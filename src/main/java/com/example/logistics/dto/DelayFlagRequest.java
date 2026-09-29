package com.example.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Flags (or, via the resolve endpoint, clears) a delay on a transport task without
 * changing its lifecycle status. The acting user comes from the authenticated caller.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DelayFlagRequest {

    @NotBlank(message = "reason is required")
    @Size(max = 500, message = "reason must be at most 500 characters")
    private String reason;
}
