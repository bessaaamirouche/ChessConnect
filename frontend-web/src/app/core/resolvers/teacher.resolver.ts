import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { TeacherService } from '../services/teacher.service';
import { User } from '../models/user.model';
import { catchError, of } from 'rxjs';

export const teacherResolver: ResolveFn<User | null> = (route) => {
  const teacherService = inject(TeacherService);
  const uuid = route.paramMap.get('uuid');

  if (!uuid) {
    return of(null);
  }

  return teacherService.getTeacherByUuid(uuid).pipe(
    catchError(() => of(null))
  );
};
