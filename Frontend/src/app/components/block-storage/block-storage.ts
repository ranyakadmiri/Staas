import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../../services/api';

@Component({
  selector: 'app-block-storage',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './block-storage.html',
  styleUrl: './block-storage.css',
})


  
export class BlockStorage implements OnInit {

  volumes: string[] = [];
  selectedVolumeInfo: string = '';
  selectedVolume: string = '';

  newVolumeName = '';
  newVolumeSizeGB = 10;

  loading = false;
  error: string | null = null;
  success: string | null = null;
  showForm = false;

  constructor(private api: Api,private cd: ChangeDetectorRef ) {}

  ngOnInit(): void {
    this.loadVolumes();
  }

  loadVolumes(): void {
    this.loading = true;
    this.error = null;
    this.api.listVolumes().subscribe({
      next: (data) => {
        this.volumes = data;
        this.loading = false;
        this.cd.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load volumes';
        this.loading = false;
      }
    });
     this.cd.detectChanges();
  }

  createVolume(): void {
    if (!this.newVolumeName.trim()) return;
    this.loading = true;
    this.api.createVolume(this.newVolumeName.trim(), this.newVolumeSizeGB).subscribe({
      next: () => {
        this.success = `Volume "${this.newVolumeName}" created`;
        this.newVolumeName = '';
        this.newVolumeSizeGB = 10;
        this.showForm = false;
        this.loadVolumes();
        setTimeout(() => this.success = null, 3000);
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to create volume';
        this.loading = false;
      }
    });
  }

  deleteVolume(name: string): void {
    if (!confirm(`Delete volume "${name}"? This cannot be undone.`)) return;
    this.api.deleteVolume(name).subscribe({
      next: () => {
        this.success = `Volume "${name}" deleted`;
        if (this.selectedVolume === name) {
          this.selectedVolume = '';
          this.selectedVolumeInfo = '';
        }
        this.loadVolumes();
        setTimeout(() => this.success = null, 3000);
      },
      error: () => this.error = 'Failed to delete volume'
    });
  }

  showInfo(name: string): void {
    this.selectedVolume = name;
    this.api.getVolumeInfo(name).subscribe({
      next: (res) => this.selectedVolumeInfo = res.info,
      error: () => this.selectedVolumeInfo = 'Could not load info'
    });
  }

  closeInfo(): void {
    this.selectedVolume = '';
    this.selectedVolumeInfo = '';
  }
}
