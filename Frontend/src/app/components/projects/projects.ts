import { ChangeDetectorRef, Component } from '@angular/core';
import { Api } from '../../services/api';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-projects',
  imports: [FormsModule, CommonModule],
  templateUrl: './projects.html',
  styleUrl: './projects.css',
})
export class Projects {

  projects: any[] = [];
  plans: any[] = [];

  showCreate = false;
  step: 'form' | 'plan' = 'form';

  createdProjectId: number | null = null;
  selectedPlanId: number | null = null;
  subscribing = false;
  creating = false;

  project: any = {
    name: '',
    description: '',
    storageType: 'OBJECT',
    region: 'default',
    maxBuckets: 100,
    maxStorageGB: 1000
  };

  constructor(
    private api: Api,
    private router: Router,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadProjects();
    this.loadPlans();
  }

  loadProjects() {
    this.api.getProjects().subscribe({
      next: (res: any) => {
        this.projects = res;
        this.cd.detectChanges();
      },
      error: (err) => console.error('Projects loading failed', err)
    });
  }

  loadPlans() {
    this.api.getPlans().subscribe({
      next: (res: any[]) => this.plans = res,
      error: (err) => console.error('Plans loading failed', err)
    });
  }

  createProject() {
    if (!this.project.name) return;
    this.creating = true;

    this.api.createProject(this.project).subscribe({
      next: (res: any) => {
        this.createdProjectId = res.id;
        this.creating = false;
        this.step = 'plan';
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Project creation failed', err);
        this.creating = false;
      }
    });
  }

  confirmPlan() {
    if (!this.selectedPlanId || !this.createdProjectId) return;
    this.subscribing = true;

    this.api.subscribe(this.createdProjectId, this.selectedPlanId).subscribe({
      next: () => {
        this.subscribing = false;
        this.showCreate = false;
        this.resetForm();
        this.loadProjects();
      },
      error: (err) => {
        console.error('Subscription failed', err);
        this.subscribing = false;
      }
    });
  }

  resetForm() {
    this.step = 'form';
    this.createdProjectId = null;
    this.selectedPlanId = null;
    this.project = {
      name: '', description: '',
      storageType: 'OBJECT', region: 'default',
      maxBuckets: 100, maxStorageGB: 1000
    };
  }

  openProject(id: number)  { this.router.navigate(['/buckets', id]); }
  openFiles(id: number)    { this.router.navigate(['/file-shares', id]); }
  openBlock(id: number)    { this.router.navigate(['/iscsii-storage', id]); }
  openBilling(id: number)  { this.router.navigate(['/billing', id]); }

  planLabel(type: string, months: number | null): string {
    if (type === 'PAY_AS_YOU_GO') return 'Pay As You Go';
    if (months === 1)  return '1 Month Pack';
    if (months === 3)  return '3 Months Pack';
    if (months === 6)  return '6 Months Pack';
    if (months === 12) return 'Annual Pack';
    return type;
  }
}