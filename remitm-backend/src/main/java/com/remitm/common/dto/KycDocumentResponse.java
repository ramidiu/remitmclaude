package com.remitm.common.dto;

import com.remitm.common.enums.KycDocumentStatus;
import com.remitm.common.enums.KycDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentResponse {

    private Long id;
    private KycDocumentType documentType;
    private String documentNumber;
    private String filePath;
    private KycDocumentStatus status;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private LocalDate expiryDate;
    private LocalDate issueDate;
    private String fileName;
    private String fileUrl;
    private LocalDateTime createdAt;
    // true = a real customer upload (has a SHA-256 file_hash); false = legacy/imported placeholder.
    private Boolean realUpload;
    // true = the most recent document of its type (the "latest" the admin should review);
    // false = an older document of that type, e.g. a previously APPROVED copy preserved for
    // reference when the customer re-uploaded.
    private Boolean latest;
}
