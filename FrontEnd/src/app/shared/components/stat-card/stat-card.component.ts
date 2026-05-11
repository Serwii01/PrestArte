import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';

type Accent = 'primary' | 'emerald' | 'orange' | 'purple' | 'red' | 'blue';

/** Mapa explícito: Tailwind no puede inferir clases construidas dinámicamente. */
const ACCENT_CLASSES: Record<Accent, string> = {
  primary: 'text-primary bg-primary/10',
  emerald: 'text-emerald-600 bg-emerald-100',
  orange: 'text-orange-500 bg-orange-100',
  purple: 'text-purple-600 bg-purple-100',
  red: 'text-red-600 bg-red-100',
  blue: 'text-blue-600 bg-blue-100',
};

/**
 * Tarjeta de estadística para los dashboards (estilo Stitch):
 * label arriba-izquierda, icono arriba-derecha en cuadradito tintado, valor grande abajo.
 */
@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.scss',
})
export class StatCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly icon = input.required<string>();
  readonly accent = input<Accent>('primary');
  readonly hint = input<string | undefined>(undefined);

  protected readonly iconClasses = computed(() => ACCENT_CLASSES[this.accent()]);
}
