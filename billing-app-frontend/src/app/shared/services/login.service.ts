import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LoginService {
  constructor(private http: HttpClient) {}

  checkUser(username: string, password: string): Observable<any> {
    return this.http.post('/api/auth/login', {
      userName: username,
      password: password
    });
  }
}
