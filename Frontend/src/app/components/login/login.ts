import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../services/api';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth-service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule,CommonModule ,RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login  implements OnInit {
  loginForm: FormGroup;
  errorMessage: string | null = null;
  isLoading = false;
  welcomeMessage = "Bienvenue chez NextStep - Connectez-vous pour continuer";
  companyTips = [
    "Explorez les dernières opportunités de carrière chez Poulina.",
    "Collaborez avec une équipe d'experts passionnés.",
    "Faites partie d'un groupe en constante évolution.",
    "Votre avenir commence ici, rejoignez-nous."
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.showWelcomeMessage();
  }

  showWelcomeMessage(): void {
    this.welcomeMessage = this.companyTips[Math.floor(Math.random() * this.companyTips.length)];
  }

  dismissError(): void {
    this.errorMessage = null;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading = true;

    const credentials = this.loginForm.value;
    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.authService.setToken(response.token);
        this.router.navigate(['/dashboard']); // Redirige après connexion réussie
      },
      error: (error) => {
        this.errorMessage = "Adresse e-mail ou mot de passe invalide.";
        this.isLoading = false;
      }
    });
  }
}