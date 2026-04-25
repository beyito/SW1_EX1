import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface CopilotChatMessage {
  role: 'user' | 'assistant' | 'system';
  text: string;
  timestamp: string;
  suggestedActions?: string[];
}

@Component({
  selector: 'app-copilot-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './copilot-chat.component.html',
  styleUrl: './copilot-chat.component.scss'
})
export class CopilotChatComponent {
  @Input() messages: CopilotChatMessage[] = [];
  @Input() loading = false;

  @Output() messageSubmitted = new EventEmitter<string>();
  @Output() analyzeRequested = new EventEmitter<void>();

  public inputText = '';

  public submitMessage(): void {
    const text = this.inputText.trim();
    if (!text || this.loading) {
      return;
    }
    this.messageSubmitted.emit(text);
    this.inputText = '';
  }

  public requestAnalyze(): void {
    if (this.loading) {
      return;
    }
    this.analyzeRequested.emit();
  }
}
