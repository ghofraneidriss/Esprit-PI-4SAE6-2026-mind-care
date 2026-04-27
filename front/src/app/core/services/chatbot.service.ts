import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private api = '/api/users/chatbot';

  constructor(private http: HttpClient) {}

  sendMessage(message: string): Observable<{ reply: string }> {
    return this.http.post<{ reply: string }>(`${this.api}/chat`, { message });
  }
}
