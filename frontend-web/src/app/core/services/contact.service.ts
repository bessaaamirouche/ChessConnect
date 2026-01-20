import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ContactAdminRequest {
  name: string;
  email: string;
  subject: string;
  message: string;
}

export interface ContactResponse {
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private readonly apiUrl = '/api/contact';
  private http = inject(HttpClient);

  contactAdmin(request: ContactAdminRequest): Observable<ContactResponse> {
    return this.http.post<ContactResponse>(`${this.apiUrl}/admin`, request);
  }
}
