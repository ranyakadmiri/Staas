import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Api } from '../../services/api';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './billing.html',
  styleUrl: './billing.css'
})
export class Billing implements OnInit {

  projectId!: number;
  invoices:     any[] = [];
  usageRecords: any[] = [];
  subscription: any   = null;

  loadingInvoices  = true;
  loadingUsage     = true;
  loadingSubscription = true;
  generatingInvoice = false;
  downloadingId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: Api,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.projectId = Number(this.route.snapshot.paramMap.get('projectId'));
    this.loadAll();
  }

  loadAll() {
    this.loadSubscription();
    this.loadUsage();
    this.loadInvoices();
  }

  loadSubscription() {
    this.loadingSubscription = true;
    this.api.getSubscription(this.projectId).subscribe({
      next: (res) => {
        this.subscription = res;
        this.loadingSubscription = false;
        this.cd.detectChanges();
      },
      error: () => {
        this.subscription = null;
        this.loadingSubscription = false;
        this.cd.detectChanges();
      }
    });
  }

  loadUsage() {
    this.loadingUsage = true;
    this.api.getUsageByProject(this.projectId).subscribe({
      next: (res) => {
        this.usageRecords = res;
        this.loadingUsage = false;
        this.cd.detectChanges();
      },
      error: () => {
        this.loadingUsage = false;
      }
    });
  }

  loadInvoices() {
    this.loadingInvoices = true;
    this.api.getInvoices(this.projectId).subscribe({
      next: (res) => {
        this.invoices = res;
        this.loadingInvoices = false;
        this.cd.detectChanges();
      },
      error: () => {
        this.loadingInvoices = false;
      }
    });
  }

  generateInvoice() {
    this.generatingInvoice = true;
    this.api.generateInvoiceNow(this.projectId).subscribe({
      next: () => {
        this.generatingInvoice = false;
        this.loadInvoices();
      },
      error: (err: any) => {
        console.error('Invoice generation failed', err);
        this.generatingInvoice = false;
      }
    });
  }

  downloadPdf(invoice: any) {
    this.downloadingId = invoice.id;
    this.api.downloadInvoicePdf(invoice.id).subscribe({
      next: (blob: any) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = invoice.invoiceNumber + '.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
        this.downloadingId = null;
      },
      error: () => {
        this.downloadingId = null;
      }
    });
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  getUsageByType(type: string): number {
    const record = this.usageRecords.find(r => r.resourceType === type);
    return record ? record.usedGB : 0;
  }

  usageBarWidth(usedGB: number, maxGB: number): string {
    if (!maxGB || maxGB === 0) return '0%';
    const pct = Math.min((usedGB / maxGB) * 100, 100);
    return pct + '%';
  }

  usageBarColor(usedGB: number, maxGB: number): string {
    if (!maxGB) return '#6366f1';
    const pct = (usedGB / maxGB) * 100;
    if (pct >= 90) return '#ef4444';
    if (pct >= 70) return '#f59e0b';
    return '#6366f1';
  }

  planLabel(): string {
    if (!this.subscription?.plan) return '—';
    const p = this.subscription.plan;
    if (p.type === 'PAY_AS_YOU_GO') return 'Pay As You Go';
    const m = p.durationMonths;
    if (m === 1)  return '1 Month Pack';
    if (m === 3)  return '3 Months Pack';
    if (m === 6)  return '6 Months Pack';
    if (m === 12) return 'Annual Pack';
    return p.name;
  }

  statusClass(status: string): string {
    switch (status) {
      case 'PAID':    return 'badge-paid';
      case 'UNPAID':  return 'badge-unpaid';
      case 'OVERDUE': return 'badge-overdue';
      default:        return '';
    }
  }

  formatGB(gb: number): string {
    if (gb < 0.001) return (gb * 1024).toFixed(3) + ' MB';
    return gb.toFixed(3) + ' GB';
  }

  goBack() {
    this.router.navigate(['/projects']);
  }
}