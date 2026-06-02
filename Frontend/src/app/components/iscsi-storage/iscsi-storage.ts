import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Api } from '../../services/api';

type WizardStep = 1 | 2 | 3 | 4;

@Component({
  selector: 'app-iscsi-storage',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './iscsi-storage.html',
  styleUrls: ['./iscsi-storage.css'],
})
export class IscsiStorage implements OnInit {

  projectId!: number;

  // ── Data ──────────────────────────────────────────────────────────────────
  volumes: any[] = [];
  agents:  any[] = [];
  selectedEvents: any[] = [];
  selectedVolume: any = null;

  // ── Create form ───────────────────────────────────────────────────────────
  newVolume = { name: '', sizeGB: 10 };

  // ── Modals ────────────────────────────────────────────────────────────────
  showEventsModal  = false;
  showAttachModal  = false;
  showExtentModal  = false;
  showWizard       = false;

  attachTarget:  any    = null;
  extentTarget:  string | null = null;
  selectedAgentId: number | null = null;
  newExtent = { name: '', sizeGB: 10 };

  // ── Wizard ────────────────────────────────────────────────────────────────
  wizardStep: WizardStep = 1;
  agentOs: 'linux' | 'windows' = 'linux';

  // ── UI state ──────────────────────────────────────────────────────────────
  loading         = false;
  creating        = false;
  deleting: string | null = null;
  attaching       = false;
  extendingVolume = false;
  copiedKey: string | null = null;

  toast: { message: string; type: 'success' | 'error' | 'info' } | null = null;

  // The server base URL — used to build download + register commands
  serverUrl = 'http://10.0.2.2:8080';

  constructor(private api: Api, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('projectId'));
    this.loadVolumes();
    this.loadAgents();
  }

  // ── JWT helper ────────────────────────────────────────────────────────────
  // Reads the user's JWT from wherever your AuthService stores it.
  // Adjust the key if yours differs.
 // iscsi-storage.ts — line to fix
getUserJwt(): string {
  return localStorage.getItem('authToken') || ''; // ← was 'token', must be 'authToken'
}

  // ── Data loading ──────────────────────────────────────────────────────────

  loadVolumes(): void {
    this.loading = true;
    this.api.listBlockVolumes(this.projectId).subscribe({
      next: (res) => { this.volumes = res || []; this.loading = false; },
      error: () => { this.showToast('Failed to load volumes', 'error'); this.loading = false; }
    });
  }

  loadAgents(): void {
    this.api.listAgents(this.projectId).subscribe({
      next: (res) => { this.agents = res || []; },
      error: () => {}
    });
  }

  // ── Create ────────────────────────────────────────────────────────────────

  createVolume(): void {
    if (!this.newVolume.name.trim() || this.creating) return;
    this.creating = true;
    const name = this.newVolume.name.trim();
    this.showToast('Provisioning started — this takes ~30s', 'info');

    this.api.createBlockVolume(this.projectId, {
      name,
      sizeGB: this.newVolume.sizeGB
      // no initiatorIqn — the agent provides it at attach time
    }).subscribe({
      next: () => {
        this.newVolume = { name: '', sizeGB: 10 };
        this.creating = false;
        this.pollVolumeStatus(name);
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Failed to create volume', 'error');
        this.creating = false;
      }
    });
  }

  pollVolumeStatus(name: string): void {
    const iv = setInterval(() => {
      this.api.getBlockVolume(this.projectId, name).subscribe({
        next: (v) => {
          this.loadVolumes();
          if (['ISCSI_READY', 'READY'].includes(v.status)) {
            clearInterval(iv);
            this.showToast(`"${name}" is ready — you can now attach it`, 'success');
          }
          if (v.status === 'ERROR') {
            clearInterval(iv);
            this.showToast(v.errorMessage || 'Provisioning failed', 'error');
          }
        },
        error: () => clearInterval(iv)
      });
    }, 4000);
  }

  // ── Delete ────────────────────────────────────────────────────────────────

  deleteVolume(name: string): void {
    if (!confirm(`Delete "${name}"? This is permanent.`)) return;
    this.deleting = name;
    this.api.deleteBlockVolume(this.projectId, name).subscribe({
      next: () => { this.showToast('Deleted', 'success'); this.deleting = null; this.loadVolumes(); },
      error: () => { this.showToast('Delete failed', 'error'); this.deleting = null; }
    });
  }

  // ── Events modal ──────────────────────────────────────────────────────────

  openEventsModal(v: any): void {
    this.selectedVolume = v;
    this.selectedEvents = [];
    this.showEventsModal = true;
    this.api.getBlockVolumeEvents(this.projectId, v.name).subscribe({
      next: (res) => { this.selectedEvents = res || []; },
      error: () => this.showToast('Failed to load events', 'error')
    });
  }

  closeEventsModal(): void { this.showEventsModal = false; }

  // ── Attach modal ──────────────────────────────────────────────────────────

  openAttachModal(v: any): void {
    this.attachTarget = v;
    this.selectedAgentId = this.agents.length === 1 ? this.agents[0].id : null;
    this.showAttachModal = true;
  }

  closeAttachModal(): void { this.showAttachModal = false; this.attachTarget = null; }

  doAttach(): void {
    if (!this.attachTarget || !this.selectedAgentId || this.attaching) return;
    this.attaching = true;
    this.api.attachVolume(this.projectId, this.attachTarget.name, this.selectedAgentId).subscribe({
      next: () => {
        this.showToast('Attach queued — disk appears on your VM within 10s', 'success');
        this.attaching = false;
        this.closeAttachModal();
        this.loadVolumes();
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Attach failed', 'error');
        this.attaching = false;
      }
    });
  }

  // ── Extent modal ──────────────────────────────────────────────────────────

  openExtentModal(name: string): void {
    this.extentTarget = name;
    this.newExtent = { name: name + '-ext1', sizeGB: 10 };
    this.showExtentModal = true;
  }

  closeExtentModal(): void { this.showExtentModal = false; this.extentTarget = null; }

  appendExtent(): void {
    if (!this.extentTarget || this.extendingVolume) return;
    this.extendingVolume = true;
    this.api.appendBlockVolumeExtent(this.projectId, this.extentTarget, {
      name: this.newExtent.name,
      sizeGB: this.newExtent.sizeGB
    }).subscribe({
      next: () => {
        this.showToast('Disk appended', 'success');
        this.extendingVolume = false;
        this.closeExtentModal();
        this.loadVolumes();
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Failed to append disk', 'error');
        this.extendingVolume = false;
      }
    });
  }

  // ── Wizard ────────────────────────────────────────────────────────────────

  openWizard(): void {
    this.wizardStep = 1;
    this.agentOs = 'linux';
    this.showWizard = true;
  }

  closeWizard(): void {
    this.showWizard = false;
    this.loadAgents(); // refresh in case they registered
  }

  nextWizardStep(): void {
    if (this.wizardStep < 4) this.wizardStep = (this.wizardStep + 1) as WizardStep;
  }

  prevWizardStep(): void {
    if (this.wizardStep > 1) this.wizardStep = (this.wizardStep - 1) as WizardStep;
  }

  // ── Wizard command builders ───────────────────────────────────────────────

  getDownloadCommand(): string {
    const url = `${this.serverUrl}/api/agent/download`;
    return this.agentOs === 'linux'
      ? `curl ${url} -o staas_agent.py`
      : `Invoke-WebRequest -Uri "${url}" -OutFile staas_agent.py`;
  }

  getPrereqCommand(): string {
    return this.agentOs === 'linux'
      ? 'sudo apt install open-iscsi -y && pip3 install requests'
      : '# Run as Administrator:\nEnable-WindowsOptionalFeature -Online -FeatureName MicrosoftISCSIInitiator\npip install requests';
  }

  getRegisterCommand(): string {
    const jwt = this.getUserJwt();
    const py = this.agentOs === 'linux' ? 'python3' : 'python';
    return `${py} staas_agent.py register \\\n  --server ${this.serverUrl} \\\n  --token ${jwt || '<paste-your-login-jwt>'}`;
  }

  getStartCommand(): string {
    return this.agentOs === 'linux'
      ? 'python3 staas_agent.py start'
      : 'python staas_agent.py start';
  }

  getAgentDownloadUrl(): string {
    return `${this.serverUrl}/api/agent/download`;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  copy(text: string, key: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.copiedKey = key;
      setTimeout(() => this.copiedKey = null, 2000);
    });
  }

  isCopied(key: string): boolean {
    return this.copiedKey === key;
  }

  statusClass(s: string): string {
    if (['READY','ISCSI_READY'].includes(s)) return 'ready';
    if (s === 'ERROR') return 'error';
    if (s === 'DELETING' || s === 'DELETED') return 'deleting';
    return 'pending';
  }

  statusLabel(s: string): string {
    const map: Record<string,string> = {
      ISCSI_READY:'Ready', READY:'Ready (ESXi)',
      PENDING:'Pending', RBD_CREATED:'Creating image…',
      TARGET_CREATED:'Creating target…', GATEWAY_ADDED:'Adding gateway…',
      DISK_EXPOSED:'Exposing disk…', HOST_MAPPED:'Mapping host…',
      LUN_READY:'LUN active', ESXI_RESCANNED:'Scanning ESXi…',
      DATASTORE_CREATED:'Creating datastore…',
      DELETING:'Deleting…', DELETED:'Deleted', ERROR:'Error',
    };
    return map[s] || s;
  }

  isReady(v: any): boolean {
    return ['ISCSI_READY','READY'].includes(v.status);
  }

  trackBy(_: number, item: any) { return item.id || item.name; }

  private showToast(message: string, type: 'success'|'error'|'info') {
    this.toast = { message, type };
    setTimeout(() => this.toast = null, 4500);
  }
}