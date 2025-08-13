/**
 * WebSocket Message Types for Frontend Integration
 * Defines TypeScript interfaces for real-time communication between backend and frontend
 */

// Base WebSocket Message Interface
export interface WebSocketMessage {
  type: string;
  sessionId?: string;
  timestamp: number;
  data: any;
}

// Agent Progress Message Types
export interface AgentProgressMessage extends WebSocketMessage {
  type: 'agent.progress';
  sessionId: string;
  data: {
    stage: number;
    status: 'started' | 'completed' | 'failed';
    message: string;
    timestamp: string;
    duration?: number;
    cost?: number;
  };
}

export interface AgentErrorMessage extends WebSocketMessage {
  type: 'agent.error';
  sessionId: string;
  data: {
    stage: number;
    status: 'failed';
    message: string;
    timestamp: string;
  };
}

// Session Status Message Types
export interface SessionStartedMessage extends WebSocketMessage {
  type: 'session.started';
  sessionId: string;
  data: {
    topic: string;
    estimatedDuration?: number;
    timestamp: string;
  };
}

export interface SessionCompletedMessage extends WebSocketMessage {
  type: 'session.completed';
  sessionId: string;
  data: {
    actualDuration?: number;
    qualityScore?: number;
    totalCost?: number;
    timestamp: string;
  };
}

export interface SessionErrorMessage extends WebSocketMessage {
  type: 'session.error';
  sessionId: string;
  data: {
    errorMessage: string;
    failedAtStage?: string;
    timestamp: string;
  };
}

// Content Generation Message Types
export interface ContentGeneratedMessage extends WebSocketMessage {
  type: 'content.generated';
  sessionId: string;
  data: {
    contentId: string;
    contentType: string;
    title: string;
    wordCount?: number;
    qualityScore?: number;
    timestamp: string;
  };
}

export interface ContentUpdatedMessage extends WebSocketMessage {
  type: 'content.updated';
  sessionId: string;
  data: {
    contentId: string;
    newVersion: number;
    changes?: string;
    timestamp: string;
  };
}

// System Notification Message Types
export interface SystemNotificationMessage extends WebSocketMessage {
  type: 'system.notification';
  data: {
    notificationType: 'maintenance' | 'alert' | 'announcement';
    maintenanceType?: string;
    estimatedDuration?: number;
    affectedServices?: string[];
    timestamp: string;
  };
}

export interface SystemAlertMessage extends WebSocketMessage {
  type: 'system.alert';
  data: {
    alertType: string;
    severity: 'low' | 'medium' | 'high' | 'critical';
    message: string;
    affectedServices?: string[];
    timestamp: string;
  };
}

// Union type for all WebSocket messages
export type WebSocketMessageType = 
  | AgentProgressMessage
  | AgentErrorMessage
  | SessionStartedMessage
  | SessionCompletedMessage
  | SessionErrorMessage
  | ContentGeneratedMessage
  | ContentUpdatedMessage
  | SystemNotificationMessage
  | SystemAlertMessage;

// WebSocket Connection States
export type WebSocketConnectionState = 
  | 'connecting'
  | 'connected'
  | 'disconnected'
  | 'error'
  | 'reconnecting';

// WebSocket Client Configuration
export interface WebSocketClientConfig {
  url: string;
  reconnectAttempts: number;
  reconnectDelay: number;
  heartbeatInterval: number;
  token?: string;
}

// WebSocket Event Handlers
export interface WebSocketEventHandlers {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Event) => void;
  onMessage?: (message: WebSocketMessageType) => void;
  onAgentProgress?: (message: AgentProgressMessage) => void;
  onSessionUpdate?: (message: SessionStartedMessage | SessionCompletedMessage | SessionErrorMessage) => void;
  onContentUpdate?: (message: ContentGeneratedMessage | ContentUpdatedMessage) => void;
  onSystemNotification?: (message: SystemNotificationMessage | SystemAlertMessage) => void;
}

// WebSocket Subscription Management
export interface WebSocketSubscription {
  userId: string;
  sessionId?: string;
  topics: string[];
}

// Frontend WebSocket Client Interface
export interface WebSocketClient {
  connect(config: WebSocketClientConfig): Promise<void>;
  disconnect(): void;
  subscribe(subscription: WebSocketSubscription): void;
  unsubscribe(subscription: WebSocketSubscription): void;
  send(message: any): void;
  getConnectionState(): WebSocketConnectionState;
  setEventHandlers(handlers: WebSocketEventHandlers): void;
}