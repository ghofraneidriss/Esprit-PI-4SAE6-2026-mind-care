/**
 * Dev proxy for `ng serve` (see server/ports.env.example).
 * Order matters: more specific paths first.
 *
 * - /api/users → users-service :8081
 * - /api/posts|categories|comments → forums-service :8082 (bypass gateway if :8080 is down)
 * - activities-service :8084 (quiz, performance, rapports PDF, …) — évite 503 Eureka sur la gateway
 * - /api → API gateway :8080 (incidents, localization, …)
 */
module.exports = [
  {
    context: ['/api/users'],
    target: 'http://localhost:8081',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  {
    context: ['/api/posts'],
    target: 'http://localhost:8082',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  {
    context: ['/api/categories'],
    target: 'http://localhost:8082',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  {
    context: ['/api/comments'],
    target: 'http://localhost:8082',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  {
    context: ['/api/forum'],
    target: 'http://localhost:8082',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  {
    context: [
      '/api/performance',
      '/api/reports',
      '/api/quiz',
      '/api/quizzes',
      '/api/photo-activities',
      '/api/game-results',
      '/api/quiz-limits',
      '/api/simple',
      '/api/test',
    ],
    target: 'http://localhost:8084',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  {
    context: ['/api/recommendations', '/api/events', '/api/puzzles', '/api/sudoku'],
    target: 'http://localhost:8080',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  {
    context: ['/api/souvenirs', '/api/reminiscence-activities'],
    target: 'http://localhost:8080',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  {
    context: ['/api'],
    target: 'http://localhost:8080',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
];
