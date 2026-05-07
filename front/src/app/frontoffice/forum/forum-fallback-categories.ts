import { Category } from '../../core/services/forum.service';

/** MindCare forum themes — aligned with backend seed when offline (Alzheimer’s / dementia focus). */
export const FORUM_FALLBACK_CATEGORIES: Category[] = [
  {
    id: 1,
    name: 'Early Signs & Symptoms',
    description:
      'Alzheimer’s disease and dementia: early warning signs, memory changes, and when to seek medical evaluation.',
    icon: 'ri-heart-pulse-line',
    color: '#0891b2',
  },
  {
    id: 2,
    name: 'Caregiver Support',
    description:
      'Support for family and professional caregivers: daily challenges, coping, respite, and emotional well-being in Alzheimer’s care.',
    icon: 'ri-hand-heart-line',
    color: '#f43f5e',
  },
  {
    id: 3,
    name: 'Treatment & Research',
    description:
      'Approved treatments, clinical trials, biomarkers, and research news in Alzheimer’s and related dementias.',
    icon: 'ri-microscope-line',
    color: '#6366f1',
  },
  {
    id: 4,
    name: 'Daily Living Tips',
    description:
      'Practical routines, home safety, nutrition, sleep, and quality of life while living with Alzheimer’s disease.',
    icon: 'ri-home-heart-line',
    color: '#059669',
  },
  {
    id: 5,
    name: 'Legal & Financial',
    description:
      'Legal capacity, advance directives, insurance, care costs, and navigating health and social systems.',
    icon: 'ri-scales-3-line',
    color: '#d97706',
  },
  {
    id: 6,
    name: 'Memory Cafe',
    description:
      'Community activities, memory cafés, hobbies, and staying socially connected with dementia.',
    icon: 'ri-cup-line',
    color: '#db2777',
  },
];
