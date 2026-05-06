import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Api } from '../../services/api';
import { ActivatedRoute } from '@angular/router';
import { FileShare, FileEntry, MountInfo } from '../../models/file-share.model';

@Component({
  selector: 'app-file-shares',
   standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './file-shares.html',
  styleUrl: './file-shares.css',
})
export class FileShares implements OnInit {

  projectId!: number;

  shares: FileShare[] = [];
  selectedShare: FileShare | null = null;
  files: FileEntry[] = [];
  mountInfo: MountInfo | null = null;

  newShareName = '';

  loading = false;
  error: string | null = null;
  success: string | null = null;

  constructor(
    private api: Api,
    private route: ActivatedRoute,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {

    this.route.params.subscribe(params => {

      this.projectId = params['projectId'];

      if (this.projectId) {
        this.loadShares();
      }

    });

  }

  // ========================
  // LOAD SHARES
  // ========================

  loadShares() {

    this.loading = true;

    this.api.listProjectShares(this.projectId).subscribe({
      next: (res) => {
        this.shares = res;
        this.loading = false;
        this.cd.detectChanges();
      },
      error: () => {
        this.error = "Failed to load shares";
        this.loading = false;
      }
    });
  }

  // ========================
  // CREATE
  // ========================

  createShare() {

    if (!this.newShareName.trim()) return;

    this.api.createProjectShare(this.projectId, this.newShareName).subscribe({
      next: () => {
        this.success = "Share created";
        this.newShareName = '';
        this.loadShares();
      },
      error: (err) => {
        this.error = err.error?.error || "Create failed";
      }
    });
  }

  // ========================
  // SELECT SHARE
  // ========================

  selectShare(share: FileShare) {

    this.selectedShare = share;

    this.api.getProjectShareMountInfo(this.projectId, share.name)
      .subscribe(res => this.mountInfo = res);

    this.api.browseProjectShare(this.projectId, share.name)
      .subscribe(res => this.files = res);
  }

  // ========================
  // DELETE
  // ========================

  deleteShare(name: string) {

    if (!confirm("Delete this share?")) return;

    this.api.deleteProjectShare(this.projectId, name).subscribe({
      next: () => {
        this.success = "Deleted";
        this.selectedShare = null;
        this.loadShares();
      },
      error: () => this.error = "Delete failed"
    });
  }

  // ========================
  // UPLOAD
  // ========================

  uploadFile(event: any) {

    const file = event.target.files[0];
    if (!file || !this.selectedShare) return;

    this.api.uploadToProjectShare(
      this.projectId,
      this.selectedShare.name,
      file
    ).subscribe(() => {

      this.success = "Uploaded";

      this.selectShare(this.selectedShare!);

    });
  }

}