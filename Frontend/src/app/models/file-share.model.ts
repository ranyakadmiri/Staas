export interface MountInfo {
  server: string;
  exportPath: string;
  nfsVersion: string;
  linuxCommand: string;
  windowsCommand: string;
  macosCommand: string;
}

export interface FileShare {
  id: number;
  projectId: number;
  name: string;
  status: string;
  mountInfo: MountInfo;
}

export interface FileEntry {
  name: string;
  size: string;
  date: string;
  directory: boolean;
}