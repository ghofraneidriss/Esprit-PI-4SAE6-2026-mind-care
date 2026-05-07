import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ForumService, Post, Category } from '../../../core/services/forum.service';

@Component({
  selector: 'app-forum-category',
  templateUrl: './forum-category.html',
  styleUrls: ['./forum-category.css'],
  standalone: false
})
export class ForumCategory implements OnInit {
  categoryId: number = 0;
  categoryName: string = 'Category';
  description: string = 'Category description...';
  posts: Post[] = [];

  // Mock logic for filtering
  currentFilter = 'Latest';

  constructor(
    private route: ActivatedRoute,
    private forumService: ForumService
  ) { }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.categoryId = +id;
        this.loadCategoryDetails(this.categoryId);
        this.loadPosts(this.categoryId);
      }
    });
  }

  loadCategoryDetails(id: number): void {
    console.log('[Forum Category] Loading category details for ID:', id);
    this.forumService.getAllCategories().subscribe({
      next: (categories) => {
        const category = categories.find(c => c.id === id);
        if (category) {
          console.log('[Forum Category] Category found:', category);
          this.categoryName = category.name;
          this.description = category.description || 'Category description';
        } else {
          console.log('[Forum Category] Category not found, using mock data');
          this.useMockCategoryData(id);
        }
      },
      error: (err) => {
        console.error('[Forum Category] Error loading category:', err);
        this.useMockCategoryData(id);
      }
    });
  }

  private useMockCategoryData(id: number): void {
    const categories: { [key: number]: any } = {
      1: { name: 'Early Signs & Symptoms', desc: 'Discuss early warning signs, memory changes, and when to seek medical advice.' },
      2: { name: 'Caregiver Support', desc: 'A safe space for caregivers to share challenges, coping strategies, and find emotional support.' },
      3: { name: 'Treatment & Research', desc: 'Latest research findings, treatment options, clinical trials, and medical breakthroughs.' },
      4: { name: 'Daily Living', desc: 'Tips for managing daily routines, safety, communication, and quality of life.' },
      5: { name: 'Legal & Financial', desc: 'Navigate power of attorney, long-term care planning, and financial resources.' },
      6: { name: 'Memory Cafe', desc: 'Social space for casual conversation, shared interests, and community connection.' }
    };

    const cat = categories[id];
    if (cat) {
      this.categoryName = cat.name;
      this.description = cat.desc;
    }
  }

  loadPosts(categoryId: number): void {
    console.log('[Forum Category] Loading posts for category:', categoryId);
    this.forumService.getPostsByCategory(categoryId).subscribe({
      next: (data) => {
        console.log('[Forum Category] Posts received:', data);
        this.posts = data;
        if (this.posts.length === 0) {
          console.log('[Forum Category] No posts found, using mock data');
          this.addMockPosts();
        }
      },
      error: (err) => {
        console.error('[Forum Category] Error loading posts:', err);
        this.addMockPosts();
      }
    });
  }

  addMockPosts() {
    this.posts = [
      {
        id: 101, title: 'When should I be concerned about memory lapses?',
        content: 'My mother is 68 and has been forgetting small things lately - misplacing keys, forgetting appointments...',
        createdAt: '2024-01-15T10:00:00', userId: 1, categoryId: 1, categoryName: 'Early Signs', author: 'User 1', commentCount: 0
      },
      {
        id: 102, title: 'Difference between dementia and Alzheimer\'s?',
        content: 'Can someone explain the difference between dementia and Alzheimer\'s disease? My doctor mentioned both...',
        createdAt: '2024-01-14T15:30:00', userId: 2, categoryId: 1, categoryName: 'Early Signs', author: 'User 2', commentCount: 0
      }
    ];
  }
}
