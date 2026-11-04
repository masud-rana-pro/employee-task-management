import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';

export interface DownloadedFile {
  blob: Blob;
  fileName: string;
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  constructor(private readonly http: HttpClient) {}

  /** Downloads the PDF as a Blob (the JWT interceptor adds the Authorization header). */
  downloadTaskReport(): Observable<DownloadedFile> {
    return this.http
      .get(`${environment.apiUrl}/reports/tasks/pdf`, { responseType: 'blob', observe: 'response' })
      .pipe(map(response => ({ blob: response.body as Blob, fileName: this.fileNameOf(response) })));
  }

  /** Reads the file name from the Content-Disposition header, with a safe fallback. */
  private fileNameOf(response: HttpResponse<Blob>): string {
    const header = response.headers.get('Content-Disposition') ?? '';
    const match = /filename="?([^";]+)"?/i.exec(header);
    return match ? match[1] : 'task-report.pdf';
  }
}
