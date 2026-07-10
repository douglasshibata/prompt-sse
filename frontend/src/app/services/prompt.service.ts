import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PromptService {
  private apiUrl = '/api/prompt';

  constructor(private http: HttpClient) {}

  sendPrompt(prompt: string, sessionId: string): Observable<any> {
    const headers = new HttpHeaders({
      'X-Session-Id': sessionId
    });
    return this.http.post(this.apiUrl, { prompt }, { headers });
  }
}
