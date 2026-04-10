import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../../services/api';

interface FileEntry {
  name: string;
  size: string;
  date: string;
  isDirectory: boolean;
}
@Component({
  selector: 'app-file-storage',
 standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './file-storage.html',
  styleUrl: './file-storage.css',
})
export class FileStorage implements OnInit {

  shares: string[] = [];
  currentShare: string | null = null;
  entries: FileEntry[] = [];
  shareSize = '';

  newShareName = '';
  showForm = false;
  loading = false;
  error: string | null = null;
  success: string | null = null;

  constructor(private api: Api,private cd: ChangeDetectorRef ) {}

  ngOnInit(): void {
    this.loadShares();
  }

  loadShares(): void {
       
    this.loading = true;
    this.error = null;
    this.api.listShares().subscribe({
      next: data => { this.shares = data; this.loading = false; },
      error: () => { this.error = 'Failed to load shares'; this.loading = false;
        this.cd.detectChanges();
       }
    });
       this.cd.detectChanges(); 
  }

  createShare(): void {
    if (!this.newShareName.trim()) return;
    this.loading = true;
    this.api.createShare(this.newShareName.trim()).subscribe({
      next: () => {
        this.success = `Share "${this.newShareName}" created`;
        this.newShareName = '';
        this.showForm = false;
        this.loadShares();
        setTimeout(() => this.success = null, 3000);
      },
      error: err => {
        this.error = err.error?.error || 'Failed to create share';
        this.loading = false;
      }
    });
  }

  openShare(name: string): void {
    this.currentShare = name;
    this.loading = true;
    this.error = null;
    this.api.browseShare(name).subscribe({
      next: data => { this.entries = data; this.loading = false; },
      error: () => { this.error = 'Failed to browse share'; this.loading = false; }
    });
    this.api.getShareSize(name).subscribe({
      next: res => this.shareSize = res.size,
      error: () => this.shareSize = '—'
    });
  }

  goBack(): void {
    this.currentShare = null;
    this.entries = [];
    this.shareSize = '';
    this.loadShares();
  }

  deleteShare(name: string): void {
    if (!confirm(`Delete share "${name}" and all its contents?`)) return;
    this.api.deleteShare(name).subscribe({
      next: () => {
        this.success = `Share "${name}" deleted`;
        this.loadShares();
        setTimeout(() => this.success = null, 3000);
      },
      error: () => this.error = 'Failed to delete share'
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length || !this.currentShare) return;
    const file = input.files[0];
    this.loading = true;
    this.api.uploadToShare(this.currentShare, file).subscribe({
      next: () => {
        this.success = `"${file.name}" uploaded`;
        this.openShare(this.currentShare!);
        setTimeout(() => this.success = null, 3000);
      },
      error: () => { this.error = 'Upload failed'; this.loading = false; }
    });
  }

  clearError(): void { this.error = null; }
}