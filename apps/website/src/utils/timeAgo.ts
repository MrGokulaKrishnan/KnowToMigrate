import { useState, useEffect } from 'react';

/**
 * Returns the exact timestamp of the latest build / project modification.
 * When built with Vite, __PROJECT_LAST_MODIFIED__ is injected at bundle time.
 */
export function getProjectLastModified(fallback?: string): string {
  try {
    if (typeof __PROJECT_LAST_MODIFIED__ !== 'undefined' && __PROJECT_LAST_MODIFIED__) {
      return __PROJECT_LAST_MODIFIED__;
    }
  } catch {
    // fallback if variable is undefined in dev environment
  }
  return fallback || new Date().toISOString();
}

/**
 * Returns an accurate human-friendly relative time string that matches product requirements:
 * "Recently Updated just now", "Recently Updated 1min ago", "Recently Updated 2mins ago", etc.
 * Never outputs a static frozen time string.
 */
export function formatRelativeTime(dateInput?: string | number | Date, referenceNow = new Date()): string {
  if (!dateInput) return 'Recently Updated just now';

  let date: Date;
  if (typeof dateInput === 'number') {
    date = new Date(dateInput);
  } else if (typeof dateInput === 'string') {
    date = new Date(dateInput);
  } else {
    date = dateInput;
  }

  if (isNaN(date.getTime())) {
    return 'Recently Updated just now';
  }

  const diffMs = referenceNow.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);

  // If clock skew or within the last 45 seconds
  if (diffSec < 45) {
    return 'Recently Updated just now';
  }

  const diffMin = Math.round(diffSec / 60);
  if (diffMin <= 1) {
    return 'Recently Updated 1min ago';
  }

  if (diffMin < 60) {
    return `Recently Updated ${diffMin}mins ago`;
  }

  const diffHours = Math.floor(diffMin / 60);
  if (diffHours === 1) {
    return 'Recently Updated 1h ago';
  }
  if (diffHours < 24) {
    return `Recently Updated ${diffHours}h ago`;
  }

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) {
    return 'Recently Updated 1d ago';
  }
  if (diffDays < 7) {
    return `Recently Updated ${diffDays}d ago`;
  }

  return `Updated on ${date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}`;
}

/**
 * React Hook that actively refreshes relative time at a fixed interval (default 10s),
 * ensuring the UI dynamically updates on screen as time passes.
 */
export function useLiveTimeAgo(dateInput?: string | number | Date, intervalMs = 10000): string {
  const [timeAgo, setTimeAgo] = useState<string>(() => formatRelativeTime(dateInput));

  useEffect(() => {
    // Immediate calculation on input change
    setTimeAgo(formatRelativeTime(dateInput));

    const timer = setInterval(() => {
      setTimeAgo(formatRelativeTime(dateInput));
    }, intervalMs);

    return () => clearInterval(timer);
  }, [dateInput, intervalMs]);

  return timeAgo;
}
