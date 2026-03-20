import request from './request';

export interface RuntimeInfo {
  chatModel: string;
}

export const systemApi = {
  getRuntimeInfo(): Promise<RuntimeInfo> {
    return request.get<RuntimeInfo>('/api/system/runtime-info');
  },
};
