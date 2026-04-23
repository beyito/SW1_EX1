import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../auth.service';

export interface UploadedFile {
  key: string;
  url: string;
  contentType: string;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class FileService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  public async uploadAttachment(file: File, policyId?: string): Promise<UploadedFile> {
    const formData = new FormData();
    formData.append('file', file);
    if (policyId) {
      formData.append('policyId', policyId);
    }

    const token = this.authService.getToken();
    const headers = token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : undefined;

    return await firstValueFrom(
      this.http.post<UploadedFile>('/api/files/upload', formData, { headers })
    );
  }
}

