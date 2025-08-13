// Multi-Agent Learning Platform - TypeScript Type Definitions
// Frontend-Backend Communication Interfaces

// User domain exports
export * from './user.types';

// Learning domain exports
export * from './learning.types';

// Content domain exports
export * from './content.types';

// Event system exports
export * from './events.types';

// WebSocket communication exports
export * from './websocket.types';

// Common types and utilities
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: ApiError;
  meta: ResponseMeta;
}

export interface ApiError {
  code: string;
  message: string;
  details?: ErrorDetail[];
}

export interface ErrorDetail {
  field: string;
  code: string;
  message: string;
}

export interface ResponseMeta {
  timestamp: string;
  correlationId: string;
  version: string;
}

// Pagination utilities
export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

export interface PageResponse<T> {
  content: T[];
  pagination: PaginationInfo;
}

export interface PaginationInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// Filter utilities
export interface FilterCriteria {
  field: string;
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'like' | 'in' | 'between';
  value: any;
}

export interface SortCriteria {
  field: string;
  direction: 'asc' | 'desc';
}

// Date utilities
export type DateString = string; // ISO 8601 format
export type TimestampString = string; // ISO 8601 format with timezone

// ID types for type safety
export type UserId = string;
export type SessionId = string;
export type ContentId = string;
export type ExecutionId = string;
export type EventId = string;

// Configuration types
export interface LLMConfiguration {
  provider: string;
  model: string;
  apiKey?: string; // Encrypted on backend
  parameters?: Record<string, any>;
}

export interface AgentConfiguration {
  agentType: string;
  llmConfiguration: LLMConfiguration;
  customParameters?: Record<string, any>;
}

// Health check types
export interface HealthStatus {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  components: Record<string, ComponentHealth>;
  timestamp: string;
}

export interface ComponentHealth {
  status: 'UP' | 'DOWN';
  details?: Record<string, any>;
}

// Metrics types
export interface SystemMetrics {
  timestamp: string;
  cpu: {
    usage: number; // percentage
    cores: number;
  };
  memory: {
    used: number; // bytes
    total: number; // bytes
    usage: number; // percentage
  };
  disk: {
    used: number; // bytes
    total: number; // bytes
    usage: number; // percentage
  };
  network: {
    bytesIn: number;
    bytesOut: number;
  };
}

export interface ApplicationMetrics {
  timestamp: string;
  activeUsers: number;
  activeSessions: number;
  completedSessions: number;
  failedSessions: number;
  averageSessionDuration: number; // minutes
  totalContentGenerated: number;
  averageQualityScore: number;
}

// Validation utilities
export interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
}

export interface ValidationError {
  field: string;
  message: string;
  code: string;
}

// Constants
export const API_VERSION = 'v1';
export const DEFAULT_PAGE_SIZE = 20;
export const MAX_PAGE_SIZE = 100;

// Type guards
export function isApiResponse<T>(obj: any): obj is ApiResponse<T> {
  return obj && typeof obj === 'object' && 'success' in obj && 'meta' in obj;
}

export function isApiError(obj: any): obj is ApiError {
  return obj && typeof obj === 'object' && 'code' in obj && 'message' in obj;
}

export function isValidUUID(str: string): boolean {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  return uuidRegex.test(str);
}