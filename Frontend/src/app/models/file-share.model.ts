export interface FileShare {
  id: number;
  projectId: number;
  name: string;
  shareKey: string;
  pseudoPath: string;
  realPath: string;
  serverIp: string;
  exportId: number;
  status: string;
  mountTarget: string;
}

export interface MountInfo {
  server: string;
  path: string;
  mountTarget: string;
  protocol: string;
  esxiVersion: string;
}

export interface FileEntry {
  name: string;
  size: string;
  date: string;
  directory: boolean;
}