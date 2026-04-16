import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { ForumLayout } from './forum-layout/forum-layout';
import { ForumHomeComponent } from './forum-home/forum-home.component';
import { ForumCategory } from './forum-category/forum-category';
import { CategoryListComponent } from './category-list/category-list.component';
import { PostsByCategoryComponent } from './posts-by-category/posts-by-category.component';
import { PostDetailsComponent } from './post-details/post-details.component';
import { ForumCvpInsightsComponent } from './forum-cvp-insights/forum-cvp-insights.component';
import { SharedModule } from '../../shared/shared.module';

const routes: Routes = [
    {
        path: '',
        component: ForumLayout,
        children: [
            { path: '', component: ForumHomeComponent, title: 'Forum Home' },
            { path: 'statistiques', component: ForumCvpInsightsComponent, title: 'Forum — statistics' },
            { path: 'categories', component: CategoryListComponent, title: 'Forum Categories' },
            { path: 'category/:id', component: PostsByCategoryComponent, title: 'Posts by Category' },
            { path: 'post/:id', component: PostDetailsComponent, title: 'Post Details' }
        ]
    }
];

@NgModule({
    declarations: [
        ForumLayout,
        ForumHomeComponent,
        ForumCategory,
        CategoryListComponent,
        PostsByCategoryComponent,
        PostDetailsComponent,
        ForumCvpInsightsComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule.forChild(routes),
        SharedModule
    ]
})
export class ForumModule { }
