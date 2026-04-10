import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from './auth-service';

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
// iSCSI STORAGE
// ========================

createIscsiVolume(name: string, sizeGB: number, initiatorIqn: string) {
  return this.http.post(
    `${this.baseUrl}/iscsi/create`,
    { name, sizeGB, initiatorIqn },
    this.getHeaders()
  );
}

deleteIscsiVolume(name: string, initiatorIqn: string) {
  return this.http.delete(
    `${this.baseUrl}/iscsi/delete?name=${name}&initiatorIqn=${initiatorIqn}`,
    this.getHeaders()
  );
}

listIscsiTargets() {
  return this.http.get<any>(
    `${this.baseUrl}/iscsi/targets`,
    this.getHeaders()
  );
}

listIscsiDisks() {
  return this.http.get<any>(
    `${this.baseUrl}/iscsi/disks`,
    this.getHeaders()
  );
}

getEsxiHelp(name: string) {
  return this.http.get<any>(
    `${this.baseUrl}/iscsi/esxi-help?name=${name}`,
    this.getHeaders()
  );
}}