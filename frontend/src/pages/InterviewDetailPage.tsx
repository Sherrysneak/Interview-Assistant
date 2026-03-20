import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { ChevronLeft, Download, Loader2 } from 'lucide-react';
import { historyApi, InterviewDetail } from '../api/history';
import InterviewDetailPanel from '../components/InterviewDetailPanel';

interface InterviewDetailPageProps {
  sessionId: string;
  onBack: () => void;
}

/**
 * 面试详情独立页面。
 * 统一挂载在 /interviews 路由域下，确保导航归属一致。
 */
export default function InterviewDetailPage({ sessionId, onBack }: InterviewDetailPageProps) {
  const [interview, setInterview] = useState<InterviewDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    const loadInterview = async () => {
      setLoading(true);
      try {
        const detail = await historyApi.getInterviewDetail(sessionId);
        setInterview(detail);
      } catch (err) {
        console.error('加载面试详情失败', err);
        setInterview(null);
      } finally {
        setLoading(false);
      }
    };

    loadInterview();
  }, [sessionId]);

  const handleExport = async () => {
    setExporting(true);
    try {
      const blob = await historyApi.exportInterviewPdf(sessionId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `面试报告_${sessionId.slice(-8)}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch {
      alert('导出失败，请稍后重试');
    } finally {
      setExporting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  if (!interview) {
    return (
      <div className="max-w-4xl mx-auto bg-white rounded-2xl border border-red-100 px-6 py-10 text-center">
        <p className="text-red-600 mb-4">面试详情加载失败</p>
        <button
          onClick={onBack}
          className="px-5 py-2.5 rounded-lg bg-slate-900 text-white"
        >
          返回面试记录
        </button>
      </div>
    );
  }

  return (
    <motion.div
      className="w-full max-w-6xl mx-auto"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.28 }}
    >
      <div className="mb-6 rounded-2xl border border-slate-200 bg-white/80 backdrop-blur-sm px-5 py-4">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-3">
            <button
              onClick={onBack}
              className="w-10 h-10 rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors flex items-center justify-center"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="text-xl font-bold text-slate-900">面试结果详情</h1>
              <p className="text-sm text-slate-500">会话编号 #{sessionId.slice(-8)}</p>
            </div>
          </div>

          <button
            onClick={handleExport}
            disabled={exporting}
            className="px-4 py-2 rounded-lg bg-primary-600 text-white disabled:opacity-50 inline-flex items-center gap-2"
          >
            {exporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
            {exporting ? '导出中...' : '导出 PDF'}
          </button>
        </div>
      </div>

      <InterviewDetailPanel interview={interview} />
    </motion.div>
  );
}
