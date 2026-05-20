import { Component, signal, computed, OnInit } from '@angular/core';
import { RouterLink, RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('staas-ui');
  isDarkMode = signal(false);
  currentRoute = signal('');

  // Routes where we hide the sidebar/topnav (auth pages)
  isAuthRoute = computed(() => {
    const r = this.currentRoute();
    return r === '/' || r === '' || r.startsWith('/register');
  });

  constructor(private router: Router) {}

  ngOnInit() {
    // Restore saved theme
    const saved = localStorage.getItem('theme');
    if (saved === 'dark') {
      this.isDarkMode.set(true);
      document.body.classList.add('dark');
    }

    // Track route changes
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        this.currentRoute.set(e.urlAfterRedirects ?? e.url);
      });

    // Set initial route
    this.currentRoute.set(this.router.url);
  }

  toggleTheme() {
    const dark = !this.isDarkMode();
    this.isDarkMode.set(dark);
    document.body.classList.toggle('dark', dark);
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  }
}
