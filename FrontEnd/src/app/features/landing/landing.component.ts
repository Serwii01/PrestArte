import { CommonModule } from '@angular/common';
import { Component, computed, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
})
export class LandingComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly year = new Date().getFullYear();
  protected readonly isLoggedIn = computed(() => this.auth.isLoggedIn());

  /** True cuando el usuario ha hecho scroll desde el top — el navbar se condensa. */
  protected readonly scrolled = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 8);
  }

  goToApp(): void {
    this.router.navigate(['/app']);
  }

  /** Datos de demostración para la sección "Currently on Loan" en la landing. */
  protected readonly demoArtworks = [
    {
      title: 'Las meninas',
      subtitle: 'Diego Velázquez · Óleo sobre lienzo',
      location: 'Madrid · Museo del Prado',
      img: '/assets/images/demo/las_meninas.jpg',
      available: false,
    },
    {
      title: 'La noche estrellada',
      subtitle: 'Vincent van Gogh · Óleo sobre lienzo',
      location: 'Nueva York · MoMA',
      img: '/assets/images/demo/noche_estrellada.jpg',
      available: true,
    },
    {
      title: 'La Gioconda',
      subtitle: 'Leonardo da Vinci · Óleo sobre tabla',
      location: 'París · Museo del Louvre',
      img: '/assets/images/demo/gioconda.jpg',
      available: false,
    },
  ];
}
