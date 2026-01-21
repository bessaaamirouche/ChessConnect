import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface JitsiTokenResponse {
  token: string;
  roomName: string;
  domain: string;
  isModerator: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class JitsiService {
  private readonly apiUrl = '/api/jitsi';

  constructor(private http: HttpClient) {}

  getToken(roomName: string): Observable<JitsiTokenResponse> {
    return this.http.get<JitsiTokenResponse>(`${this.apiUrl}/token`, {
      params: { roomName }
    });
  }

  buildJitsiUrl(token: string, roomName: string): string {
    const baseUrl = `https://meet.mychess.fr/${roomName}`;
    const config = [
      'config.prejoinPageEnabled=false',
      'config.startWithAudioMuted=false',
      'config.startWithVideoMuted=false'
    ].join('&');

    return `${baseUrl}?jwt=${token}#${config}`;
  }
}
