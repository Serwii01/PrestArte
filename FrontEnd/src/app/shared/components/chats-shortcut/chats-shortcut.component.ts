import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ChatListService, ChatSummary } from '../../../core/services/chat-list.service';

/**
 * Tarjeta de acceso directo a los chats del usuario. Se inserta en cada
 * dashboard (admin, coleccionista, fundación, transporte) para que la entrada
 * a una conversación esté a un click desde el panel principal, además de la
 * vía habitual desde el detalle del préstamo.
 */
@Component({
  selector: 'app-chats-shortcut',
  standalone: true,
  imports: [CommonModule, RouterLink],
  // Solo `block` para respetar space-y-*/mb-* del padre. Si el dashboard
  // quiere que la tarjeta se estire para igualar a un vecino (caso de
  // collector), basta con añadir `class="h-full"` desde fuera.
  host: { class: 'block' },
  template: `
    <div class="stitch-card overflow-hidden h-full flex flex-col">
      <div class="px-6 py-5 border-b border-border flex items-center justify-between shrink-0">
        <h3 class="text-lg font-bold text-text-main flex items-center gap-2">
          <span class="material-symbols-outlined text-primary" style="font-size:22px;">forum</span>
          Conversaciones
        </h3>
        @if (chats().length > 0) {
          <span class="text-text-secondary text-xs">
            {{ chats().length }} {{ chats().length === 1 ? 'chat' : 'chats' }}
          </span>
        }
      </div>

      @if (loading()) {
        <div class="p-6 text-center text-text-secondary text-sm">Cargando…</div>
      } @else if (chats().length === 0) {
        <div class="p-8 text-center">
          <span class="material-symbols-outlined text-text-secondary mb-1" style="font-size:32px;">
            chat_bubble_outline
          </span>
          <p class="text-sm text-text-secondary">
            Todavía no tienes conversaciones activas.
          </p>
          <p class="text-xs text-text-secondary mt-1">
            Cuando participes en un préstamo, su chat aparecerá aquí.
          </p>
        </div>
      } @else {
        <ul class="flex-1 min-h-0 overflow-y-auto divide-y divide-border">
          @for (c of chats(); track c.loanId) {
            <li>
              <a [routerLink]="['/app/loans', c.loanId, 'chat']"
                 class="flex items-center gap-3 px-5 py-3 hover:bg-background transition-colors">
                <div class="size-10 rounded-full bg-primary/10 text-primary flex items-center justify-center shrink-0">
                  <span class="material-symbols-outlined" style="font-size:20px;">forum</span>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-bold text-text-main truncate" [title]="c.artworkTitle">
                    {{ c.artworkTitle }}
                  </p>
                  <p class="text-xs text-text-secondary truncate">
                    @if (c.counterpartyName) {
                      {{ c.counterpartyName }}
                    } @else if (c.artworkArtist) {
                      {{ c.artworkArtist }}
                    } @else {
                      Conversación del préstamo
                    }
                  </p>
                </div>
                <span class="material-symbols-outlined text-text-secondary shrink-0" style="font-size:20px;">
                  chevron_right
                </span>
              </a>
            </li>
          }
        </ul>
      }
    </div>
  `,
})
export class ChatsShortcutComponent implements OnInit {
  private readonly chatList = inject(ChatListService);

  protected readonly chats = signal<ChatSummary[]>([]);
  protected readonly loading = signal(true);

  ngOnInit(): void {
    this.chatList.getMyChats().subscribe({
      next: (list) => {
        this.chats.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
