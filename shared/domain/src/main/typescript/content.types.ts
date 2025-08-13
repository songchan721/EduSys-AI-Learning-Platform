// TypeScript interfaces for Content domain - Frontend-Backend communication

export interface GeneratedContentDto {
  id: string;
  sessionId: string;
  contentType: ContentType;
  title: string;
  contentData: any; // JSON data - structure depends on content type
  filePath?: string; // S3 path for large content files
  version: number;
  qualityScore?: number; // 0.0 to 1.0
  wordCount?: number;
  estimatedReadingTimeMinutes?: number;
  createdAt: string; // ISO 8601 date string
  updatedAt: string;
  metadata: Record<string, string>; // Key-value metadata pairs
}

export interface ContentVersionDto {
  id: string;
  contentId: string;
  versionNumber: number;
  contentData: any; // JSON data
  changesDescription?: string;
  createdAt: string;
}

export interface ContentMetadataDto {
  id: string;
  contentId: string;
  metadataKey: string;
  metadataValue: string;
  createdAt: string;
}

export enum ContentType {
  TEXT = 'TEXT',
  DIAGRAM = 'DIAGRAM',
  EXERCISE = 'EXERCISE',
  QUIZ = 'QUIZ',
  ASSESSMENT = 'ASSESSMENT',
  FLASHCARD = 'FLASHCARD',
  MIND_MAP = 'MIND_MAP',
  TUTORIAL = 'TUTORIAL',
  CODE_EXAMPLE = 'CODE_EXAMPLE',
  FORMULA = 'FORMULA'
}

// Specific content data structures
export interface TextContentData {
  text: string;
  format: 'markdown' | 'html' | 'plain';
  sections?: ContentSection[];
  references?: Reference[];
}

export interface ContentSection {
  id: string;
  title: string;
  content: string;
  level: number; // Heading level (1-6)
  order: number;
}

export interface Reference {
  id: string;
  title: string;
  url?: string;
  author?: string;
  publicationDate?: string;
  credibilityScore?: number; // 0.0 to 1.0
}

export interface DiagramContentData {
  diagramType: 'mermaid' | 'plantuml' | 'svg' | 'image';
  diagramCode?: string; // For mermaid/plantuml
  imageUrl?: string; // For static images
  description: string;
  alt: string; // Accessibility description
}

export interface QuizContentData {
  questions: QuizQuestion[];
  timeLimit?: number; // minutes
  passingScore?: number; // percentage
  instructions?: string;
}

export interface QuizQuestion {
  id: string;
  question: string;
  type: 'multiple_choice' | 'true_false' | 'short_answer' | 'essay';
  options?: string[]; // For multiple choice
  correctAnswer?: string | string[]; // Can be multiple for multi-select
  explanation?: string;
  points: number;
  difficulty: 'easy' | 'medium' | 'hard';
}

export interface ExerciseContentData {
  title: string;
  description: string;
  instructions: string[];
  expectedOutput?: string;
  hints?: string[];
  solution?: string;
  difficulty: 'beginner' | 'intermediate' | 'advanced';
  estimatedTimeMinutes: number;
}

export interface CodeExampleContentData {
  language: string;
  code: string;
  description: string;
  explanation?: string;
  runnable: boolean;
  expectedOutput?: string;
}

// Request/Response DTOs for API endpoints
export interface ContentSearchRequest {
  query?: string;
  sessionId?: string;
  contentTypes?: ContentType[];
  page?: number;
  size?: number;
  sortBy?: 'createdAt' | 'updatedAt' | 'qualityScore' | 'title';
  sortDirection?: 'asc' | 'desc';
}

export interface ContentSearchResponse {
  success: boolean;
  data: {
    content: GeneratedContentDto[];
    pagination: PaginationInfo;
    facets: SearchFacets;
  };
  meta: ResponseMeta;
}

export interface SearchFacets {
  contentTypes: FacetCount[];
  qualityScores: FacetRange[];
  sessions: FacetCount[];
}

export interface FacetCount {
  value: string;
  count: number;
}

export interface FacetRange {
  min: number;
  max: number;
  count: number;
}

export interface ContentFeedbackRequest {
  contentId: string;
  rating: number; // 1-5 stars
  feedback?: string;
  helpful: boolean;
}

export interface ContentFeedbackResponse {
  success: boolean;
  data: {
    averageRating: number;
    totalRatings: number;
    helpfulCount: number;
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