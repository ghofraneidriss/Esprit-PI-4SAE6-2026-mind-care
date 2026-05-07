import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { activitiesApiBase } from '../../../environments/environment';

export interface PhotoActivity {
  id?: number;
  title: string;
  description?: string;
  /** Chemin API `/api/photo-activities/{id}/image` lorsque l’image est stockée en base (LONGBLOB). */
  imageUrl: string;
  type?: string;
  difficulty: string;
  points?: number;
  status?: string;
  createdAt?: string;
  correctAnswer?: string;
  options?: string[];
}

@Injectable({ providedIn: 'root' })
export class PhotoService {
  private readonly apiUrl = `${activitiesApiBase()}/photo-activities`;

  constructor(private http: HttpClient) {}

  getPhotos(): Observable<PhotoActivity[]> {
    return this.http.get<PhotoActivity[]>(this.apiUrl);
  }

  getPhotoById(id: number): Observable<PhotoActivity> {
    return this.http.get<PhotoActivity>(`${this.apiUrl}/${id}`);
  }

  /** Création avec fichier (JPEG redimensionné côté serveur, stockage LONGBLOB). */
  createWithImage(formData: FormData): Observable<PhotoActivity> {
    return this.http.post<PhotoActivity>(`${this.apiUrl}/with-image`, formData);
  }

  /** Mise à jour avec fichier optionnel. */
  updateWithImage(id: number, formData: FormData): Observable<PhotoActivity> {
    return this.http.put<PhotoActivity>(`${this.apiUrl}/${id}/with-image`, formData);
  }

  createPhoto(photo: PhotoActivity): Observable<unknown> {
    return this.http.post(this.apiUrl, photo);
  }

  updatePhoto(id: number, photo: PhotoActivity): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/${id}`, photo);
  }

  deletePhoto(id: number): Observable<unknown> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  getPhotosByDifficulty(difficulty: string): Observable<PhotoActivity[]> {
    return this.http.get<PhotoActivity[]>(`${this.apiUrl}/difficulty/${difficulty}`);
  }

  searchPhotos(title: string): Observable<PhotoActivity[]> {
    return this.http.get<PhotoActivity[]>(
      `${this.apiUrl}/search?title=${encodeURIComponent(title)}`
    );
  }
}
