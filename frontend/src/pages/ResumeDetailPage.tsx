import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { historyApi, ResumeDetail } from '../api/history';
import AnalysisPanel from '../components/AnalysisPanel';
import InterviewPanel from '../components/InterviewPanel';
import { formatDateOnly } from '../utils/date';
import {
  ChevronLeft,
  Clock,
  Mic,
  CheckSquare,
  MessageSquare,
} from 'lucide-react';

interface ResumeDetailPageProps {
  resumeId: number;
  onBack: () => void;
  onStartInterview: (resumeText: string, resumeId: number) => void;
}

type TabType = 'analysis' | 'interview';

export default function ResumeDetailPage({ resumeId, onBack, onStartInterview }: ResumeDetailPageProps) {
  const navigate = useNavigate();
  const [resume, setResume] = useState<ResumeDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<TabType>('analysis');
  const [exporting, setExporting] = useState<string | null>(null);
  const [[page, direction], setPage] = useState([0, 0]);
  const [reanalyzing, setReanalyzing] = useState(false);

  const loadResumeDetailSilent = useCallback(async () => {
    try {
      const data = await historyApi.getResumeDetail(resumeId);
      setResume(data);
    } catch (err) {
      console.error('加载简历详情失败', err);
    }
  }, [resumeId]);

  const loadResumeDetail = useCallback(async () => {
    setLoading(true);
    try {
      const data = await historyApi.getResumeDetail(resumeId);
      setResume(data);
    } catch (err) {
      console.error('加载简历详情失败', err);
    } finally {
      setLoading(false);
    }
  }, [resumeId]);

  useEffect(() => {
    loadResumeDetail();
  }, [loadResumeDetail]);

  useEffect(() => {
    const isProcessing = resume && (
      resume.analyzeStatus === 'PENDING'
      || resume.analyzeStatus === 'PROCESSING'
      || (resume.analyzeStatus === undefined && (!resume.analyses || resume.analyses.length === 0))
    );

    if (isProcessing && !loading) {
      const timer = setInterval(() => {
        loadResumeDetailSilent();
      }, 5000);

      return () => clearInterval(timer);
    }
  }, [resume, loading, loadResumeDetailSilent]);

  const handleReanalyze = async () => {
    try {
      setReanalyzing(true);
      await historyApi.reanalyze(resumeId);
      await loadResumeDetailSilent();
    } catch (err) {
      console.error('重新分析失败', err);
    } finally {
      setReanalyzing(false);
    }
  };

  const handleExportAnalysisPdf = async () => {
    setExporting('analysis');
    try {
      const blob = await historyApi.exportAnalysisPdf(resumeId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `简历分析报告_${resume?.filename || resumeId}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('导出失败，请重试');
    } finally {
      setExporting(null);
    }
  };

  const handleExportInterviewPdf = async (sessionId: string) => {
    setExporting(sessionId);
    try {
      const blob = await historyApi.exportInterviewPdf(sessionId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `面试报告_${sessionId}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('导出失败，请重试');
    } finally {
      setExporting(null);
    }
  };

  const handleDeleteInterview = async (_sessionId: string) => {
    await loadResumeDetail();
  };

  const handleViewInterview = (sessionId: string) => {
    navigate(`/interviews/${sessionId}`);
  };

  const handleTabChange = (tab: TabType) => {
    const newPage = tab === 'analysis' ? 0 : 1;
    setPage([newPage, newPage > page ? 1 : -1]);
    setActiveTab(tab);
  };

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

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <motion.div
          className="w-12 h-12 border-4 border-slate-200 border-t-primary-500 rounded-full"
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
        />
      </div>
    );
  }

  if (!resume) {
    return (
      <div className="text-center py-20">
        <p className="text-red-500 mb-4">加载失败，请返回重试</p>
        <button onClick={onBack} className="px-6 py-2 bg-primary-500 text-white rounded-lg">返回列表</button>
      </div>
    );
  }

  const latestAnalysis = resume.analyses?.[0];
  const tabs = [
    { id: 'analysis' as const, label: '简历分析', icon: CheckSquare },
    { id: 'interview' as const, label: '面试记录', icon: MessageSquare, count: resume.interviews?.length || 0 },
  ];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full"
    >
      <div className="flex justify-between items-center mb-8 flex-wrap gap-4">
        <div className="flex items-center gap-4">
          <motion.button
            onClick={onBack}
            className="w-10 h-10 bg-white rounded-xl flex items-center justify-center text-slate-500 hover:bg-slate-50 hover:text-slate-700 transition-all shadow-sm"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            <ChevronLeft className="w-5 h-5" />
          </motion.button>
          <div>
            <h2 className="text-xl font-bold text-slate-900">{resume.filename}</h2>
            <p className="text-sm text-slate-500 flex items-center gap-1.5">
              <Clock className="w-4 h-4" />
              上传于 {formatDateOnly(resume.uploadedAt)}
            </p>
          </div>
        </div>

        <motion.button
          onClick={() => onStartInterview(resume.resumeText, resumeId)}
          className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl font-medium shadow-lg shadow-primary-500/30 hover:shadow-xl transition-all flex items-center gap-2"
          whileHover={{ scale: 1.02, y: -1 }}
          whileTap={{ scale: 0.98 }}
        >
          <Mic className="w-4 h-4" />
          开始模拟面试
        </motion.button>
      </div>

      <div className="bg-white rounded-2xl p-2 mb-6 inline-flex gap-1">
        {tabs.map((tab) => (
          <motion.button
            key={tab.id}
            onClick={() => handleTabChange(tab.id)}
            className={`relative px-6 py-3 rounded-xl font-medium flex items-center gap-2 transition-colors
              ${activeTab === tab.id ? 'text-primary-600' : 'text-slate-500 hover:text-slate-700'}`}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
          >
            {activeTab === tab.id && (
              <motion.div
                layoutId="activeTab"
                className="absolute inset-0 bg-primary-50 rounded-xl"
                transition={{ type: 'spring', bounce: 0.2, duration: 0.6 }}
              />
            )}
            <span className="relative z-10 flex items-center gap-2">
              <tab.icon className="w-5 h-5" />
              {tab.label}
              {tab.count !== undefined && tab.count > 0 && (
                <span className="px-2 py-0.5 bg-primary-100 text-primary-600 text-xs rounded-full">{tab.count}</span>
              )}
            </span>
          </motion.button>
        ))}
      </div>

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
              <AnalysisPanel
                analysis={latestAnalysis}
                analyzeStatus={resume.analyzeStatus}
                analyzeError={resume.analyzeError}
                onExport={handleExportAnalysisPdf}
                exporting={exporting === 'analysis'}
                onReanalyze={handleReanalyze}
                reanalyzing={reanalyzing}
              />
            ) : (
              <InterviewPanel
                interviews={resume.interviews || []}
                onStartInterview={() => onStartInterview(resume.resumeText, resumeId)}
                onViewInterview={handleViewInterview}
                onExportInterview={handleExportInterviewPdf}
                onDeleteInterview={handleDeleteInterview}
                exporting={exporting}
                loadingInterview={false}
              />
            )}
          </motion.div>
        </AnimatePresence>
      </div>
    </motion.div>
  );
}
