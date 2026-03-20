import { useEffect, useState, type MouseEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { jobMatchingApi } from '../api/jobMatching';
import { historyApi } from '../api/history';
import { getErrorMessage } from '../api/request';
import type { ResumeListItem } from '../api/history';
import type { JobDescription, JobMatchingRecord } from '../types/jobMatching';
import ConfirmDialog from '../components/ConfirmDialog';
import { AnimatePresence, motion } from 'framer-motion';
import { Briefcase, CheckSquare, ChevronLeft, FileSearch, MessageSquare, PlayCircle } from 'lucide-react';

type TabType = 'analysis' | 'records';

/**
 * JD 详情页。
 */
export default function JobDescriptionDetailPage() {
  const { jdId } = useParams<{ jdId: string }>();
  const navigate = useNavigate();
  const [jd, setJd] = useState<JobDescription | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('analysis');
  const [[page, direction], setPage] = useState([0, 0]);
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [selectedResumeId, setSelectedResumeId] = useState<number | null>(null);
  const [durationWeeks, setDurationWeeks] = useState(4);
  const [questionCount, setQuestionCount] = useState(8);
  const [matchings, setMatchings] = useState<JobMatchingRecord[]>([]);
  const [matchingsLoading, setMatchingsLoading] = useState(false);
  const [startInterviewTarget, setStartInterviewTarget] = useState<JobMatchingRecord | null>(null);
  const [showMatchingDialog, setShowMatchingDialog] = useState(false);
  const [showStartInterviewDialog, setShowStartInterviewDialog] = useState(false);
  const [matchingLoading, setMatchingLoading] = useState(false);
  const [startInterviewLoading, setStartInterviewLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [successMessage, setSuccessMessage] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    if (!jdId) return;
    try {
      const [detail, resumeList] = await Promise.all([
        jobMatchingApi.getJobDescription(Number(jdId)),
        historyApi.getResumes(),
      ]);
      setJd(detail);
      setResumes(resumeList);
      setSelectedResumeId((prev) => prev ?? (resumeList[0]?.id ?? null));
      setError('');
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  const loadMatchings = async (silent = false) => {
    if (!jdId) return;
    if (!silent) {
      setMatchingsLoading(true);
    }
    try {
      const records = await jobMatchingApi.listMatchingsByJd(Number(jdId));
      setMatchings(records);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      if (!silent) {
        setMatchingsLoading(false);
      }
    }
  };

  useEffect(() => {
    load();
    loadMatchings();
  }, [jdId]);

  useEffect(() => {
    if (!jd || (jd.parseStatus !== 'PENDING' && jd.parseStatus !== 'PROCESSING')) {
      return;
    }
    const timer = window.setInterval(load, 3000);
    return () => window.clearInterval(timer);
  }, [jd]);

  useEffect(() => {
    if (!matchings.some((item) => item.status === 'PENDING' || item.status === 'PROCESSING')) {
      return;
    }
    const timer = window.setInterval(() => loadMatchings(true), 4000);
    return () => window.clearInterval(timer);
  }, [matchings]);

  useEffect(() => {
    if (!successMessage) {
      return;
    }
    const timer = window.setTimeout(() => setSuccessMessage(''), 5000);
    return () => window.clearTimeout(timer);
  }, [successMessage]);

  const handleTabChange = (tab: TabType) => {
    const newPage = tab === 'analysis' ? 0 : 1;
    setPage([newPage, newPage > page ? 1 : -1]);
    setActiveTab(tab);
  };

  const retry = async () => {
    if (!jd) return;
    try {
      const updated = await jobMatchingApi.retryJobDescription(jd.id);
      setJd(updated);
      setError('');
    } catch (e) {
      setError(getErrorMessage(e));
    }
  };

  const startMatchingAnalysis = async () => {
    if (!jd) return;
    if (!selectedResumeId) {
      setError('请先选择一份简历再开始匹配分析');
      return;
    }

    setMatchingLoading(true);
    setError('');
    try {
      await jobMatchingApi.createMatching({
        jdId: jd.id,
        resumeId: selectedResumeId,
        durationWeeks,
      });
      setShowMatchingDialog(false);
      setSuccessMessage('匹配分析任务已提交，正在后台异步执行，可切换页面继续操作。');
      handleTabChange('records');
      loadMatchings(true);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setMatchingLoading(false);
    }
  };

  const handleOpenStartInterview = (record: JobMatchingRecord, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    if (record.status !== 'COMPLETED') {
      return;
    }
    setStartInterviewTarget(record);
    setQuestionCount(8);
    setShowStartInterviewDialog(true);
  };

  const startInterviewFromRecord = async () => {
    if (!jd || !startInterviewTarget) {
      return;
    }

    setStartInterviewLoading(true);
    setError('');
    try {
      const report = await jobMatchingApi.getMatching(startInterviewTarget.matchingId);
      const session = await jobMatchingApi.generateInterview(startInterviewTarget.matchingId, { questionCount });
      setShowStartInterviewDialog(false);
      navigate(`/interview/${startInterviewTarget.resumeId}`, {
        state: {
          interviewSessionId: session.sessionId,
          from: `/job-descriptions/${jd.id}`,
          jobContext: {
            jdId: report.jdId,
            jobTitle: jd.jobTitle,
            jdSummary: report.matchSummary || jd.coreRequirements || '',
            requiredSkills: jd.technicalSkills || [],
            focusSkills: report.skillGaps
              .filter((gap) => gap.severity !== 'NICE_TO_HAVE')
              .map((gap) => gap.skillName),
          },
        },
      });
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setStartInterviewLoading(false);
    }
  };

  const tabs = [
    { id: 'analysis' as const, label: 'JD 分析结果', icon: CheckSquare },
    { id: 'records' as const, label: '匹配记录', icon: MessageSquare, count: matchings.length },
  ];

  const slideVariants = {
    enter: (dir: number) => ({
      x: dir > 0 ? 300 : -300,
      opacity: 0,
    }),
    center: {
      x: 0,
      opacity: 1,
    },
    exit: (dir: number) => ({
      x: dir < 0 ? 300 : -300,
      opacity: 0,
    }),
  };

  if (!jd) {
    return <div className="text-slate-500">{loading ? '加载中...' : (error || '未找到 JD 详情')}</div>;
  }

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="bg-white rounded-2xl p-6 border border-slate-200">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-4">
            <motion.button
              onClick={() => navigate('/job-target')}
              className="w-10 h-10 bg-white rounded-xl flex items-center justify-center text-slate-500 hover:bg-slate-50 hover:text-slate-700 transition-all shadow-sm cursor-pointer"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              <ChevronLeft className="w-5 h-5" />
            </motion.button>
            <div>
              <h1 className="text-2xl font-bold text-slate-900">{jd.jobTitle || '未命名岗位'}</h1>
              <p className="text-slate-500 mt-1">{jd.companyName || '未知公司'} · {statusText(jd.parseStatus)}</p>
            </div>
          </div>
          <div className="space-x-2 flex items-center">
            {jd.parseStatus === 'FAILED' && (
              <button onClick={retry} className="px-4 py-2 rounded-lg bg-amber-500 text-white cursor-pointer">重试解析</button>
            )}
            <motion.button
              onClick={() => setShowMatchingDialog(true)}
              disabled={jd.parseStatus !== 'COMPLETED'}
              className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl font-medium shadow-lg shadow-primary-500/30 hover:shadow-xl transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
              whileHover={{ scale: jd.parseStatus === 'COMPLETED' ? 1.02 : 1, y: jd.parseStatus === 'COMPLETED' ? -1 : 0 }}
              whileTap={{ scale: jd.parseStatus === 'COMPLETED' ? 0.98 : 1 }}
            >
              <PlayCircle className="w-4 h-4" />
              简历匹配分析
            </motion.button>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-2 mt-6 inline-flex gap-1 border border-slate-100">
          {tabs.map((tab) => (
            <motion.button
              key={tab.id}
              onClick={() => handleTabChange(tab.id)}
              className={`relative px-5 py-2.5 rounded-xl font-medium flex items-center gap-2 transition-colors cursor-pointer ${activeTab === tab.id ? 'text-primary-600' : 'text-slate-500 hover:text-slate-700'}`}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              {activeTab === tab.id && (
                <motion.div
                  layoutId="activeJdTab"
                  className="absolute inset-0 bg-primary-50 rounded-xl"
                  transition={{ type: 'spring', bounce: 0.2, duration: 0.6 }}
                />
              )}
              <span className="relative z-10 flex items-center gap-2">
                <tab.icon className="w-4 h-4" />
                {tab.label}
                {tab.count !== undefined && tab.count > 0 && (
                  <span className="px-2 py-0.5 bg-primary-100 text-primary-600 text-xs rounded-full">{tab.count}</span>
                )}
              </span>
            </motion.button>
          ))}
        </div>
      </div>

      {(jd.parseStatus === 'PENDING' || jd.parseStatus === 'PROCESSING') && (
        <div className="bg-indigo-50 text-indigo-700 px-4 py-3 rounded-xl border border-indigo-100">
          后台正在异步解析，可继续上传其他 JD。当前状态：{statusText(jd.parseStatus)}
        </div>
      )}

      {jd.parseStatus === 'FAILED' && (
        <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl border border-red-100">
          解析失败：{jd.parseError || '未知错误'}
        </div>
      )}

      {successMessage && (
        <div className="bg-emerald-50 text-emerald-700 px-4 py-3 rounded-xl border border-emerald-100">{successMessage}</div>
      )}

      <div className="relative overflow-hidden">
        <AnimatePresence initial={false} custom={direction} mode="wait">
          <motion.div
            key={activeTab}
            custom={direction}
            variants={slideVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ type: 'spring', stiffness: 300, damping: 30 }}
          >
            {activeTab === 'analysis' ? (
              <div className="bg-white rounded-2xl p-6 border border-slate-200 space-y-3">
                <h2 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
                  <FileSearch className="w-5 h-5 text-primary-500" />
                  JD 分析结果
                </h2>

                <div className="rounded-xl border border-slate-200 p-4 bg-slate-50/60">
                  <p className="text-sm text-slate-500 mb-1">技能要求</p>
                  <p className="text-slate-800">{(jd.technicalSkills || []).join('、') || '-'}</p>
                </div>

                <div className="rounded-xl border border-slate-200 p-4 bg-slate-50/60">
                  <p className="text-sm text-slate-500 mb-1">核心要求</p>
                  <p className="text-slate-800 whitespace-pre-wrap">{jd.coreRequirements || '-'}</p>
                </div>

                <div className="rounded-xl border border-slate-200 p-4 bg-slate-50/60">
                  <p className="text-sm text-slate-500 mb-1">岗位职责</p>
                  <p className="text-slate-800 whitespace-pre-wrap">{jd.responsibilities || '-'}</p>
                </div>
              </div>
            ) : (
              <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
                <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-slate-900">匹配记录</h2>
                  {matchingsLoading && <span className="text-sm text-slate-400">加载中...</span>}
                </div>
                {matchings.length === 0 ? (
                  <div className="px-5 py-10 text-center text-slate-500">暂无匹配记录，先发起一次简历匹配分析。</div>
                ) : (
                  <div className="divide-y divide-slate-100">
                    {matchings.map((item) => (
                      <div
                        key={item.matchingId}
                        onClick={() => item.status === 'COMPLETED' && navigate(`/job-target/report/${item.matchingId}`)}
                        className={`px-5 py-4 transition-colors group ${item.status === 'COMPLETED' ? 'hover:bg-slate-50 cursor-pointer' : ''}`}
                      >
                        <div className="flex items-center gap-4 justify-between">
                          <div className="min-w-0">
                            <div className="text-slate-900 font-medium truncate">{item.resumeFilename || '未命名简历'}</div>
                            <div className="text-xs text-slate-500 mt-1">创建时间：{new Date(item.createdAt).toLocaleString()}</div>
                            {item.status === 'FAILED' && item.error && (
                              <div className="text-xs text-red-500 mt-1 truncate">失败原因：{item.error}</div>
                            )}
                          </div>
                          <div className="flex items-center gap-3">
                            <span className={matchingStatusClass(item.status)}>{matchingStatusText(item.status)}</span>
                            <span className="text-slate-700 font-semibold w-14 text-right">{item.overallScore ?? '-'} 分</span>
                            <button
                              onClick={(event) => handleOpenStartInterview(item, event)}
                              disabled={item.status !== 'COMPLETED'}
                              className="px-3 py-1.5 rounded-lg bg-primary-600 text-white text-sm font-medium cursor-pointer hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              开始模拟面试
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </motion.div>
        </AnimatePresence>
      </div>

      {error && <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl border border-red-100">{error}</div>}

      <ConfirmDialog
        open={showMatchingDialog}
        title="选择简历并开始匹配分析"
        message=""
        confirmText="开始分析"
        cancelText="取消"
        confirmVariant="primary"
        loading={matchingLoading}
        onConfirm={startMatchingAnalysis}
        onCancel={() => setShowMatchingDialog(false)}
        customContent={(
          <div className="space-y-4">
            <div>
              <label className="text-sm text-slate-600 mb-1 block">选择简历</label>
              <div className="relative">
                <Briefcase className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <select
                  value={selectedResumeId ?? ''}
                  onChange={(e) => setSelectedResumeId(e.target.value ? Number(e.target.value) : null)}
                  className="w-full pl-9 pr-3 py-3 rounded-lg border border-slate-200"
                >
                  <option value="">请选择一份简历</option>
                  {resumes.map((resume) => (
                    <option key={resume.id} value={resume.id}>{resume.filename}</option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="text-sm text-slate-600 mb-1 block">学习周期（周）</label>
              <select
                value={durationWeeks}
                onChange={(e) => setDurationWeeks(Number(e.target.value))}
                className="w-full px-3 py-3 rounded-lg border border-slate-200"
              >
                <option value={3}>3 周</option>
                <option value={4}>4 周</option>
                <option value={5}>5 周</option>
                <option value={6}>6 周</option>
              </select>
            </div>
          </div>
        )}
      />

      <ConfirmDialog
        open={showStartInterviewDialog}
        title="开始模拟面试"
        message="将默认使用该匹配记录关联的 JD 和简历，仅需配置题目数量。"
        confirmText="开始模拟"
        cancelText="取消"
        confirmVariant="primary"
        loading={startInterviewLoading}
        onConfirm={startInterviewFromRecord}
        onCancel={() => {
          setShowStartInterviewDialog(false);
          setStartInterviewTarget(null);
        }}
        customContent={(
          <div className="space-y-2">
            <label className="text-sm text-slate-600 block">题目数量</label>
            <div className="grid grid-cols-5 gap-2">
              {[6, 8, 10, 12, 15].map((count) => (
                <button
                  key={count}
                  type="button"
                  onClick={() => setQuestionCount(count)}
                  className={`px-2 py-2 rounded-lg border text-sm cursor-pointer ${questionCount === count ? 'bg-primary-600 text-white border-primary-600' : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'}`}
                >
                  {count}
                </button>
              ))}
            </div>
          </div>
        )}
      />
    </div>
  );
}

function statusText(status: JobDescription['parseStatus']) {
  if (status === 'PENDING') return '排队中';
  if (status === 'PROCESSING') return '解析中';
  if (status === 'COMPLETED') return '已完成';
  return '失败';
}

function matchingStatusText(status: JobMatchingRecord['status']) {
  if (status === 'PENDING') return '排队中';
  if (status === 'PROCESSING') return '分析中';
  if (status === 'COMPLETED') return '已完成';
  return '失败';
}

function matchingStatusClass(status: JobMatchingRecord['status']) {
  if (status === 'PENDING') return 'px-2 py-1 text-xs rounded bg-blue-50 text-blue-700';
  if (status === 'PROCESSING') return 'px-2 py-1 text-xs rounded bg-indigo-50 text-indigo-700';
  if (status === 'COMPLETED') return 'px-2 py-1 text-xs rounded bg-emerald-50 text-emerald-700';
  return 'px-2 py-1 text-xs rounded bg-red-50 text-red-700';
}
