import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { ChatbotService } from '../../core/services/chatbot.service';

interface ChatMessage {
  role: 'user' | 'bot';
  content: string;
  time: string;
}

@Component({
  selector: 'app-chat',
  standalone: false,
  templateUrl: './chat.html',
  styleUrls: ['./chat.css'],
})
export class ChatPage implements OnInit, AfterViewChecked {
  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  messages: ChatMessage[] = [];
  userInput = '';
  loading = false;
  private shouldScroll = false;

  readonly suggestions = [
    'How to manage patient agitation?',
    'Tips for nighttime wandering',
    'What are early Alzheimer symptoms?',
    'Daily activities for memory care',
  ];

  constructor(private chatbotService: ChatbotService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.messages.push({
      role: 'bot',
      content: "Hello! I'm your MindCare assistant, focused on Alzheimer's care. How can I help you today?",
      time: this.getTime()
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  send(): void {
    const message = this.userInput.trim();
    if (!message || this.loading) return;

    this.messages.push({ role: 'user', content: message, time: this.getTime() });
    this.userInput = '';
    this.loading = true;
    this.shouldScroll = true;

    this.chatbotService.sendMessage(message).subscribe({
      next: (res) => {
        this.messages.push({ role: 'bot', content: res.reply, time: this.getTime() });
        this.loading = false;
        this.shouldScroll = true;
        this.cdr.detectChanges();
      },
      error: () => {
        this.messages.push({
          role: 'bot',
          content: 'Sorry, the AI service is currently unavailable. Please check that the backend is running and your Groq API key is configured.',
          time: this.getTime()
        });
        this.loading = false;
        this.shouldScroll = true;
        this.cdr.detectChanges();
      }
    });
  }

  useSuggestion(text: string): void {
    this.userInput = text;
    this.send();
  }

  onEnter(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  private scrollToBottom(): void {
    try {
      const el = this.messagesContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    } catch {}
  }

  private getTime(): string {
    return new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }
}
