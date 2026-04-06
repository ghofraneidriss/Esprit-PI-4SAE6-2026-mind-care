import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class VolunteerService {
    private readonly apiUrl = 'http://localhost:8080/api/volunteers';

    constructor(private readonly http: HttpClient) { }

    // TODO: Add volunteer CRUD and availability endpoints
}
