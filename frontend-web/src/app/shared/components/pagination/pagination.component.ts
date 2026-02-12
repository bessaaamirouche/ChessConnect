import { Component, input, output } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroChevronLeft, heroChevronRight } from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-pagination',
  imports: [TranslateModule, NgIconComponent],
  providers: [provideIcons({ heroChevronLeft, heroChevronRight })],
  template: `
    @if (totalPages() > 1) {
      <nav class="pagination" [attr.aria-label]="'pagination.ariaLabel' | translate">
        <span class="pagination__info">
          {{ 'pagination.showing' | translate : {
            start: startItem(),
            end: endItem(),
            total: totalItems()
          } }}
        </span>

        <div class="pagination__controls">
          <button
            class="pagination__btn"
            [disabled]="currentPage() === 0"
            (click)="pageChange.emit(currentPage() - 1)"
            [attr.aria-label]="'pagination.previous' | translate"
          >
            <ng-icon name="heroChevronLeft" size="16"></ng-icon>
            <span class="pagination__btn-text">{{ 'pagination.previous' | translate }}</span>
          </button>

          <div class="pagination__pages">
            @for (page of visiblePages(); track page) {
              <button
                class="pagination__page"
                [class.pagination__page--active]="page === currentPage()"
                (click)="pageChange.emit(page)"
                [attr.aria-label]="'pagination.goToPage' | translate : { page: page + 1 }"
                [attr.aria-current]="page === currentPage() ? 'page' : null"
              >
                {{ page + 1 }}
              </button>
            }
          </div>

          <button
            class="pagination__btn"
            [disabled]="currentPage() >= totalPages() - 1"
            (click)="pageChange.emit(currentPage() + 1)"
            [attr.aria-label]="'pagination.next' | translate"
          >
            <span class="pagination__btn-text">{{ 'pagination.next' | translate }}</span>
            <ng-icon name="heroChevronRight" size="16"></ng-icon>
          </button>
        </div>
      </nav>
    }
  `,
  styles: [`
    .pagination {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: var(--space-md, 1rem) 0;
      gap: var(--space-md, 1rem);
      flex-wrap: wrap;
    }

    .pagination__info {
      font-size: 0.8125rem;
      color: rgba(255, 255, 255, 0.4);
    }

    .pagination__controls {
      display: flex;
      align-items: center;
      gap: var(--space-xs, 0.25rem);
    }

    .pagination__btn {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 6px 12px;
      font-size: 0.8125rem;
      font-weight: 500;
      font-family: inherit;
      color: rgba(255, 255, 255, 0.6);
      background: transparent;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover:not(:disabled) {
        background: rgba(255, 255, 255, 0.05);
        color: rgba(255, 255, 255, 0.9);
        border-color: rgba(255, 255, 255, 0.2);
      }

      &:disabled {
        opacity: 0.35;
        cursor: not-allowed;
      }
    }

    .pagination__btn-text {
      @media (max-width: 480px) {
        display: none;
      }
    }

    .pagination__pages {
      display: flex;
      gap: 2px;
    }

    .pagination__page {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8125rem;
      font-weight: 500;
      font-family: inherit;
      color: rgba(255, 255, 255, 0.6);
      background: transparent;
      border: 1px solid transparent;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover:not(.pagination__page--active) {
        background: rgba(255, 255, 255, 0.05);
        color: rgba(255, 255, 255, 0.9);
      }

      &--active {
        background: rgba(212, 168, 75, 0.15);
        border-color: rgba(212, 168, 75, 0.3);
        color: #D4A84B;
        font-weight: 600;
      }
    }

    @media (max-width: 480px) {
      .pagination {
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
      }
    }
  `]
})
export class PaginationComponent {
  currentPage = input.required<number>();
  totalPages = input.required<number>();
  totalItems = input.required<number>();
  startItem = input.required<number>();
  endItem = input.required<number>();
  visiblePages = input.required<number[]>();

  pageChange = output<number>();
}
