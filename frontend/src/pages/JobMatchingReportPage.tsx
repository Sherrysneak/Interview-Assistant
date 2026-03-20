import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { jobMatchingApi } from '../api/jobMatching';
import { getErrorMessage } from '../api/request';
import type { JobMatchingReport } from '../types/jobMatching';
import ConfirmDialog from '../components/ConfirmDialog';
import { motion } from 'framer-motion';
import { ArrowLeft, PlayCircle } from 'lucide-react';

const severityColor: Record<string, string> = {
  CRITICAL: 'text-red-600 bg-red-50 border-red-100',
  IMPORTANT: 'text-amber-600 bg-amber-50 border-amber-100',
  NICE_TO_HAVE: 'text-emerald-600 bg-emerald-50 border-emerald-100',
};

/**
 * 岗位匹配报告页。
 * 展示评分、证据链、技能差距以及学习路径入口。
 */
export default function JobMatchingReportPage() {
  const { matchingId } = useParams<{ matchingId: string }>();
  const navigate = useNavigate();
  const [report, setReport] = useState<JobMatchingReport | null>(null);
  const [showStartInterviewDialog, setShowStartInterviewDialog] = useState(false);
  const [startInterviewLoading, setStartInterviewLoading] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const handleBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }
    if (report?.jdId) {
      navigate(`/job-descriptions/${report.jdId}`);
      return;
    }
    navigate('/job-target');
  };

  useEffect(() => {
    if (!matchingId) return;
    jobMatchingApi.getMatching(Number(matchingId))
      .then((data) => setReport(data))
      .catch((e) => setError(getErrorMessage(e)))
      .finally(() => setLoading(false));
  }, [matchingId]);

  /**
   * 从当前匹配报告直接发起 JD+简历联动面试。
   */
  const generateInterview = async () => {
    if (!report) return;

    setStartInterviewLoading(true);
    try {
      const session = await jobMatchingApi.generateInterview(report.matchingId, { questionCount: 10 });

      setShowStartInterviewDialog(false);
      navigate(`/interview/${report.resumeId}`, {
        state: {
          interviewSessionId: session.sessionId,
          from: `/job-target/report/${report.matchingId}`,
          jobContext: {
            jdId: report.jdId,
            jobTitle: '目标岗位',
            jdSummary: report.matchSummary,
            requiredSkills: report.skillGaps.map((g) => g.skillName),
            focusSkills: report.skillGaps
              .filter((g) => g.severity !== 'NICE_TO_HAVE')
              .map((g) => g.skillName),
          },
        },
      });
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setStartInterviewLoading(false);
    }
  };

  if (loading) {
    return <div className="text-slate-500">加载中...</div>;
  }

  if (!report) {
    return <div className="text-red-600">{error || '报告不存在'}</div>;
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <div className="bg-white rounded-2xl p-6 border border-slate-200">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-3">
            <button
              onClick={handleBack}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4" />
              返回上一级
            </button>
            <h1 className="text-2xl font-bold text-slate-900">岗位匹配报告</h1>
          </div>
          <motion.button
            onClick={() => setShowStartInterviewDialog(true)}
            className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl font-medium shadow-lg shadow-primary-500/30 hover:shadow-xl transition-all flex items-center gap-2 cursor-pointer"
            whileHover={{ scale: 1.02, y: -1 }}
            whileTap={{ scale: 0.98 }}
          >
            <PlayCircle className="w-4 h-4" />
            开始模拟
          </motion.button>
        </div>
        <div className="mt-4 grid grid-cols-2 md:grid-cols-5 gap-3">
          <ScoreCard label="总分" value={report.overallScore} />
          <ScoreCard label="技能" value={report.skillMatchScore} />
          <ScoreCard label="经验" value={report.experienceMatchScore} />
          <ScoreCard label="项目" value={report.projectMatchScore} />
          <ScoreCard label="学历" value={report.educationMatchScore} />
        </div>
        <p className="text-slate-700 mt-4">{report.matchSummary}</p>
      </div>

      <div className="bg-white rounded-2xl p-6 border border-slate-200">
        <h2 className="text-xl font-semibold text-slate-900 mb-3">证据链</h2>
        <ul className="list-disc pl-5 text-slate-700 space-y-2">
          {report.evidenceChains.map((item, idx) => <li key={idx}>{item}</li>)}
        </ul>
      </div>

      <div className="bg-white rounded-2xl p-6 border border-slate-200">
        <h2 className="text-xl font-semibold text-slate-900 mb-3">技能差距分级</h2>
        <div className="space-y-3">
          {report.skillGaps.map((gap, idx) => (
            <div key={`${gap.skillName}-${idx}`} className="p-4 border rounded-xl border-slate-200">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-slate-900">{gap.skillName}</span>
                <span className={`text-xs px-2 py-1 rounded-md border ${severityColor[gap.severity]}`}>{gap.severity}</span>
              </div>
              <p className="text-sm text-slate-600 mt-2">JD 证据：{gap.jdEvidence || '无'}</p>
              <p className="text-sm text-slate-600">简历现状：{gap.resumeEvidence || '无'}</p>
              <p className="text-sm text-slate-700 mt-1">行动建议：{gap.actionSuggestion || '无'}</p>
            </div>
          ))}
        </div>
      </div>

      {report.learningPath && (
        <div className="bg-white rounded-2xl p-6 border border-slate-200">
          <h2 className="text-xl font-semibold text-slate-900">学习路径</h2>
          <p className="text-slate-600 mt-2">{report.learningPath.targetGoal}</p>
          <p className="text-slate-600 mt-1">周期 {report.learningPath.durationWeeks} 周，每周 {report.learningPath.estimatedHoursPerWeek} 小时</p>
          <button onClick={() => navigate(`/job-target/report/${report.matchingId}/learning`)} className="mt-4 px-4 py-2 rounded-lg bg-slate-900 text-white">
            查看并打卡学习路径
          </button>
        </div>
      )}

      <div className="flex gap-3">
        <button onClick={() => navigate('/job-target')} className="px-5 py-2.5 rounded-lg bg-slate-100 text-slate-700 cursor-pointer">新建目标岗位</button>
      </div>

      {error && <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl border border-red-100">{error}</div>}

      <ConfirmDialog
        open={showStartInterviewDialog}
        title="开始模拟面试"
        message="将直接使用当前匹配记录关联的 JD 与简历。"
        confirmText="开始模拟"
        cancelText="取消"
        confirmVariant="primary"
        loading={startInterviewLoading}
        onConfirm={generateInterview}
        onCancel={() => setShowStartInterviewDialog(false)}
        customContent={<></>}
      />
    </div>
  );
}

/**
 * 分数字段展示卡片。
 */
function ScoreCard({ label, value }: { label: string; value: number | null }) {
  return (
    <div className="rounded-xl bg-slate-50 p-3 border border-slate-200">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="text-2xl font-bold text-slate-900 mt-1">{value ?? '-'}</p>
    </div>
  );
}
