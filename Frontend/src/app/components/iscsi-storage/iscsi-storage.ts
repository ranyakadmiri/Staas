import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Api } from '../../services/api';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-iscsi-storage',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './iscsi-storage.html',
  styleUrls: ['./iscsi-storage.css'],
})export class IscsiStorage implements OnInit {

  // 🔥 PROJECT ID
  projectId!: number;

  // DATA
  volumes: any[] = [];
  selectedEvents: any[] = [];
  showExtentModal = false;
extentTarget: string | null = null;
newExtent = { sizeGB: 10 };
extendingVolume = false;

  // FORM
  newVolume = {
    name: '',
    sizeGB: 10,
    initiatorIqn: 'iqn.1998-01.com.vmware:localhost.localdomain:1218095324:65'
  };

  // UI
  loading = false;
  creating = false;
  deleting: string | null = null;

  toast: { message: string; type: 'success' | 'error' } | null = null;

  constructor(private api: Api, private route: ActivatedRoute) {}

ngOnInit(): void {
  this.projectId = Number(this.route.snapshot.paramMap.get('projectId'));
  this.loadVolumes();


}


  // ───────── LOAD ─────────
  loadVolumes(): void {
    this.loading = true;

    this.api.listBlockVolumes(this.projectId).subscribe({
      next: (res) => {
        this.volumes = res || [];
        this.loading = false;
      },
      error: () => {
        this.showToast('Failed to load volumes', 'error');
        this.loading = false;
      },
    });
  }

  // ───────── CREATE ─────────
createVolume(): void {
  if (!this.newVolume.name.trim() || this.creating) return;

  this.creating = true;

  const name = this.newVolume.name.trim();

  this.showToast('Provisioning started (can take ~1 min)...', 'success');

  this.api.createBlockVolume(this.projectId, this.newVolume).subscribe({
    next: () => {

      this.newVolume = { name: '', sizeGB: 10 , initiatorIqn: 'iqn.1998-01.com.vmware:localhost.localdomain:1218095324:65'};
      this.creating = false;

      // 🔥 start polling THIS volume only
      this.pollVolumeStatus(name);

    },
    error: (err) => {
      console.error(err);
      this.showToast(err.error?.error || 'Failed to create volume', 'error');
      this.creating = false;
    },
  });
}
pollVolumeStatus(name: string) {

  const interval = setInterval(() => {

    this.api.getBlockVolume(this.projectId, name).subscribe({
      next: (volume) => {

        this.loadVolumes(); // refresh list

        if (volume.status === 'READY') {
          clearInterval(interval);
          this.showToast(`Volume "${name}" READY`, 'success');
        }

        if (volume.status === 'ERROR') {
          clearInterval(interval);
          this.showToast(volume.errorMessage || 'Provisioning failed', 'error');
        }

      },
      error: () => {
        clearInterval(interval);
      }
    });

  }, 4000); // every 4s
}

  // ───────── DELETE ─────────
  deleteVolume(name: string): void {
    if (!confirm(`Delete "${name}"?`)) return;

    this.deleting = name;

    this.api.deleteBlockVolume(this.projectId, name).subscribe({
      next: () => {
        this.showToast('Deleted', 'success');
        this.deleting = null;
        this.loadVolumes();
      },
      error: () => {
        this.showToast('Delete failed', 'error');
        this.deleting = null;
      },
    });
  }

  // ───────── EVENTS ─────────
  loadEvents(name: string): void {
    this.api.getBlockVolumeEvents(this.projectId, name).subscribe({
      next: (res) => {
        this.selectedEvents = res || [];
      },
      error: () => {
        this.showToast('Failed to load events', 'error');
      },
    });
  }

  // ───────── TOAST ─────────
  private showToast(message: string, type: 'success' | 'error') {
    this.toast = { message, type };
    setTimeout(() => (this.toast = null), 3000);
  }
  openExtentModal(name: string): void {
  this.extentTarget = name;
  this.newExtent = { sizeGB: 10 };
  this.showExtentModal = true;
}

closeExtentModal(): void {
  this.showExtentModal = false;
  this.extentTarget = null;
}
appendExtent(): void {
  if (!this.extentTarget || this.extendingVolume) return;

  this.extendingVolume = true;

  const request: any = {
    name: this.extentTarget,
    sizeGB: this.newExtent.sizeGB,
    initiatorIqn: this.newVolume.initiatorIqn
  };

  this.api.appendBlockVolumeExtent(this.projectId, this.extentTarget, request).subscribe({
    next: () => {
      this.showToast(`Disk appended to "${this.extentTarget}"`, 'success');
      this.extendingVolume = false;
      this.closeExtentModal();
      this.pollVolumeStatus(this.extentTarget!);
      this.loadVolumes();
    },
    error: (err: any) => {
      this.showToast(err.error?.error || 'Failed to append disk', 'error');
      this.extendingVolume = false;
    }
  });
}}