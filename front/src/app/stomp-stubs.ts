// Stub STOMP modules for compilation - actual STOMP is optional
export class Client {
  active = false;
  onConnect: (() => void) | null = null;
  onDisconnect: (() => void) | null = null;

  constructor(config?: any) {}
  activate() { this.active = true; }
  deactivate() { this.active = false; }
  subscribe() { return {}; }
  publish(msg: any) {}
}

export type IFrame = any;
export type IMessage = any;
