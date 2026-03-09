import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { FileRecord } from './file.model';

@Injectable({ providedIn: 'root' })
export class FileService {
  private readonly apiUrl = 'http://localhost:8083/api/files';

  constructor(private readonly http: HttpClient) { }

  getAll(): Observable<FileRecord[]> {
    return this.http.get<FileRecord[]>(this.apiUrl);
  }

  getById(id: number): Observable<FileRecord> {
    return this.http.get<FileRecord>(`${this.apiUrl}/${id}`);
  }

  create(payload: Partial<FileRecord>): Observable<FileRecord> {
    return this.http.post<FileRecord>(this.apiUrl, payload);
  }

  update(payload: Partial<FileRecord>): Observable<FileRecord> {
    return this.http.put<FileRecord>(this.apiUrl, payload);
  }

  delete(id: number): Observable<boolean> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(map(() => true));
  }
}
