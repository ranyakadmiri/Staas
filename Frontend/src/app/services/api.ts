import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from './auth-service';

import { FileShare, MountInfo, FileEntry } from '../models/file-share.model';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class Api {

  baseUrl = "http://localhost:8080/api";

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // Headers with JWT token
  getHeaders() {
    const token = this.authService.getToken();

    return {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`
      })
    };
  }
// ADMIN USERS
getPendingUsers(): Observable<User[]> {
  return this.http.get<User[]>(`${this.baseUrl}/admin/pending-users`, this.getHeaders());
}

approveUser(id: number): Observable<string> {
  return this.http.post<string>(`${this.baseUrl}/admin/approve/${id}`, {}, this.getHeaders());
}

rejectUser(id: number): Observable<string> {
  return this.http.post<string>(`${this.baseUrl}/admin/reject/${id}`, {}, this.getHeaders());
}
  // ========================
  // PROJECTS
  // ========================

  createProject(data: any) {
    return this.http.post(
      `${this.baseUrl}/projects/createprojects`,
      data,
      this.getHeaders()
    );
  }

  getProjects() {
    return this.http.get(
      `${this.baseUrl}/projects/ListProjects`,
      this.getHeaders()
    );
  }

  // ========================
  // BUCKETS
  // ========================

  createBucket(projectId: number, bucketName: string) {
    return this.http.post(
      `${this.baseUrl}/buckets/create/${projectId}?bucketName=${bucketName}`,
      {},
      this.getHeaders()
    );
  }

  getBuckets(projectId: number) {
    return this.http.get(
      `${this.baseUrl}/buckets/list/${projectId}`,
      this.getHeaders()
    );
  }

  deleteBucket(projectId: number, bucketName: string) {
    return this.http.delete(
      `${this.baseUrl}/buckets/delete/${projectId}?bucketName=${bucketName}`,
      this.getHeaders()
    );
  }

  // ========================
  // FILE UPLOAD
  // ========================

  uploadFile(projectId: number, bucketName: string, file: File) {

    const formData = new FormData();
    formData.append("bucketName", bucketName);
    formData.append("file", file);

    return this.http.post(
      `${this.baseUrl}/buckets/${projectId}/upload`,
      formData,
      this.getHeaders()
    );
  }
  register(data: any) {
  return this.http.post(
    `http://localhost:8080/api/auth/register`,
    data
  );
}
getObjects(projectId:number, bucket:string){
  return this.http.get(
    `${this.baseUrl}/objects/${projectId}/${bucket}`,
    this.getHeaders()
  );
}
getMyCredentials(){
  return this.http.get(
    this.baseUrl + "/my-credentials",
    this.getHeaders()
  );
}
getBucketStats(projectId:number,bucketName:string){

return this.http.get(
this.baseUrl + "/buckets/" + projectId + "/stats?bucketName=" + bucketName,
this.getHeaders()
);

}
// ========================
// BLOCK STORAGE (RBD)
// ========================

createVolume(name: string, sizeGB: number) {
  return this.http.post(
    `${this.baseUrl}/block/create`,
    { name, sizeGB },
    this.getHeaders()
  );
}

listVolumes() {
  return this.http.get<string[]>(
    `${this.baseUrl}/block/list`,
    this.getHeaders()
  );
}

deleteVolume(name: string) {
  return this.http.delete(
    `${this.baseUrl}/block/delete?name=${name}`,
    this.getHeaders()
  );
}

getVolumeInfo(name: string) {
  return this.http.get<{info: string}>(
    `${this.baseUrl}/block/info?name=${name}`,
    this.getHeaders()
  );
}
// ========================
// FILE STORAGE (CephFS)
// ========================

listShares() {
  return this.http.get<string[]>(
    `${this.baseUrl}/file/list`,
    this.getHeaders()
  );
}

createShare(name: string) {
  return this.http.post(
    `${this.baseUrl}/file/create`,
    { name },
    this.getHeaders()
  );
}

browseShare(name: string) {
  return this.http.get<any[]>(
    `${this.baseUrl}/file/browse?name=${name}`,
    this.getHeaders()
  );
}

getShareSize(name: string) {
  return this.http.get<any>(
    `${this.baseUrl}/file/size?name=${name}`,
    this.getHeaders()
  );
}

deleteShare(name: string) {
  return this.http.delete(
    `${this.baseUrl}/file/delete?name=${name}`,
    this.getHeaders()
  );
}

uploadToShare(dir: string, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  const token = this.authService.getToken();
  return this.http.post(
    `${this.baseUrl}/file/upload?dir=${dir}`,
    formData,
    { headers: new HttpHeaders({ Authorization: `Bearer ${token}` }) }
  );
}
// ========================
// iSCSI STORAGE (CLEAN)
// ========================

createIscsiVolume(name: string, sizeGB: number, initiatorIqn: string) {
  return this.http.post<{
    message: string;
    details: string;
    esxi: string;
  }>(
    `${this.baseUrl}/iscsi/create`,
    { name, sizeGB, initiatorIqn },
    this.getHeaders()
  );
}

deleteIscsiVolume(name: string, initiatorIqn: string) {
  return this.http.delete<{
    message: string;
    details: string;
  }>(
    `${this.baseUrl}/iscsi/delete?name=${name}&initiatorIqn=${initiatorIqn}`,
    this.getHeaders()
  );
}

listIscsiTargets() {
  return this.http.get<{
    targets: string[];
  }>(
    `${this.baseUrl}/iscsi/targets`,
    this.getHeaders()
  );
}

listIscsiDisks() {
  return this.http.get<{
    disks: string[];
  }>(
    `${this.baseUrl}/iscsi/disks`,
    this.getHeaders()
  );
}

getEsxiHelp(name: string) {
  return this.http.get<{
    help: string;
  }>(
    `${this.baseUrl}/iscsi/esxi-help?name=${name}`,
    this.getHeaders()
  );
}
// ========================
// BLOCK VOLUMES (NEW)
// ========================

createBlockVolume(projectId: number, request: any) {
  return this.http.post<any>(
    `${this.baseUrl}/projects/${projectId}/block-volumes`,
    request,
    this.getHeaders()
  );
}

listBlockVolumes(projectId: number) {
  return this.http.get<any>(
    `${this.baseUrl}/projects/${projectId}/block-volumes`,
    this.getHeaders()
  );
}

getBlockVolume(projectId: number, name: string) {
  return this.http.get<any>(
    `${this.baseUrl}/projects/${projectId}/block-volumes/${name}`,
    this.getHeaders()
  );
}

deleteBlockVolume(projectId: number, name: string) {
  return this.http.delete<any>(
    `${this.baseUrl}/projects/${projectId}/block-volumes/${name}`,
    this.getHeaders()
  );
}

getBlockVolumeEvents(projectId: number, name: string) {
  return this.http.get<any>(
    `${this.baseUrl}/projects/${projectId}/block-volumes/${name}/events`,
    this.getHeaders()
  );
}
// ========================
// FILE SHARES (CephFS + NFS)
// ========================

// CREATE SHARE
createProjectShare(projectId: number, name: string) {
  return this.http.post<FileShare>(
    `${this.baseUrl}/projects/${projectId}/shares`,
    { name },
    this.getHeaders()
  );
}

// LIST SHARES
listProjectShares(projectId: number) {
  return this.http.get<FileShare[]>(
    `${this.baseUrl}/projects/${projectId}/shares`,
    this.getHeaders()
  );
}

// GET SHARE
getProjectShare(projectId: number, name: string) {
  return this.http.get<FileShare>(
    `${this.baseUrl}/projects/${projectId}/shares/${name}`,
    this.getHeaders()
  );
}

// MOUNT INFO
getProjectShareMountInfo(projectId: number, name: string) {
  return this.http.get<MountInfo>(
    `${this.baseUrl}/projects/${projectId}/shares/${name}/mount`,
    this.getHeaders()
  );
}

// BROWSE FILES
browseProjectShare(projectId: number, name: string) {
  return this.http.get<FileEntry[]>(
    `${this.baseUrl}/projects/${projectId}/shares/${name}/browse`,
    this.getHeaders()
  );
}

// SIZE
getProjectShareSize(projectId: number, name: string) {
  return this.http.get<{ name: string; size: string }>(
    `${this.baseUrl}/projects/${projectId}/shares/${name}/size`,
    this.getHeaders()
  );
}

// DELETE SHARE
deleteProjectShare(projectId: number, name: string) {
  return this.http.delete(
    `${this.baseUrl}/projects/${projectId}/shares/${name}`,
    this.getHeaders()
  );
}

// UPLOAD FILE
uploadToProjectShare(projectId: number, name: string, file: File) {

  const formData = new FormData();
  formData.append('file', file);

  const token = this.authService.getToken();

  return this.http.post(
    `${this.baseUrl}/projects/${projectId}/shares/${name}/upload`,
    formData,
    {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`
      })
    }
  );
}
appendBlockVolumeExtent(projectId: number, name: string, request: any) {
  return this.http.post<any>(
    `${this.baseUrl}/projects/${projectId}/block-volumes/${name}/extents`,
    request,
    this.getHeaders()
  );
}
// ========================
// BUCKET QUOTA
// ========================

getBucketQuota(bucketId: number) {
  return this.http.get<any>(
    `${this.baseUrl}/buckets/quota/${bucketId}`,
    this.getHeaders()
  );
}

createBucketQuota(bucketId: number, maxSizeGB: number, maxObjects: number) {
  return this.http.post<any>(
    `${this.baseUrl}/buckets/quota/${bucketId}`,
    { maxSizeGB, maxObjects },
    this.getHeaders()
  );
}

updateBucketQuota(bucketId: number, maxSizeGB: number, maxObjects: number) {
  return this.http.put<any>(
    `${this.baseUrl}/buckets/quota/${bucketId}`,
    { maxSizeGB, maxObjects },
    this.getHeaders()
  );
}
// ========================
// BILLING
// ========================

getPlans() {
  return this.http.get<any[]>(
    `${this.baseUrl}/billing/plans`,
    this.getHeaders()
  );
}

subscribe(projectId: number, planId: number) {
  return this.http.post<any>(
    `${this.baseUrl}/billing/subscribe/${projectId}?planId=${planId}`,
    {},
    this.getHeaders()
  );
}

getInvoices(projectId: number) {
  return this.http.get<any[]>(
    `${this.baseUrl}/billing/invoices/${projectId}`,
    this.getHeaders()
  );
}

downloadInvoicePdf(invoiceId: number) {
  const token = this.authService.getToken();
  return this.http.get(
    `${this.baseUrl}/billing/invoices/${invoiceId}/pdf`,
    {
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` }),
      responseType: 'blob'
    }
  );
}
// Add these to your existing Api service

getUsageByProject(projectId: number) {
  return this.http.get<any[]>(
    `${this.baseUrl}/billing/usage/${projectId}`,
    this.getHeaders()
  );
}

generateInvoiceNow(projectId: number) {
  return this.http.post<any>(
    `${this.baseUrl}/billing/test/generate-invoice/${projectId}`,
    {},
    this.getHeaders()
  );
}

getSubscription(projectId: number) {
  return this.http.get<any>(
    `${this.baseUrl}/billing/subscription/${projectId}`,
    this.getHeaders()
  );
}
verifyOtp(mfaToken: string, otp: string) {
  return this.http.post<{ token: string; email: string }>(
    `${this.baseUrl}/auth/verify-otp`,
    { mfaToken, otp }
    // ← no headers
  );
}
// ADD these methods to your existing Api service (api.ts)
// Place them after the existing block volumes section

// ========================
// AGENTS
// ========================

listAgents(projectId: number) {
  return this.http.get<any[]>(
    `${this.baseUrl}/projects/${projectId}/agents`,
    this.getHeaders()
  );
}

registerAgent(request: { hostname: string; initiatorIqn: string; os: string }) {
  return this.http.post<{ agentId: number; jwt: string }>(
    `${this.baseUrl}/agents/register`,
    request,
    this.getHeaders()
  );
}

// ========================
// ATTACH / DETACH
// ========================

attachVolume(projectId: number, volumeName: string, agentId: number) {
  return this.http.post<any>(
    `${this.baseUrl}/projects/${projectId}/block-volumes/${volumeName}/attach`,
    { agentId },
    this.getHeaders()
  );
}

detachVolume(projectId: number, volumeName: string, agentId: number) {
  return this.http.post<any>(
    `${this.baseUrl}/projects/${projectId}/block-volumes/${volumeName}/detach`,
    { agentId },
    this.getHeaders()
  );
}
}