/**
 * Total persisted movement alerts (all types: safe zone, registered home exit, immobility,
 * no GPS, rapid movement — same rows as movement-service DB) above this count → critical list.
 */
export const CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD = 5;

/** @deprecated use CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD */
export const SAFE_ZONE_EXITS_CRITICAL_THRESHOLD = CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD;

export function isCriticalClinicalMovementAlertCount(count: number): boolean {
  return count > CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD;
}

/** @deprecated use isCriticalClinicalMovementAlertCount */
export function isCriticalSafeZoneExits(count: number): boolean {
  return isCriticalClinicalMovementAlertCount(count);
}

/** 3-band “thermometer” from total movement alerts (for UI + history). */
export type MovementRiskBand = 'low' | 'medium' | 'high';

/** ≤10 = Low (green), ≤20 = Medium (orange), else High (red). */
export const RISK_THERM_ALERT_LOW_MAX = 10;
export const RISK_THERM_ALERT_MEDIUM_MAX = 20;

export function movementRiskBand(totalAlerts: number): MovementRiskBand {
  const n = Math.max(0, Number(totalAlerts) || 0);
  if (n <= RISK_THERM_ALERT_LOW_MAX) return 'low';
  if (n <= RISK_THERM_ALERT_MEDIUM_MAX) return 'medium';
  return 'high';
}

/** English labels for jury / doctor UI */
export const MOVEMENT_RISK_LABEL_EN: Record<MovementRiskBand, string> = {
  low: 'Low',
  medium: 'Medium',
  high: 'High',
};
