package com.remitm.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Step 1 of the customer email-change flow: request an OTP to be sent to the NEW email. */
@Data
public class EmailChangeRequest {

    @NotBlank(message = "New email is required")
    @Email(message = "A valid email address is required")
    private String newEmail;
}
