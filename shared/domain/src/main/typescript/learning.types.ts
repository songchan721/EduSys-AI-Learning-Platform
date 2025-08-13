// TypeScript interfaces for Learning domain - Frontend-Backend communication

export interface LearningSessionDto {
  id: string;
  userId: string;
  topic: string;
  status: SessionStatus;
  estimatedDurationMinutes?: number;
  actualDurationMinutes?: number;
  qualityScore?: number; // 0.0 to 1.0
  costUsd?: number;
  createdAt: string; // ISO 8601 date string
  completedAt?: string; // ISO 8601 date string
  agentExecutions: AgentExecutionDto[];
  progressPercentage: number; // 0 to 100
  currentStage: number; // Current agent stage (1-8)
}

export interface AgentExecutionDto {
  id: string;
  sessionId: string;
  agentType: AgentType;
  stageNumber: number;
  status: ExecutionStatus;
  inputData?: any; // JSON data
  outputData?: any; // JSON data
  errorMessage?: string;
  llmProvider?: string;
  llmModel?: string;
  tokensUsed?: number;
  costUsd?: number;
  startedAt?: string; // ISO 8601 date string
  completedAt?: string; // ISO 8601 date string
  createdAt: string;
  durationMinutes?: number;
}

export interface AgentCheckpointDto {
  id: string;
  executionId: string;
  checkpointData: any; // JSON data
  createdAt: string;
}

export enum SessionStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export enum ExecutionStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export enum AgentType {
  RESEARCH = 'RESEARCH',
  VERIFICATION = 'VERIFICATION',
  DECOMPOSITION = 'DECOMPOSITION',
  STRUCTURING = 'STRUCTURING',
  CONTENT_GENERATION = 'CONTENT_GENERATION',
  VALIDATION = 'VALIDATION',
  SYNTHESIS = 'SYNTHESIS',
  LEARNING_EXPERIENCE = 'LEARNING_EXPERIENCE'
}

// Request/Response DTOs for API endpoints
export interface CreateLearningSessionRequest {
  topic: string;
  agentConfiguration?: AgentConfigurationRequest;
  priority?: SessionPriority;
}

export interface AgentConfigurationRequest {
  llmProvider?: string;
  llmModel?: string;
  customParameters?: Record<string, any>;
}

export enum SessionPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH'
}

export interface CreateLearningSessionResponse {
  success: boolean;
  data: {
    session: LearningSessionDto;
    websocketUrl: string;
    estimatedCompletionTime: string; // ISO 8601 date string
  };
  meta: ResponseMeta;
}

export interface SessionProgressUpdate {
  sessionId: string;
  currentStage: number;
  totalStages: number;
  progressPercentage: number;
  currentAgentType: AgentType;
  estimatedTimeRemaining?: number; // minutes
  message?: string;
  timestamp: string;
}

export interface AgentProgressUpdate {
  sessionId: string;
  executionId: string;
  agentType: AgentType;
  stageNumber: number;
  status: ExecutionStatus;
  progressPercentage: number;
  message?: string;
  intermediateResults?: any;
  timestamp: string;
}

export interface SessionListResponse {
  success: boolean;
  data: {
    sessions: LearningSessionDto[];
    pagination: PaginationInfo;
  };
  meta: ResponseMeta;
}

export interface PaginationInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ResponseMeta {
  timestamp: string;
  correlationId: string;
  version: string;
}