import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Category {
  id: number;
  name: string;
  description?: string;
}

export interface Post {
  id: number;
  title: string;
  content: string;
  userId: number;
  author: string;
  categoryId: number;
  categoryName?: string;
  status?: 'PUBLISHED' | 'DRAFT' | string;
  commentCount?: number;
  createdAt: string;
}

export interface Comment {
  id: number;
  postId?: number;
  userId: number;
  content: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ForumService {
  private readonly baseApi = 'http://localhost:8085/api';

  constructor(private readonly http: HttpClient) {}

  getAllPosts(): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.baseApi}/posts`);
  }

  createPost(payload: { title: string; content: string; userId: number }, categoryId: number): Observable<Post> {
    return this.http.post<Post>(`${this.baseApi}/posts`, { ...payload, categoryId });
  }

  updatePost(postId: number, payload: { title: string; content: string; userId: number }): Observable<Post> {
    return this.http.put<Post>(`${this.baseApi}/posts/${postId}`, payload);
  }

  deletePost(postId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseApi}/posts/${postId}`);
  }

  getAllCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.baseApi}/categories`);
  }

  createCategory(payload: { name: string; description?: string }): Observable<Category> {
    return this.http.post<Category>(`${this.baseApi}/categories`, payload);
  }

  updateCategory(categoryId: number, payload: { name: string; description?: string }): Observable<Category> {
    return this.http.put<Category>(`${this.baseApi}/categories/${categoryId}`, payload);
  }

  deleteCategory(categoryId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseApi}/categories/${categoryId}`);
  }

  getAllComments(): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.baseApi}/comments`);
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseApi}/comments/${commentId}`);
  }
}
