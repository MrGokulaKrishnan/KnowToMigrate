/**
 * KnowToMigrate - Smart Device Migration Models
 */

export type MigrationCategoryType = 
  | 'photos'
  | 'videos'
  | 'documents'
  | 'downloads'
  | 'music'
  | 'contacts'
  | 'calendar'
  | 'custom_folders';

export interface MigrationCategorySummary {
  type: MigrationCategoryType;
  displayName: string;
  description: string;
  itemCount: number;
  totalSizeBytes: number;
  selected: boolean;
  sourceDirectories: string[];
  supportedOnTarget: boolean;
}

export interface PreflightStorageCheck {
  sourceTotalBytes: number;
  destinationFreeBytes: number;
  destinationTotalBytes: number;
  hasSufficientSpace: boolean;
  safetyMarginBytes: number; // 2GB minimum reserve
  estimatedDurationSeconds: number;
  recommendedTransport: string;
  batteryLevelPercent?: number;
  isPluggedIn?: boolean;
}

export interface MigrationPlan {
  id: string;
  sourceDeviceId: string;
  targetDeviceId: string;
  categories: MigrationCategorySummary[];
  totalFiles: number;
  totalBytes: number;
  storageCheck: PreflightStorageCheck;
  step: number; // 1 to 10
  status: 'draft' | 'analyzing' | 'ready' | 'in_progress' | 'completed' | 'failed';
  startedAt?: number;
  completedAt?: number;
}

export interface MigrationAuditReport {
  migrationId: string;
  sourceDevice: string;
  targetDevice: string;
  categoriesMigrated: {
    type: MigrationCategoryType;
    filesTransferred: number;
    bytesTransferred: number;
  }[];
  totalFilesTransferred: number;
  totalBytesTransferred: number;
  duplicatesFound: number;
  duplicatesAction: 'renamed' | 'overwritten' | 'skipped';
  integrityVerified: boolean;
  failedItems: { path: string; error: string }[];
  durationSeconds: number;
  averageSpeedMbps: number;
  completedAt: number;
}
