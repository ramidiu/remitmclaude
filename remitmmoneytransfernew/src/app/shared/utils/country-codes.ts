// ISO country-code conversion (alpha-3 → alpha-2). Corridors use alpha-3 (CMR) while Transfer
// Config / flags use alpha-2 (CM). Single source so per-COUNTRY matching and flags stay correct
// even when several countries share a currency (XOF, XAF). Reference data only.
export const ALPHA3_TO_ALPHA2: Record<string, string> = {
  GBR: 'GB', IND: 'IN', PAK: 'PK', NGA: 'NG', GHA: 'GH', PHL: 'PH', AUS: 'AU',
  NPL: 'NP', BGD: 'BD', KEN: 'KE', ZAF: 'ZA', LKA: 'LK', USA: 'US', ARE: 'AE',
  CAN: 'CA', SGP: 'SG', MYS: 'MY', DEU: 'DE', FRA: 'FR', ITA: 'IT', ESP: 'ES',
  SDN: 'SD', TUR: 'TR', EGY: 'EG', SAU: 'SA', QAT: 'QA', UGA: 'UG', MAR: 'MA',
  // African corridor additions
  ZMB: 'ZM', ZWE: 'ZW', RWA: 'RW', CIV: 'CI', CMR: 'CM', TZA: 'TZ', SEN: 'SN',
  MOZ: 'MZ', SLE: 'SL', BFA: 'BF', NER: 'NE', GIN: 'GN', GAB: 'GA', MWI: 'MW',
  COD: 'CD', BEN: 'BJ', BDI: 'BI', TGO: 'TG', TCD: 'TD', ETH: 'ET',
};

/** Normalize any alpha-2/alpha-3 code to upper-case alpha-2 ('' if blank). */
export function toAlpha2(code?: string | null): string {
  if (!code) return '';
  const c = code.trim().toUpperCase();
  if (c.length === 2) return c;
  return ALPHA3_TO_ALPHA2[c] || c;
}

// Display names keyed by alpha-2.
export const COUNTRY_NAMES: Record<string, string> = {
  GB: 'United Kingdom', IN: 'India', PK: 'Pakistan', NG: 'Nigeria', GH: 'Ghana',
  PH: 'Philippines', AU: 'Australia', NP: 'Nepal', BD: 'Bangladesh', KE: 'Kenya',
  ZA: 'South Africa', LK: 'Sri Lanka', US: 'United States', AE: 'UAE', CA: 'Canada',
  SG: 'Singapore', MY: 'Malaysia', DE: 'Germany', FR: 'France', IT: 'Italy', ES: 'Spain',
  SD: 'Sudan', TR: 'Turkey', EG: 'Egypt', SA: 'Saudi Arabia', QA: 'Qatar', UG: 'Uganda',
  MA: 'Morocco', ZM: 'Zambia', ZW: 'Zimbabwe', RW: 'Rwanda', CI: "Côte d'Ivoire",
  CM: 'Cameroon', TZ: 'Tanzania', SN: 'Senegal', MZ: 'Mozambique', SL: 'Sierra Leone',
  BF: 'Burkina Faso', NE: 'Niger', GN: 'Guinea', GA: 'Gabon', MW: 'Malawi',
  CD: 'DR Congo', BJ: 'Benin', BI: 'Burundi', TG: 'Togo', TD: 'Chad', ET: 'Ethiopia',
};

/** Full country name for any alpha-2/alpha-3 code (falls back to the code itself). */
export function countryName(code?: string | null): string {
  if (!code) return '';
  return COUNTRY_NAMES[toAlpha2(code)] || code;
}
