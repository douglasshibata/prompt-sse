import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PromptEvent {
  user: string;
  ai: string;
  time: string;
}

@Injectable({
  providedIn: 'root'
})
export class SseService {
  private eventSource: EventSource | null = null;

  connect(url: string = '/api/events'): Observable<PromptEvent> {
    return new Observable<PromptEvent>(observer => {
      this.eventSource = new EventSource(url);

      this.eventSource.addEventListener('response', (event: MessageEvent) => {
        try {
          const data: PromptEvent = JSON.parse(event.data);
          observer.next(data);
        } catch (e) {
          console.error('Error parsing SSE data', e);
        }
      });

      this.eventSource.onerror = (error) => {
        console.error('SSE error', error);
        observer.error(error);
      };

      return () => {
        this.disconnect();
      };
    });
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
