import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { jobMatchingApi } from '../api/jobMatching';
import { getErrorMessage } from '../api/request';
import type { JobMatchingReport, WeeklyPlan } from '../types/jobMatching';
import { ArrowLeft } from 'lucide-react';

/**
 * 学习路径执行页。
 * 展示周计划、任务打卡与资源推荐。
 */
export default function LearningPathPage() {
  const { matchingId } = useParams<{ matchingId: string }>();
  const navigate = useNavigate();
  const [report, setReport] = useState<JobMatchingReport | null>(null);
  const [error, setError] = useState('');

  const handleBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }
    if (matchingId) {
      navigate(`/job-target/report/${matchingId}`);
      return;
    }
    navigate('/job-target');
  };

  useEffect(() => {
    if (!matchingId) return;
    jobMatchingApi.getMatching(Number(matchingId))
      .then(setReport)
      .catch((e) => setError(getErrorMessage(e)));
  }, [matchingId]);

  /**
   * 标记任务完成并刷新路径进度。
   */
  const toggleTask = async (plan: WeeklyPlan, taskIndex: number) => {
    try {
      const path = await jobMatchingApi.completeTask(plan.planId, taskIndex);
      if (!report) return;
      setReport({ ...report, learningPath: path });
    } catch (e) {
      setError(getErrorMessage(e));
    }
  };

  if (!report?.learningPath) {
    return <div className="text-slate-500">{error || '学习路径加载中...'}</div>;
  }

  const path = report.learningPath;

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <div className="bg-white rounded-2xl p-6 border border-slate-200">
        <div className="flex items-center gap-3">
          <button
            onClick={handleBack}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4" />
            返回上一级
          </button>
          <h1 className="text-2xl font-bold text-slate-900">学习路径执行</h1>
        </div>
        <p className="text-slate-600 mt-2">{path.targetGoal}</p>
        <p className="text-slate-600">整体进度：{path.progressPercentage}%</p>
      </div>

      {path.weeklyPlans.map((plan) => (
        <div key={plan.planId} className="bg-white rounded-2xl p-6 border border-slate-200">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">第 {plan.weekNumber} 周：{plan.title}</h2>
            <span className="text-sm text-slate-500">完成度 {plan.completionPercentage}%</span>
          </div>

          <Section title="目标" items={plan.objectives} />
          <Section title="产出物" items={plan.deliverables} />
          <Section title="验收标准" items={plan.acceptanceCriteria} />

          <div className="mt-4">
            <h3 className="font-medium text-slate-900 mb-2">任务打卡</h3>
            <div className="space-y-2">
              {plan.tasks.map((task, idx) => (
                <button
                  key={`${plan.planId}-${idx}`}
                  onClick={() => toggleTask(plan, idx)}
                  className="w-full text-left px-3 py-2 rounded-lg border border-slate-200 hover:bg-slate-50"
                >
                  ✓ {task}
                </button>
              ))}
            </div>
          </div>

          {plan.resources.length > 0 && (
            <div className="mt-4">
              <h3 className="font-medium text-slate-900 mb-2">推荐资源</h3>
              <div className="space-y-2">
                {plan.resources.map((res, idx) => (
                  <a
                    key={`${plan.planId}-res-${idx}`}
                    href={res.url}
                    target="_blank"
                    rel="noreferrer"
                    className="block px-3 py-2 rounded-lg border border-slate-200 hover:bg-slate-50"
                  >
                    <p className="text-slate-900">{res.title}</p>
                    <p className="text-xs text-slate-500">{res.type} · {res.platform}</p>
                  </a>
                ))}
              </div>
            </div>
          )}
        </div>
      ))}

      {error && <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl border border-red-100">{error}</div>}
    </div>
  );
}

/**
 * 简单列表区块渲染组件。
 */
function Section({ title, items }: { title: string; items: string[] }) {
  if (!items.length) return null;
  return (
    <div className="mt-4">
      <h3 className="font-medium text-slate-900 mb-2">{title}</h3>
      <ul className="list-disc pl-5 text-slate-700 space-y-1">
        {items.map((item, idx) => <li key={`${title}-${idx}`}>{item}</li>)}
      </ul>
    </div>
  );
}
