package com.remitm.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Step 2 of the customer email-change flow: verify the OTP sent to the new email and apply the change. */
@Data
public class EmailChangeVerifyRequest {

    @NotBlank(message = "New email is required")
    @Email(message = "A valid email address is required")
    private String newEmail;

    @NotBlank(message = "Verification code is required")
    private String otp;
}
