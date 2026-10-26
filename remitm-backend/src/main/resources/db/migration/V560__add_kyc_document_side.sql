-- Two-sided identity documents (e.g. driving licence, national ID) have a FRONT and a BACK.
-- Previously both were uploaded under the same document_type and the second upload superseded
-- (deleted) the first, so only one side survived. This column distinguishes the sides so both
-- are stored and reviewable. NULL = single-file documents (passport, proof of address).
ALTER TABLE kyc_documents
    ADD COLUMN document_side VARCHAR(10) NULL AFTER document_type;
