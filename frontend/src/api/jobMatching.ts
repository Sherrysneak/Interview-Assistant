import { request } from './request';
import type {
  CreateJobMatchingRequest,
  JobDescription,
  JobMatchingRecord,
  JobMatchingReport,
  LearningPath
} from '../types/jobMatching';
import type { InterviewSession } from '../types/interview';

/**
 * 岗位匹配模块 API。
 */
export const jobMatchingApi = {
  /** 文本方式创建 JD。 */
  createJdFromText(payload: { jdText: string; jobTitleHint?: string; companyNameHint?: string }) {
    return request.post<JobDescription>('/api/job-descriptions/from-text', payload);
  },

  /** URL 方式创建 JD。 */
  createJdFromUrl(payload: { url: string }) {
    return request.post<JobDescription>('/api/job-descriptions/from-url', payload);
  },

  /** 文件方式创建 JD。 */
  createJdFromFile(file: File) {
    const form = new FormData();
    form.append('file', file);
    return request.upload<JobDescription>('/api/job-descriptions/from-file', form);
  },

  /** 创建匹配分析并触发学习路径生成。 */
  createMatching(payload: CreateJobMatchingRequest) {
    return request.post<JobMatchingRecord>('/api/job-matchings', payload);
  },

  /** 获取匹配报告。 */
  getMatching(id: number) {
    return request.get<JobMatchingReport>(`/api/job-matchings/${id}`);
  },

  /** 获取 JD 库列表。 */
  listJobDescriptions() {
    return request.get<JobDescription[]>('/api/job-descriptions');
  },

  /** 获取 JD 详情。 */
  getJobDescription(id: number) {
    return request.get<JobDescription>(`/api/job-descriptions/${id}`);
  },

  /** 获取某个 JD 的匹配记录列表。 */
  listMatchingsByJd(jdId: number) {
    return request.get<JobMatchingRecord[]>(`/api/job-descriptions/${jdId}/matchings`);
  },

  /** 软删除 JD。 */
  deleteJobDescription(id: number) {
    return request.delete<void>(`/api/job-descriptions/${id}`);
  },

  /** 重试解析 JD。 */
  retryJobDescription(id: number) {
    return request.post<JobDescription>(`/api/job-descriptions/${id}/retry`);
  },

  /** 学习任务打卡。 */
  completeTask(planId: number, taskIndex: number, notes?: string) {
    return request.post<LearningPath>(`/api/learning-paths/plans/${planId}/tasks/${taskIndex}/complete`, {
      notes,
    });
  },

  /** 基于岗位匹配结果创建联动面试。 */
  generateInterview(matchingId: number, payload?: { questionCount?: number; forceCreate?: boolean }) {
    return request.post<InterviewSession>(`/api/job-matchings/${matchingId}/generate-interview`, payload ?? {});
  },
};
