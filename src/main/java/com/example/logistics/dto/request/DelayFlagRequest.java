package com.example.logistics.dto.request;

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
    @Size(min = 3, max = 500, message = "reason must be between 3 and 500 characters")
    private String reason;
}
