import { Signal, signal, computed, WritableSignal } from '@angular/core';

export interface PaginationState<T> {
  paginatedItems: Signal<T[]>;
  currentPage: WritableSignal<number>;
  pageSize: WritableSignal<number>;
  totalItems: Signal<number>;
  totalPages: Signal<number>;
  hasPreviousPage: Signal<boolean>;
  hasNextPage: Signal<boolean>;
  startItem: Signal<number>;
  endItem: Signal<number>;
  visiblePages: Signal<number[]>;
  goToPage: (page: number) => void;
  nextPage: () => void;
  previousPage: () => void;
}

export function paginate<T>(
  source: Signal<T[]>,
  initialPageSize: number = 10
): PaginationState<T> {
  const currentPage = signal(0);
  const pageSize = signal(initialPageSize);

  const totalItems = computed(() => source().length);
  const totalPages = computed(() => Math.max(1, Math.ceil(totalItems() / pageSize())));

  const paginatedItems = computed(() => {
    const items = source();
    const page = Math.min(currentPage(), Math.max(0, Math.ceil(items.length / pageSize()) - 1));
    const size = pageSize();
    const start = page * size;
    return items.slice(start, start + size);
  });

  const hasPreviousPage = computed(() => currentPage() > 0);
  const hasNextPage = computed(() => currentPage() < totalPages() - 1);

  const startItem = computed(() => {
    if (totalItems() === 0) return 0;
    return currentPage() * pageSize() + 1;
  });

  const endItem = computed(() =>
    Math.min((currentPage() + 1) * pageSize(), totalItems())
  );

  const visiblePages = computed(() => {
    const total = totalPages();
    const current = currentPage();
    const maxVisible = 5;

    if (total <= maxVisible) {
      return Array.from({ length: total }, (_, i) => i);
    }

    let start = Math.max(0, current - Math.floor(maxVisible / 2));
    const end = Math.min(total, start + maxVisible);
    start = end - maxVisible;

    return Array.from({ length: end - start }, (_, i) => start + i);
  });

  const goToPage = (page: number) => {
    currentPage.set(Math.max(0, Math.min(page, totalPages() - 1)));
  };

  const nextPage = () => {
    if (hasNextPage()) currentPage.update(p => p + 1);
  };

  const previousPage = () => {
    if (hasPreviousPage()) currentPage.update(p => p - 1);
  };

  return {
    paginatedItems,
    currentPage,
    pageSize,
    totalItems,
    totalPages,
    hasPreviousPage,
    hasNextPage,
    startItem,
    endItem,
    visiblePages,
    goToPage,
    nextPage,
    previousPage,
  };
}
