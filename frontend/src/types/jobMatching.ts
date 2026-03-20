/** 技能差距严重度。 */
export type GapSeverity = 'CRITICAL' | 'IMPORTANT' | 'NICE_TO_HAVE';

/** JD 结构化信息。 */
export interface JobDescription {
  id: number;
  jobTitle: string;
  companyName?: string;
  salaryRange?: string;
  workLocation?: string;
  experienceYears?: string;
  technicalSkills: string[];
  coreRequirements?: string;
  responsibilities?: string;
  bonusPoints?: string;
  sourceType: 'URL' | 'TEXT' | 'FILE';
  sourceUrl?: string;
  parseStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  parseError?: string;
  parseStartedAt?: string;
  parseFinishedAt?: string;
  parseRetryCount?: number;
  createdAt: string;
}

/** 单项技能差距。 */
export interface SkillGap {
  skillName: string;
  requiredLevel?: string;
  currentLevel?: string;
  severity: GapSeverity;
  jdEvidence?: string;
  resumeEvidence?: string;
  actionSuggestion?: string;
}

/** 学习资源信息。 */
export interface LearningResource {
  type: string;
  title: string;
  url: string;
  platform?: string;
  reason?: string;
}

/** 每周学习计划。 */
export interface WeeklyPlan {
  planId: number;
  weekNumber: number;
  title: string;
  objectives: string[];
  tasks: string[];
  deliverables: string[];
  acceptanceCriteria: string[];
  resources: LearningResource[];
  completionPercentage: number;
  completed: boolean;
}

/** 学习路径总览。 */
export interface LearningPath {
  id: number;
  targetGoal: string;
  durationWeeks: number;
  estimatedHoursPerWeek: number;
  progressPercentage: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'PAUSED';
  weeklyPlans: WeeklyPlan[];
}

/** 岗位匹配报告。 */
export interface JobMatchingReport {
  matchingId: number;
  resumeId: number;
  jdId: number;
  overallScore: number | null;
  skillMatchScore: number | null;
  experienceMatchScore: number | null;
  projectMatchScore: number | null;
  educationMatchScore: number | null;
  matchSummary: string | null;
  evidenceChains: string[];
  strengths: string[];
  improvementSuggestions: string[];
  skillGaps: SkillGap[];
  learningPath: LearningPath | null;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  error?: string;
}

/** 创建岗位匹配请求。 */
export interface CreateJobMatchingRequest {
  resumeId: number;
  jdId: number;
  durationWeeks?: number;
  questionCountForInterview?: number;
}

/** JD 维度的匹配记录项。 */
export interface JobMatchingRecord {
  matchingId: number;
  resumeId: number;
  resumeFilename: string;
  jdId: number;
  jobTitle: string;
  overallScore: number | null;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  error?: string;
  createdAt: string;
  completedAt?: string;
}
