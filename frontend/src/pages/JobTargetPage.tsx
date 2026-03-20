import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { jobMatchingApi } from '../api/jobMatching';
import { getErrorMessage } from '../api/request';
import type { JobDescription } from '../types/jobMatching';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';
import { Briefcase, ChevronRight, Search, Sparkles, Trash2 } from 'lucide-react';

type InputMode = 'url' | 'text' | 'file';

/**
 * 岗位目标页。
 * 支持 JD 三种输入方式，并可直接发起简历匹配分析。
 */
export default function JobTargetPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<InputMode>('url');
  const [url, setUrl] = useState('');
  const [text, setText] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [listLoading, setListLoading] = useState(true);
  const [listRefreshing, setListRefreshing] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<JobDescription | null>(null);
  const [items, setItems] = useState<JobDescription[]>([]);
  const [matchingStatusByJd, setMatchingStatusByJd] = useState<Record<number, JobMatchingStatus | undefined>>({});
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    loadJobDescriptions();
  }, []);

  const loadJobDescriptions = async (silent = false) => {
    if (silent) {
      setListRefreshing(true);
    } else {
      setListLoading(true);
    }

    try {
      const data = await jobMatchingApi.listJobDescriptions();
      setItems(data);
      await loadLatestMatchingStatus(data);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      if (silent) {
        setListRefreshing(false);
      } else {
        setListLoading(false);
      }
    }
  };

  /**
   * 提交 JD 并获取结构化结果。
   */
  const submitJd = async () => {
    if (mode === 'url' && !url.trim()) {
      setError('请先输入岗位 URL');
      return;
    }
    if (mode === 'text' && !text.trim()) {
      setError('请先输入岗位 JD 文本');
      return;
    }

    setLoading(true);
    setError('');
    try {
      let result: JobDescription;
      if (mode === 'url') {
        result = await jobMatchingApi.createJdFromUrl({ url: url.trim() });
      } else if (mode === 'text') {
        result = await jobMatchingApi.createJdFromText({ jdText: text.trim() });
      } else {
        if (!file) {
          throw new Error('请先选择文件');
        }
        result = await jobMatchingApi.createJdFromFile(file);
      }

      setUrl('');
      setText('');
      setFile(null);
      await loadJobDescriptions(true);
      navigate(`/job-descriptions/${result.id}`);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!items.some((i) => i.parseStatus === 'PENDING' || i.parseStatus === 'PROCESSING')) {
      return;
    }

    const timer = window.setInterval(() => {
      loadJobDescriptions(true);
    }, 3000);

    return () => window.clearInterval(timer);
  }, [items]);

  const filteredItems = useMemo(() => {
    if (!search.trim()) {
      return items;
    }

    const keyword = search.toLowerCase();
    return items.filter((item) =>
      (item.jobTitle || '').toLowerCase().includes(keyword)
      || (item.companyName || '').toLowerCase().includes(keyword)
      || (item.technicalSkills || []).join(',').toLowerCase().includes(keyword)
    );
  }, [items, search]);

  const remove = async () => {
    if (!deleteTarget) return;

    try {
      setDeletingId(deleteTarget.id);
      await jobMatchingApi.deleteJobDescription(deleteTarget.id);
      setItems((prev) => prev.filter((x) => x.id !== deleteTarget.id));
      setDeleteTarget(null);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setDeletingId(null);
    }
  };

  const loadLatestMatchingStatus = async (jobDescriptions: JobDescription[]) => {
    if (jobDescriptions.length === 0) {
      setMatchingStatusByJd({});
      return;
    }

    const records = await Promise.all(
      jobDescriptions.map(async (item) => {
        try {
          const list = await jobMatchingApi.listMatchingsByJd(item.id);
          return [item.id, list[0]?.status] as const;
        } catch {
          return [item.id, undefined] as const;
        }
      })
    );

    const next: Record<number, JobMatchingStatus | undefined> = {};
    records.forEach(([jdId, status]) => {
      next[jdId] = status;
    });
    setMatchingStatusByJd(next);
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <div className="relative overflow-hidden rounded-3xl border border-slate-200 bg-gradient-to-br from-white via-indigo-50/40 to-sky-50/40 p-7">
        <div className="pointer-events-none absolute inset-0 opacity-40" style={{ backgroundImage: 'radial-gradient(#dbeafe 1px, transparent 1px)', backgroundSize: '16px 16px' }} />
        <div className="relative">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-100 text-indigo-700 text-xs font-semibold mb-3">
            <Sparkles className="w-3.5 h-3.5" />
            岗位目标工作台
          </div>
          <h1 className="text-2xl font-bold text-slate-900">解析 JD 并持续管理岗位目标</h1>
          <p className="text-slate-600 mt-2">上方提交新 JD，解析完成后会自动沉淀到下方列表，可直接进入详情或发起匹配分析。</p>
        </div>
      </div>

      <div className="grid grid-cols-1 2xl:grid-cols-5 gap-6">
        <div className="2xl:col-span-3 space-y-6">
          <div className="bg-white rounded-2xl p-6 border border-slate-200 space-y-4">
            <div className="flex gap-3 flex-wrap">
              <button className={`px-4 py-2 rounded-lg cursor-pointer ${mode === 'url' ? 'bg-primary-600 text-white shadow-sm shadow-primary-600/25' : 'bg-slate-100 text-slate-700'}`} onClick={() => setMode('url')}>URL</button>
              <button className={`px-4 py-2 rounded-lg cursor-pointer ${mode === 'text' ? 'bg-primary-600 text-white shadow-sm shadow-primary-600/25' : 'bg-slate-100 text-slate-700'}`} onClick={() => setMode('text')}>文本</button>
              <button className={`px-4 py-2 rounded-lg cursor-pointer ${mode === 'file' ? 'bg-primary-600 text-white shadow-sm shadow-primary-600/25' : 'bg-slate-100 text-slate-700'}`} onClick={() => setMode('file')}>文件</button>
            </div>

            {mode === 'url' && (
              <input value={url} onChange={(e) => setUrl(e.target.value)} placeholder="粘贴岗位 URL" className="w-full px-4 py-3 rounded-lg border border-slate-200" />
            )}
            {mode === 'text' && (
              <textarea value={text} onChange={(e) => setText(e.target.value)} placeholder="粘贴岗位 JD 文本" rows={10} className="w-full px-4 py-3 rounded-lg border border-slate-200" />
            )}
            {mode === 'file' && (
              <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="w-full" />
            )}

            <button disabled={loading} onClick={submitJd} className="px-5 py-2.5 rounded-lg bg-slate-900 text-white cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
              {loading ? '解析中...' : '解析 JD'}
            </button>
          </div>
        </div>

        <div className="2xl:col-span-2 space-y-4">
          <div className="bg-white rounded-2xl p-4 border border-slate-200">
            <div className="flex items-center gap-3">
              <Search className="w-4 h-4 text-slate-400" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="搜索岗位、公司或技能"
                className="w-full outline-none text-slate-700 placeholder:text-slate-400"
              />
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
            <div className="px-4 py-3 border-b border-slate-100 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-800 flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-slate-500" />
                已解析 JD 列表
              </h2>
              {listRefreshing && <span className="text-xs text-slate-400">刷新中...</span>}
            </div>

            {listLoading ? (
              <div className="px-4 py-10 text-center text-slate-500">加载中...</div>
            ) : filteredItems.length === 0 ? (
              <div className="px-4 py-10 text-center text-slate-500">暂无 JD 记录</div>
            ) : (
              <div className="max-h-[640px] overflow-y-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 border-b border-slate-100 sticky top-0">
                    <tr>
                      <th className="text-left px-4 py-2.5">岗位</th>
                      <th className="text-left px-4 py-2.5">状态</th>
                      <th className="text-right px-4 py-2.5">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredItems.map((item) => (
                      <tr
                        key={item.id}
                        onClick={() => navigate(`/job-descriptions/${item.id}`)}
                        className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50 cursor-pointer transition-colors group"
                      >
                        <td className="px-4 py-3">
                          <div className="font-medium text-slate-900 truncate">{item.jobTitle || '未命名岗位'}</div>
                          <div className="text-xs text-slate-500 truncate">{item.companyName || '-'} · {new Date(item.createdAt).toLocaleString()}</div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex flex-col items-start gap-1">
                            <span className={statusClass(item.parseStatus)}>{statusText(item.parseStatus)}</span>
                            {matchingStatusByJd[item.id] && (
                              <span className={matchingStatusClass(matchingStatusByJd[item.id] as JobMatchingStatus)}>
                                匹配：{matchingStatusText(matchingStatusByJd[item.id] as JobMatchingStatus)}
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={(event) => {
                                event.stopPropagation();
                                setDeleteTarget(item);
                              }}
                              className="p-1.5 rounded-md bg-slate-100 text-slate-600 cursor-pointer hover:bg-red-50 hover:text-red-500"
                              title="删除 JD"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                            <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-primary-500 group-hover:translate-x-1 transition-all" />
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>

      {error && <div className="bg-red-50 text-red-600 px-4 py-3 rounded-xl border border-red-100">{error}</div>}

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        item={deleteTarget ? { id: deleteTarget.id, name: deleteTarget.jobTitle || '未命名岗位' } : null}
        itemType="JD"
        loading={deletingId !== null}
        onConfirm={remove}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}

type JobMatchingStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

function matchingStatusText(status: JobMatchingStatus) {
  if (status === 'PENDING') return '排队中';
  if (status === 'PROCESSING') return '分析中';
  if (status === 'COMPLETED') return '已完成';
  return '失败';
}

function matchingStatusClass(status: JobMatchingStatus) {
  if (status === 'PENDING') return 'px-2 py-1 text-xs rounded bg-blue-50 text-blue-700';
  if (status === 'PROCESSING') return 'px-2 py-1 text-xs rounded bg-indigo-50 text-indigo-700';
  if (status === 'COMPLETED') return 'px-2 py-1 text-xs rounded bg-emerald-50 text-emerald-700';
  return 'px-2 py-1 text-xs rounded bg-red-50 text-red-700';
}

function statusText(status: JobDescription['parseStatus']) {
  if (status === 'PENDING') return '排队中';
  if (status === 'PROCESSING') return '解析中';
  if (status === 'COMPLETED') return '已完成';
  return '失败';
}

function statusClass(status: JobDescription['parseStatus']) {
  if (status === 'PENDING') return 'px-2 py-1 text-xs rounded bg-blue-50 text-blue-700';
  if (status === 'PROCESSING') return 'px-2 py-1 text-xs rounded bg-indigo-50 text-indigo-700';
  if (status === 'COMPLETED') return 'px-2 py-1 text-xs rounded bg-emerald-50 text-emerald-700';
  return 'px-2 py-1 text-xs rounded bg-red-50 text-red-700';
}
