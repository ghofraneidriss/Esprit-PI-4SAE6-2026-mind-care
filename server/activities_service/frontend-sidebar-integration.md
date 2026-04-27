# 🎨 Frontend Integration - Sidebar Activités

## 📋 **Instructions pour ajouter le menu "Activités"**

### 1. **Localisation du fichier sidebar**
Cherchez le fichier de sidebar NexLink (probablement dans le frontend Angular) :
```
src/app/components/sidebar/sidebar.component.html
ou
src/app/shared/sidebar/sidebar.component.html
```

### 2. **Code à ajouter dans la sidebar**

```html
<!-- Menu Activités - Section Alzheimer -->
<div class="sidebar-section">
  <div class="section-title">Activités Cognitives</div>
  
  <div class="menu-item" routerLink="/activities" routerLinkActive="active">
    <div class="menu-header">
      <i class="fas fa-brain"></i>
      <span>Activités</span>
      <i class="fas fa-chevron-down submenu-toggle"></i>
    </div>
    
    <!-- Sous-menu Activités -->
    <div class="submenu">
      <a class="submenu-item" routerLink="/activities/quiz" routerLinkActive="active">
        <i class="fas fa-question-circle"></i>
        <span>Quiz</span>
      </a>
      
      <a class="submenu-item" routerLink="/activities/qa" routerLinkActive="active">
        <i class="fas fa-comments"></i>
        <span>Q&A</span>
      </a>
      
      <a class="submenu-item" routerLink="/activities/images" routerLinkActive="active">
        <i class="fas fa-images"></i>
        <span>Images</span>
      </a>
    </div>
  </div>
</div>
```

### 3. **Styles CSS à ajouter**

```css
/* Styles pour le menu Activités */
.sidebar-section {
  margin-bottom: 1rem;
}

.section-title {
  padding: 0.5rem 1rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.menu-item {
  position: relative;
}

.menu-header {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  cursor: pointer;
  border-radius: 0.5rem;
  transition: all 0.2s;
}

.menu-header:hover {
  background-color: #f3f4f6;
}

.menu-header i {
  margin-right: 0.75rem;
  color: #6b7280;
}

.submenu-toggle {
  margin-left: auto;
  transition: transform 0.2s;
}

.menu-item.expanded .submenu-toggle {
  transform: rotate(180deg);
}

.submenu {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.menu-item.expanded .submenu {
  max-height: 200px;
}

.submenu-item {
  display: flex;
  align-items: center;
  padding: 0.5rem 1rem 0.5rem 3rem;
  color: #6b7280;
  text-decoration: none;
  transition: all 0.2s;
}

.submenu-item:hover {
  background-color: #f9fafb;
  color: #374151;
}

.submenu-item.active {
  background-color: #dbeafe;
  color: #2563eb;
}

.submenu-item i {
  margin-right: 0.75rem;
  font-size: 0.875rem;
}
```

### 4. **Component TypeScript (si nécessaire)**

```typescript
// sidebar.component.ts
import { Component } from '@angular/core';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  isActivitiesExpanded = false;

  toggleActivities() {
    this.isActivitiesExpanded = !this.isActivitiesExpanded;
  }
}
```

### 5. **Création des routes**

Dans `app-routing.module.ts` :
```typescript
const routes: Routes = [
  // ... routes existantes
  
  {
    path: 'activities',
    children: [
      { path: 'quiz', component: QuizListComponent },
      { path: 'qa', component: QAListComponent },
      { path: 'images', component: ImagesListComponent },
      { path: 'quiz/create', component: QuizCreateComponent },
      { path: 'quiz/:id', component: QuizDetailComponent },
      { path: 'quiz/:id/edit', component: QuizEditComponent }
    ]
  }
];
```

## 🎯 **Pages à créer**

### 1. **Quiz List Component** (`/activities/quiz`)
- Liste des quiz disponibles
- Bouton "Créer un quiz"
- Filtres par thème/difficulté

### 2. **Quiz Create Component** (`/activities/quiz/create`)
- Formulaire de création de quiz
- Champs : title, theme, description, difficulty
- Appel API : POST /api/quiz-activities

### 3. **Quiz Detail Component** (`/activities/quiz/:id`)
- Détails du quiz
- Boutons : Modifier, Supprimer
- Lancer le quiz pour les patients

## 🚀 **Integration API**

### Service Angular pour les Quiz :
```typescript
@Injectable({
  providedIn: 'root'
})
export class QuizService {
  private baseUrl = 'http://localhost:8085/api/quiz-activities';

  constructor(private http: HttpClient) {}

  getAllQuizzes(): Observable<QuizActivity[]> {
    return this.http.get<QuizActivity[]>(this.baseUrl);
  }

  createQuiz(quiz: QuizActivity): Observable<QuizActivity> {
    return this.http.post<QuizActivity>(this.baseUrl, quiz);
  }

  updateQuiz(id: number, quiz: QuizActivity): Observable<QuizActivity> {
    return this.http.put<QuizActivity>(`${this.baseUrl}/${id}`, quiz);
  }

  deleteQuiz(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

---

**Le backend est prêt, il ne reste plus qu'à intégrer ce code dans votre frontend Angular !** 🎉
