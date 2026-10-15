// Development-only tools must stay absent even if a deployment mistakenly
// retains VITE_APP_ENV=development in its environment.
export const isDevelopment = import.meta.env.DEV
