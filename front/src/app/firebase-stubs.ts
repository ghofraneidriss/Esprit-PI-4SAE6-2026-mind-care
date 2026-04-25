// Stub Firebase modules for compilation - actual Firebase is optional
export function initializeApp(config: any) { return {}; }
export function getMessaging(app: any) { return {}; }
export type Messaging = any;

export function getToken(messaging: any, options?: any) { return Promise.resolve(''); }
export function onMessage(messaging: any, callback?: any) { return () => {}; }
export type MessagePayload = any;

export const Notification = typeof globalThis !== 'undefined' && globalThis.Notification ? globalThis.Notification : undefined;
