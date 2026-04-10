import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  

  private apiUrl = 'http://localhost:8080/api/auth';
  private emailSubject = new BehaviorSubject<string | null>(localStorage.getItem('email'));
  email$ = this.emailSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  // Méthode de connexion

  login(credentials: { email: string; password: string }) {
  return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
    tap((response) => {
      console.log('Réponse login backend:', response); // Debug affichage réponse
      localStorage.setItem('token', response.token);
      localStorage.setItem('email', response.email);
      this.emailSubject.next(response.email);
    })
  );
}


  // Récupérer l'email de l'utilisateur connecté
  getEmail(): string | null {
    return localStorage.getItem('email');
  }


getUserIdByEmail(email: string): Observable<number> {
  return this.http.get<number>(`http://192.168.49.2:30089/PGH/api/auth/GetUserIdByEmail?email=${email}`);
}

  // Stocker le token JWT
  setToken(token: string): void {
    localStorage.setItem('authToken', token);
  }

  // Récupérer le token JWT
  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  // Vérifier si l'utilisateur est authentifié
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  // Déconnexion
logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('email');
  this.emailSubject.next(null);
  this.router.navigate(['/login']); // redirect to login page after logout
}

}