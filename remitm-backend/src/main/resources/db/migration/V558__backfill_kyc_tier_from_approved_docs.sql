-- ============================================================================
-- V558: Backfill kyc_tier from already-APPROVED KYC documents.
-- ----------------------------------------------------------------------------
-- Migrated users had their KYC documents imported as APPROVED but their kyc_tier
-- left at TIER_0 (the import never ran KycTierEvaluator.evaluateAndUpgrade). With
-- TIER_0 the Send Money guard blocks them ("Please complete your identity
-- verification...") even though their ID is verified. This replicates the
-- evaluator's rules so verified users get the correct tier.
--
-- Tier rules (KycTierEvaluator.calculateTier):
--   ID (PASSPORT/DRIVING_LICENCE/NATIONAL_ID)                          -> TIER_1
--   ID + PROOF_OF_ADDRESS                                              -> TIER_2
--   ID + PROOF_OF_ADDRESS + SOURCE_OF_FUNDS                            -> TIER_3
-- Only upgrades (touches TIER_0 users that have an approved ID doc); never downgrades.
-- ============================================================================

UPDATE users u
SET u.kyc_tier = CASE
    WHEN EXISTS (SELECT 1 FROM kyc_documents d WHERE d.user_id = u.id AND d.status = 'APPROVED' AND d.document_type IN ('PASSPORT','DRIVING_LICENCE','NATIONAL_ID'))
     AND EXISTS (SELECT 1 FROM kyc_documents d WHERE d.user_id = u.id AND d.status = 'APPROVED' AND d.document_type = 'PROOF_OF_ADDRESS')
     AND EXISTS (SELECT 1 FROM kyc_documents d WHERE d.user_id = u.id AND d.status = 'APPROVED' AND d.document_type = 'SOURCE_OF_FUNDS') THEN 'TIER_3'
    WHEN EXISTS (SELECT 1 FROM kyc_documents d WHERE d.user_id = u.id AND d.status = 'APPROVED' AND d.document_type IN ('PASSPORT','DRIVING_LICENCE','NATIONAL_ID'))
     AND EXISTS (SELECT 1 FROM kyc_documents d WHERE d.user_id = u.id AND d.status = 'APPROVED' AND d.document_type = 'PROOF_OF_ADDRESS') THEN 'TIER_2'
    WHEN EXISTS (SELECT 1 FROM kyc_documents d WHERE d.user_id = u.id AND d.status = 'APPROVED' AND d.document_type IN ('PASSPORT','DRIVING_LICENCE','NATIONAL_ID')) THEN 'TIER_1'
    ELSE u.kyc_tier END
WHERE u.kyc_tier = 'TIER_0'
  AND EXISTS (SELECT 1 FROM kyc_documents d WHERE d.user_id = u.id AND d.status = 'APPROVED' AND d.document_type IN ('PASSPORT','DRIVING_LICENCE','NATIONAL_ID'));
