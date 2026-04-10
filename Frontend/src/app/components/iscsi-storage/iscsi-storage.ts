import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Api } from '../../services/api';

interface IscsiDisk {
  name: string;
  pool: string;
  sizeMB: number;
}

@Component({
  selector: 'app-iscsi-storage',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './iscsi-storage.html',
  styleUrl: './iscsi-storage.css',
})
export class IscsiStorage implements OnInit {

  // ── Data ────────────────────────────────────────────────────────────
  disks: string = '';
  targets: string = '';
  esxiHelp: string = '';

  // ── Form ────────────────────────────────────────────────────────────
  newDiskName = '';
  newDiskSize = 10;
  initiatorIqn = 'iqn.1998-01.com.vmware:localhost.localdomain:1218095324:65';

  // ── UI state ────────────────────────────────────────────────────────
  // Granular loading flags — no more single boolean causing stuck states
  loadingData    = false;
  loadingCreate  = false;
  deletingDisk:  string | null = null; // holds the name being deleted

  toast: { message: string; type: 'success' | 'error' } | null = null;
  private toastTimer: any;

  showHelp = false;

  constructor(private api: Api) {}

  ngOnInit(): void {
    this.loadData();
  }

  // ── Data loading ────────────────────────────────────────────────────

  loadData(): void {
    this.loadingData = true;
    this.clearToast();

    // forkJoin waits for BOTH calls — loading ends only when both complete
    forkJoin({
      targets: this.api.listIscsiTargets(),
      disks:   this.api.listIscsiDisks(),
    }).subscribe({
      next: ({ targets, disks }) => {
        this.targets     = targets.targets;
        this.disks       = disks.disks;
        this.loadingData = false;
      },
      error: () => {
        this.showToast('Failed to load iSCSI data', 'error');
        this.loadingData = false;
      },
    });
  }

  // ── Create ──────────────────────────────────────────────────────────

  createDisk(): void {
    if (!this.newDiskName.trim() || this.loadingCreate) return;

    this.loadingCreate = true;
    this.clearToast();

    this.api.createIscsiVolume(
      this.newDiskName.trim(),
      this.newDiskSize,
      this.initiatorIqn
    ).subscribe({
      next: (res: any) => {
        this.esxiHelp    = res.esxi;
        this.showHelp    = true;
        this.newDiskName = '';
        this.newDiskSize = 10;
        this.loadingCreate = false;
        this.showToast('iSCSI disk created successfully', 'success');
        this.loadData(); // refresh list after state is already reset
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Failed to create disk', 'error');
        this.loadingCreate = false;
      },
    });
  }

  // ── Delete ──────────────────────────────────────────────────────────

  deleteDisk(name: string): void {
    if (!confirm(`Permanently delete iSCSI disk "${name}"?`)) return;
    if (this.deletingDisk) return; // prevent double-click

    this.deletingDisk = name;
    this.clearToast();

    this.api.deleteIscsiVolume(name, this.initiatorIqn).subscribe({
      next: () => {
        this.showToast(`Disk "${name}" deleted`, 'success');
        this.deletingDisk = null;
        this.loadData();
      },
      error: () => {
        this.showToast(`Failed to delete "${name}"`, 'error');
        this.deletingDisk = null;
      },
    });
  }

  // ── ESXi Help ────────────────────────────────────────────────────────

  fetchEsxiHelp(name: string): void {
    this.api.getEsxiHelp(name).subscribe({
      next: (res: any) => {
        this.esxiHelp = res.help;
        this.showHelp = true;
      },
      error: () => this.showToast('Failed to load ESXi help', 'error'),
    });
  }

  toggleHelp(): void {
    this.showHelp = !this.showHelp;
  }

  // ── Toast ────────────────────────────────────────────────────────────

  private showToast(message: string, type: 'success' | 'error'): void {
    clearTimeout(this.toastTimer);
    this.toast = { message, type };
    this.toastTimer = setTimeout(() => (this.toast = null), 4000);
  }

  private clearToast(): void {
    clearTimeout(this.toastTimer);
    this.toast = null;
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  get isFormValid(): boolean {
    return this.newDiskName.trim().length > 0 && this.newDiskSize > 0;
  }

  parseDiskLines(raw: string): string[] {
    return raw ? raw.split('\n').filter(l => l.trim()) : [];
  }
}