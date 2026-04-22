# 🧠 Activities Service - Alzheimer Cognitive Activities

## ✅ ÉTAT ACTUEL : SERVEUR OPÉRATIONNEL

### 🚀 **Serveur Backend**
- **URL** : http://localhost:8085 ✅
- **Health** : http://localhost:8085/health ✅
- **Swagger UI** : http://localhost:8085/swagger-ui/index.html ✅

### 📋 **Endpoints CRUD Disponibles**

#### **🎮 Quiz Activities**
```
GET    /api/quiz-activities           - Lister tous les quiz
POST   /api/quiz-activities           - Créer un quiz
GET    /api/quiz-activities/{id}      - Récupérer un quiz
PUT    /api/quiz-activities/{id}      - Mettre à jour un quiz
DELETE /api/quiz-activities/{id}      - Supprimer un quiz
GET    /api/quiz-activities/theme/{theme} - Quiz par thème
GET    /api/quiz-activities/difficulty/{difficulty} - Quiz par difficulté
```

#### **📸 Photo Activities**
```
GET    /api/photo-activities         - Lister toutes les photos
POST   /api/photo-activities         - Créer une activité photo
GET    /api/photo-activities/{id}    - Récupérer une photo
PUT    /api/photo-activities/{id}    - Mettre à jour une photo
DELETE /api/photo-activities/{id}    - Supprimer une photo
```

#### **📊 Game Results**
```
GET    /api/game-results             - Lister tous les résultats
POST   /api/game-results             - Enregistrer un résultat
GET    /api/game-results/patient/{id} - Résultats par patient
GET    /api/game-results/patient/{id}/activity/{type}/stats - Statistiques
```

### 🎯 **Exemple de Création de Quiz**
```bash
curl -X POST http://localhost:8085/api/quiz-activities \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Mémoire Alzheimer",
    "theme": "MEMORY",
    "description": "Test de mémoire à court terme",
    "difficulty": "EASY"
  }'
```

### 🗄️ **Base de Données**
- **Actuel** : H2 en mémoire (pour tests)
- **MySQL Script** : `setup_database.sql` prêt pour migration
- **Tables** : quiz_activity, photo_activity, game_result

### 🎨 **Frontend - Sidebar Activités**
À ajouter dans la sidebar NexLink :

```html
<!-- Menu Activités -->
<div class="sidebar-menu">
  <div class="menu-item activities">
    <i class="icon-activities"></i>
    <span>Activités</span>
    <div class="submenu">
      <a href="/activities/quiz">Quiz</a>
      <a href="/activities/qa">Q&A</a>
      <a href="/activities/images">Images</a>
    </div>
  </div>
</div>
```

### 🚀 **Prochaines Étapes**
1. **Frontend** : Ajouter le menu "Activités" dans la sidebar
2. **Interface CRUD** : Formulaire de création/édition de quiz
3. **Moteur de test** : Interface patient pour répondre aux quiz
4. **MySQL Migration** : Passer à MySQL quand prêt

---

**Le backend est 100% fonctionnel et prêt pour l'intégration frontend !** 🎉
