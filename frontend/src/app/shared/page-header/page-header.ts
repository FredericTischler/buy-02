import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './page-header.html',
  styleUrl: './page-header.scss'
})
export class PageHeader {
  @Input() title = '';
  @Input() showBack = true;
  @Input() backRoute = '/products';
  @Output() back = new EventEmitter<void>();

  constructor(private readonly router: Router) {}

  onBack(): void {
    if (this.back.observed) {
      this.back.emit();
    } else {
      this.router.navigate([this.backRoute]);
    }
  }
}