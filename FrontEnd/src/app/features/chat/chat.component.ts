import { CommonModule } from '@angular/common';
import {
  AfterViewChecked,
  Component,
  computed,
  ElementRef,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  signal,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription, interval, switchMap, of, catchError } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { ChatService } from '../../core/services/chat.service';
import { ChatListService, ChatSummary } from '../../core/services/chat-list.service';
import { LoanService } from '../../core/services/loan.service';
import {
  ChatSessionResponse,
  MessageResponse,
} from '../../core/models/chat.models';
import { LoanResponse } from '../../core/models/loan.models';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent implements OnInit, OnChanges, OnDestroy, AfterViewChecked {
  private readonly chatService = inject(ChatService);
  private readonly chatListService = inject(ChatListService);
  private readonly loanService = inject(LoanService);
  protected readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  /** Lista de chats del usuario para la barra lateral. */
  protected readonly chats = signal<ChatSummary[]>([]);
  protected readonly chatsLoading = signal(true);
  /** Si la barra lateral está abierta en móvil (se controla con un botón). */
  protected readonly sidebarOpen = signal(false);

  /** Vinculado a `:id` por withComponentInputBinding(). */
  @Input() id?: string;

  @ViewChild('messagesEnd') private messagesEnd?: ElementRef<HTMLDivElement>;

  protected readonly chat = signal<ChatSessionResponse | null>(null);
  protected readonly loan = signal<LoanResponse | null>(null);
  protected readonly messages = signal<MessageResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly forbidden = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly sending = signal(false);

  private pollSub?: Subscription;
  private shouldScroll = true;

  protected readonly form = this.fb.nonNullable.group({
    content: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  protected readonly chatClosed = computed(() => this.chat()?.estado === 'CERRADO');

  ngOnInit(): void {
    // Lista de chats para la barra lateral (independiente del chat concreto).
    this.chatListService.getMyChats().subscribe({
      next: (list) => {
        this.chats.set(list);
        this.chatsLoading.set(false);
      },
      error: () => this.chatsLoading.set(false),
    });

    this.openChatForCurrentId();
  }

  /**
   * Cuando el usuario clica otro chat en la barra lateral, Angular reutiliza
   * este mismo componente (solo cambia el param `:id`). ngOnInit NO vuelve a
   * ejecutarse, así que enganchamos ngOnChanges para detectarlo.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['id'] && !changes['id'].firstChange) {
      this.openChatForCurrentId();
    }
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  /** Carga el chat asociado al `id` actual y arranca/reinicia el polling. */
  private openChatForCurrentId(): void {
    // Reset de estado para evitar mezclar datos del chat anterior.
    this.pollSub?.unsubscribe();
    this.pollSub = undefined;
    this.chat.set(null);
    this.loan.set(null);
    this.messages.set([]);
    this.loading.set(true);
    this.errorMessage.set(null);
    this.forbidden.set(false);
    this.shouldScroll = true;

    const loanId = Number(this.id);
    if (!loanId) {
      this.errorMessage.set('Identificador de préstamo inválido.');
      this.loading.set(false);
      return;
    }

    this.chatService.getOrCreateForLoan(loanId).subscribe({
      next: (session) => {
        this.chat.set(session);
        this.loadMessages();
        this.startPolling();
      },
      error: (err) => {
        this.loading.set(false);
        if (err?.status === 403) {
          this.forbidden.set(true);
        } else {
          this.errorMessage.set('No se pudo abrir el chat.');
        }
      },
    });

    this.loanService.getById(loanId).subscribe({
      next: (l) => this.loan.set(l),
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll && this.messagesEnd) {
      this.messagesEnd.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'end' });
      this.shouldScroll = false;
    }
  }

  private loadMessages(): void {
    const c = this.chat();
    if (!c) return;
    this.chatService.getMessages(c.id).subscribe({
      next: (msgs) => {
        const before = this.messages().length;
        this.messages.set(msgs);
        this.loading.set(false);
        if (msgs.length > before) this.shouldScroll = true;
      },
      error: () => this.loading.set(false),
    });
  }

  /** Polling cada 4 segundos para recibir mensajes nuevos. */
  private startPolling(): void {
    this.pollSub = interval(4000)
      .pipe(
        switchMap(() => {
          const c = this.chat();
          if (!c) return of([] as MessageResponse[]);
          return this.chatService.getMessages(c.id).pipe(catchError(() => of([] as MessageResponse[])));
        }),
      )
      .subscribe((msgs) => {
        if (msgs.length === 0) return;
        const before = this.messages().length;
        this.messages.set(msgs);
        if (msgs.length > before) this.shouldScroll = true;
      });
  }

  /** Archivo a adjuntar antes de enviar (preview en cliente). */
  protected pendingFile: File | null = null;

  send(): void {
    if (this.sending() || this.chatClosed()) return;
    const c = this.chat();
    if (!c) return;

    // Con adjunto: el contenido puede estar vacío.
    if (this.pendingFile) {
      this.sending.set(true);
      const content = this.form.controls.content.value ?? '';
      this.chatService.sendMessageWithFile(c.id, content, this.pendingFile).subscribe({
        next: (msg) => {
          this.messages.update((current) => [...current, msg]);
          this.form.reset({ content: '' });
          this.pendingFile = null;
          this.sending.set(false);
          this.shouldScroll = true;
        },
        error: (err) => {
          this.sending.set(false);
          this.errorMessage.set(err?.error?.message ?? 'No se pudo enviar el archivo.');
        },
      });
      return;
    }

    // Mensaje de solo texto.
    if (this.form.invalid) return;
    this.sending.set(true);
    this.chatService
      .sendMessage({ chatSessionId: c.id, content: this.form.controls.content.value })
      .subscribe({
        next: (msg) => {
          this.messages.update((current) => [...current, msg]);
          this.form.reset({ content: '' });
          this.sending.set(false);
          this.shouldScroll = true;
        },
        error: (err) => {
          this.sending.set(false);
          this.errorMessage.set(err?.error?.message ?? 'No se pudo enviar el mensaje.');
        },
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.pendingFile = input.files?.[0] ?? null;
    input.value = '';
  }

  clearPendingFile(): void {
    this.pendingFile = null;
  }

  attachmentUrl(fileId: string): string {
    return this.chatService.fileUrl(fileId);
  }

  isImageMessage(m: MessageResponse): boolean {
    return m.tipo === 'IMAGEN' || (m.attachmentFileType?.startsWith('image/') ?? false);
  }

  /** Es mi mensaje (alinear a la derecha, fondo primario). */
  isMine(m: MessageResponse): boolean {
    return m.senderId === this.auth.userId();
  }

  /** Color de avatar derivable del nombre. */
  avatarInitial(name: string): string {
    return name?.charAt(0).toUpperCase() ?? '?';
  }

  /** True si esa entrada de la lista lateral es la conversación actual. */
  isCurrentChat(loanId: number): boolean {
    return Number(this.id) === loanId;
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((v) => !v);
  }
}
