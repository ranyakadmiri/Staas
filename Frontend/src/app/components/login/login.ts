import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../services/api';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth-service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  loginForm: FormGroup;
  otpForm: FormGroup;
  errorMessage: string | null = null;
  isLoading = false;
  mfaToken: string | null = null;

  companyTips = [
    "Explorez les dernières opportunités de carrière chez Poulina.",
    "Collaborez avec une équipe d'experts passionnés.",
    "Faites partie d'un groupe en constante évolution.",
    "Votre avenir commence ici, rejoignez-nous."
  ];
  welcomeMessage = this.companyTips[0];

  constructor(
    private fb: FormBuilder,
    private api: Api,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef   // ← add this
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
    });
  }

  ngOnInit(): void {
    this.welcomeMessage =
      this.companyTips[Math.floor(Math.random() * this.companyTips.length)];
  }

  dismissError(): void {
    this.errorMessage = null;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = null;

    this.authService.login(this.loginForm.value).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.mfaRequired && res.mfaToken) {
          this.mfaToken = res.mfaToken;
          this.cdr.detectChanges();   // ← force re-render
        } else {
          this.authService.finalizeLogin(res.token, res.email);
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.errorMessage = "Adresse e-mail ou mot de passe invalide.";
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onVerifyOtp(): void {
    if (this.otpForm.invalid || !this.mfaToken) return;

    this.isLoading = true;
    this.errorMessage = null;

    this.api.verifyOtp(this.mfaToken, this.otpForm.value.otp).subscribe({
      next: (res) => {
        this.authService.finalizeLogin(res.token, res.email);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.errorMessage = "Code invalide ou expiré. Veuillez réessayer.";
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  cancelMfa(): void {
    this.mfaToken = null;
    this.otpForm.reset();
    this.errorMessage = null;
    this.cdr.detectChanges();
  }
}