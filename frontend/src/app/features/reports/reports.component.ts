import { Component } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { ReportService } from '../../core/services/report.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html'
})
export class ReportsComponent {
  downloading = false;

  constructor(
    private readonly reports: ReportService,
    private readonly toast: ToastService,
    readonly auth: AuthService
  ) {}

  download(): void {
    this.downloading = true;
    this.reports.downloadTaskReport().subscribe({
      next: ({ blob, fileName }) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
        this.downloading = false;
        this.toast.success('Report downloaded.');
      },
      error: () => (this.downloading = false) // the error interceptor already shows the message
    });
  }
}
