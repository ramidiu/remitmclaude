-- Super-admin toggles (System Controls) governing whether pay-in partners may CREATE
-- pay-in customers / pay-in transactions. When OFF, the portal hides/disables the option
-- AND the backend rejects the create call. Default ON to preserve current behaviour.
INSERT IGNORE INTO system_config (config_key, config_value, value_type, category, allowed_values, version, description)
VALUES
  ('payin.customer_creation.enabled', 'true', 'BOOLEAN', 'operations', NULL, 1,
   'When ON, pay-in partners can create pay-in customers. OFF hides/disables the Create Customer option.'),
  ('payin.transaction_creation.enabled', 'true', 'BOOLEAN', 'operations', NULL, 1,
   'When ON, pay-in partners can create pay-in transactions. OFF hides/disables the Create Transaction option.');
