import { Component, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { SseService, PromptEvent } from './services/sse.service';
import { PromptService } from './services/prompt.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: false
})
export class AppComponent implements OnDestroy {
  title = 'Chatbot Demo';
  events: PromptEvent[] = [];
  connected = false;
  userInput = '';
  sessionId = 'demo-session-' + Date.now(); // Simple session ID
  private subscription: Subscription | null = null;

  constructor(
    private sseService: SseService,
    private promptService: PromptService
  ) {}

  connect(): void {
    if (this.connected) return;

    this.connected = true;

    this.subscription = this.sseService.connect().subscribe({
      next: (event: PromptEvent) => {
        this.events.push(event);
      },
      error: (err) => {
        console.error('SSE error', err);
        this.connected = false;
      }
    });
  }

  disconnect(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
    this.sseService.disconnect();
    this.connected = false;
  }

  sendPrompt(): void {
    if (!this.userInput.trim()) return;

    this.promptService.sendPrompt(this.userInput, this.sessionId).subscribe({
      next: () => {
        this.userInput = '';
      },
      error: (err) => {
        console.error('Failed to send prompt', err);
      }
    });
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
