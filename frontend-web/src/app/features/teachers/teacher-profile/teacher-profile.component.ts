import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TeacherService } from '../../../core/services/teacher.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-teacher-profile',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './teacher-profile.component.html',
  styleUrl: './teacher-profile.component.scss'
})
export class TeacherProfileComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    public teacherService: TeacherService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const teacherId = this.route.snapshot.paramMap.get('id');
    if (teacherId) {
      this.teacherService.getTeacher(+teacherId).subscribe();
    }
  }

  formatPrice(cents: number | undefined): string {
    if (!cents) return '-';
    return (cents / 100).toFixed(0) + '€';
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }
}
