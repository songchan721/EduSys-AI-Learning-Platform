// TypeScript interfaces for Event system - Frontend-Backend communication

export interface BaseEventDto {
  eventType: string;
  eventId: string;
  timestamp: string; // ISO 8601 date string
  correlationId: string;
  causationId?: string;
  userId?: string;
  metadata: EventMetadataDto;
}

export interface EventMetadataDto {
  source: string;
  version: string;
  environment: string;
  region: string;
  schemaVersion: string;
}

// User Events
export interface UserRegisteredEventDto extends BaseEventDto {
  eventType: 'user.registered.v1';
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface UserUpdatedEventDto extends BaseEventDto {
  eventType: 'user.updated.v1';
  changes: Record<string, any>;
}

export interface UserRoleChangedEventDto extends BaseEventDto {
  eventType: 'user.role-changed.v1';
  oldRole: string;
  newRole: string;
  changedBy: string;
}

// Session Events
export interface SessionStartedEventDto extends BaseEventDto {
  eventType: 'session.started.v1';
  sessionId: string;
  topic: string;
  estimatedDurationMinutes?: number;
}

export interface SessionCompletedEventDto extends BaseEventDto {
  eventType: 'session.completed.v1';
  sessionId: string;
  actualDurationMinutes: number;
  qualityScore?: number;
  totalCostUsd?: number;
}

export interface SessionFailedEventDto extends BaseEventDto {
  eventType: 'session.failed.v1';
  sessionId: string;
  errorMessage: string;
  failedAtStage: string;
}

// Agent Events
export interface AgentStartedEventDto extends BaseEventDto {
  eventType: 'agent.started.v1';
  sessionId: string;
  executionId: string;
  agentType: string;
  stageNumber: number;
}

export interface AgentCompletedEventDto extends BaseEventDto {
  eventType: 'agent.completed.v1';
  sessionId: string;
  executionId: string;
  agentType: string;
  stageNumber: number;
  durationMinutes: number;
  tokensUsed?: number;
  costUsd?: number;
}

export interface AgentFailedEventDto extends BaseEventDto {
  eventType: 'agent.failed.v1';
  sessionId: string;
  executionId: string;
  agentType: string;
  stageNumber: number;
  errorMessage: string;
}

// Content Events
export interface ContentGeneratedEventDto extends BaseEventDto {
  eventType: 'content.generated.v1';
  contentId: string;
  sessionId: string;
  contentType: string;
  title: string;
  wordCount?: number;
  qualityScore?: number;
}

export interface ContentUpdatedEventDto extends BaseEventDto {
  eventType: 'content.updated.v1';
  contentId: string;
  newVersion: number;
  changes?: string;
}

// Payment Events
export interface PaymentSubscriptionActivatedEventDto extends BaseEventDto {
  eventType: 'payment.subscription-activated.v1';
  subscriptionId: string;
  planType: string;
  features: string[];
}

export interface PaymentSubscriptionCancelledEventDto extends BaseEventDto {
  eventType: 'payment.subscription-cancelled.v1';
  subscriptionId: string;
  reason?: string;
}

// System Events
export interface SystemMaintenanceStartedEventDto extends BaseEventDto {
  eventType: 'system.maintenance-started.v1';
  maintenanceType: string;
  estimatedDurationMinutes: number;
  affectedServices: string[];
}

export interface SystemAlertTriggeredEventDto extends BaseEventDto {
  eventType: 'system.alert-triggered.v1';
  alertType: string;
  severity: string;
  message: string;
  affectedServices: string[];
}

// WebSocket message types for real-time communication
export interface WebSocketMessage {
  type: WebSocketMessageType;
  sessionId?: string;
  payload: any;
  timestamp: string;
  messageId: string;
  requiresAck?: boolean;
}

export enum WebSocketMessageType {
  // Connection management
  CONNECTION_ESTABLISHED = 'connection.established',
  CONNECTION_ERROR = 'connection.error',
  PING = 'ping',
  PONG = 'pong',
  
  // Subscription management
  SUBSCRIBE = 'subscribe',
  UNSUBSCRIBE = 'unsubscribe',
  SUBSCRIBED = 'subscribed',
  UNSUBSCRIBED = 'unsubscribed',
  
  // Progress updates
  SESSION_PROGRESS = 'session.progress',
  AGENT_PROGRESS = 'agent.progress',
  
  // Notifications
  SESSION_STARTED = 'session.started',
  SESSION_COMPLETED = 'session.completed',
  SESSION_FAILED = 'session.failed',
  AGENT_COMPLETED = 'agent.completed',
  CONTENT_GENERATED = 'content.generated',
  
  // System notifications
  SYSTEM_NOTIFICATION = 'system.notification',
  ERROR_NOTIFICATION = 'error.notification'
}

export interface WebSocketSubscriptionRequest {
  type: 'subscribe';
  payload: {
    sessionId: string;
    eventTypes?: string[];
  };
}

export interface WebSocketProgressUpdate {
  type: 'session.progress' | 'agent.progress';
  sessionId: string;
  payload: {
    currentStage: number;
    totalStages: number;
    progressPercentage: number;
    message?: string;
    estimatedTimeRemaining?: number;
    agentType?: string;
  };
}

export interface WebSocketNotification {
  type: WebSocketMessageType;
  payload: {
    title: string;
    message: string;
    severity: 'info' | 'success' | 'warning' | 'error';
    actionUrl?: string;
    actionText?: string;
  };
}

export interface WebSocketAcknowledgment {
  type: 'ack';
  messageId: string;
  timestamp: string;
}