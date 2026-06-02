import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../../services/api';
import { ActivatedRoute } from '@angular/router';
import { FileShare, FileEntry } from '../../models/file-share.model';

@Component({
  selector: 'app-file-shares',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './file-shares.html',
  styleUrl: './file-shares.css',
})
export class FileShares implements OnInit {
  projectId!: number;
  shares: FileShare[] = [];
  selectedShare: FileShare | null = null;
  files: FileEntry[] = [];
  newShareName = '';
  loading = false;
  creating = false;
  error: string | null = null;
  success: string | null = null;
  activeTab: 'linux' | 'windows' | 'macos' = 'linux';
  copied = false;

  constructor(
    private api: Api,
    private route: ActivatedRoute,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.projectId = params['projectId'];
      if (this.projectId) this.loadShares();
    });
  }

  loadShares() {
    this.loading = true;
    this.api.listProjectShares(this.projectId).subscribe({
      next: (res) => {
        this.shares = res;
        this.loading = false;
        this.cd.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load shares';
        this.loading = false;
      }
    });
  }

  createShare() {
    if (!this.newShareName.trim() || this.creating) return;
    this.creating = true;
    this.error = null;

    this.api.createProjectShare(this.projectId, this.newShareName).subscribe({
      next: (res) => {
        this.success = `Share "${this.newShareName}" created successfully`;
        this.newShareName = '';
        this.creating = false;
        this.loadShares();
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to create share';
        this.creating = false;
      }
    });
  }

  selectShare(share: FileShare) {
    this.selectedShare = share;
    this.activeTab = 'linux';
    this.api.browseProjectShare(this.projectId, share.name)
      .subscribe({ next: (res) => { this.files = res; this.cd.detectChanges(); }, error: () => {} });
  }

  deleteShare(name: string) {
    if (!confirm(`Delete share "${name}"? This action cannot be undone.`)) return;
    this.api.deleteProjectShare(this.projectId, name).subscribe({
      next: () => {
        this.success = 'Share deleted';
        if (this.selectedShare?.name === name) this.selectedShare = null;
        this.loadShares();
      },
      error: () => this.error = 'Delete failed'
    });
  }

  uploadFile(event: any) {
    const file = event.target.files[0];
    if (!file || !this.selectedShare) return;
    this.api.uploadToProjectShare(this.projectId, this.selectedShare.name, file).subscribe({
      next: () => {
        this.success = `"${file.name}" uploaded`;
        this.selectShare(this.selectedShare!);
      },
      error: () => this.error = 'Upload failed'
    });
  }

  copyCommand(text: string) {
    navigator.clipboard.writeText(text).then(() => {
      this.copied = true;
      setTimeout(() => this.copied = false, 2000);
    });
  }

  setTab(tab: 'linux' | 'windows' | 'macos') {
    this.activeTab = tab;
  }

  get activeCommand(): string {
    if (!this.selectedShare?.mountInfo) return '';
    const m = this.selectedShare.mountInfo;
    if (this.activeTab === 'linux') return m.linuxCommand;
    if (this.activeTab === 'windows') return m.windowsCommand;
    return m.macosCommand;
  }

  dismissAlert() {
    this.error = null;
    this.success = null;
  }
}