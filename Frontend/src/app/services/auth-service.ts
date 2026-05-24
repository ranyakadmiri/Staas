import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private emailSubject = new BehaviorSubject<string | null>(localStorage.getItem('email'));
  email$ = this.emailSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  // Step 1: just sends credentials, returns mfaToken from backend
  login(credentials: { email: string; password: string }) {
    return this.http.post<any>(`${this.apiUrl}/login`, credentials);
    // ⚠️ No tap() here anymore — we don't store anything until OTP is verified
  }

  // Called after OTP is verified successfully
  finalizeLogin(token: string, email: string): void {
    localStorage.setItem('authToken', token);
    localStorage.setItem('email', email);
    this.emailSubject.next(email);
  }

  getEmail(): string | null {
    return localStorage.getItem('email');
  }

  getUserIdByEmail(email: string): Observable<number> {
    return this.http.get<number>(
      `http://192.168.49.2:30089/PGH/api/auth/GetUserIdByEmail?email=${email}`
    );
  }

  setToken(token: string): void {
    localStorage.setItem('authToken', token);
  }

  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('email');
    this.emailSubject.next(null);
    this.router.navigate(['/login']);
  }
}