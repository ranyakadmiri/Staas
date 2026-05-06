import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ChangeDetectorRef, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { User, UserStatus } from '../../models/user.model';
import { Api } from '../../services/api';
@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css',
})
export class AdminUsers implements OnInit {
  pendingUsers: User[] = [];
  loading = false;
  processingUserId: number | null = null;

  errorMessage: string = '';
  successMessage: string = '';

  constructor(
    private adminService: Api,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPendingUsers();
  }

  // ========================
  // LOAD USERS
  // ========================

  loadPendingUsers(): void {
    this.loading = true;
    this.errorMessage = '';

    this.adminService.getPendingUsers().subscribe({
      next: (users) => {
        console.log("USERS:", users);

        this.pendingUsers = users;

        this.loading = false;

        this.cd.detectChanges(); // 🔥 important
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.errorMessage = 'Failed to load pending users.';
        this.loading = false;
      }
    });
  }

  // ========================
  // APPROVE
  // ========================

  approveUser(userId: number): void {
    if (!confirm('Approve this user?')) return;

    this.processingUserId = userId;
    this.clearMessages();

    this.adminService.approveUser(userId).subscribe({
      next: (response) => {
        this.successMessage = response;

        this.removeUserFromList(userId);

        this.processingUserId = null;
      },
      error: (error) => {
        console.error(error);
        this.errorMessage = 'Failed to approve user.';
        this.processingUserId = null;
      }
    });
  }

  // ========================
  // REJECT
  // ========================

  rejectUser(userId: number): void {
    if (!confirm('Reject this user?')) return;

    this.processingUserId = userId;
    this.clearMessages();

    this.adminService.rejectUser(userId).subscribe({
      next: (response) => {
        this.successMessage = response;

        this.removeUserFromList(userId);

        this.processingUserId = null;
      },
      error: (error) => {
        console.error(error);
        this.errorMessage = 'Failed to reject user.';
        this.processingUserId = null;
      }
    });
  }

  // ========================
  // HELPERS
  // ========================

  private removeUserFromList(userId: number): void {
    this.pendingUsers = this.pendingUsers.filter(u => u.id !== userId);

    if (this.pendingUsers.length === 0) {
      this.successMessage = 'All users processed!';
    }

    this.cd.detectChanges();
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getStatusBadgeClass(status: UserStatus): string {
    switch(status) {
      case UserStatus.PENDING:
        return 'badge badge-warning';
      case UserStatus.APPROVED:
        return 'badge badge-success';
      case UserStatus.REJECTED:
        return 'badge badge-danger';
      default:
        return 'badge badge-secondary';
    }
  }
}