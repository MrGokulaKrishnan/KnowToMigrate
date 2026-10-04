import { useState, useEffect } from 'react'

export type PlatformType = 'windows' | 'android' | 'macos' | 'linux' | 'ios' | 'unknown'

export interface PlatformDetails {
  type: PlatformType
  name: string
  fullLabel: string
  isSupported: boolean
  recommendedFilename?: string
  recommendedLabel?: string
  architecture?: string
}

export function detectPlatform(): PlatformDetails {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return {
      type: 'windows',
      name: 'Windows',
      fullLabel: 'Windows 10/11 (64-bit)',
      isSupported: true,
      recommendedFilename: 'KnowToMigrate-Setup.exe',
      recommendedLabel: 'Download for Windows',
      architecture: '64-bit',
    }
  }

  const nav = navigator as Navigator & {
    userAgentData?: {
      platform?: string
      architecture?: string
      bitness?: string
    }
  }

  const ua = nav.userAgent || ''
  const platform = nav.userAgentData?.platform || nav.platform || ''

  // 1. Android
  if (/android/i.test(ua) || /android/i.test(platform)) {
    return {
      type: 'android',
      name: 'Android',
      fullLabel: 'Android (8.0+)',
      isSupported: true,
      recommendedFilename: 'KnowToMigrate-1.0.0.apk',
      recommendedLabel: 'Download for Android (.apk)',
    }
  }

  // 2. iOS (iPhone, iPad, iPod, or iPad on iOS 13+ reporting MacIntel with multitouch)
  const isIOS =
    /iphone|ipad|ipod/i.test(ua) ||
    (/mac/i.test(platform) && typeof nav.maxTouchPoints === 'number' && nav.maxTouchPoints > 1)

  if (isIOS) {
    return {
      type: 'ios',
      name: 'iOS',
      fullLabel: 'Apple iOS / iPadOS',
      isSupported: false,
    }
  }

  // 3. Windows
  if (/win/i.test(platform) || /windows/i.test(ua)) {
    const is64 =
      /x64|wow64|win64|x86_64/i.test(ua) ||
      nav.userAgentData?.bitness === '64' ||
      true // Default modern Windows installations to 64-bit

    return {
      type: 'windows',
      name: 'Windows',
      fullLabel: is64 ? 'Windows 10/11 (64-bit)' : 'Windows',
      isSupported: true,
      recommendedFilename: 'KnowToMigrate-Setup.exe',
      recommendedLabel: 'Download for Windows (Setup)',
      architecture: is64 ? '64-bit' : '32-bit',
    }
  }

  // 4. macOS
  if (/mac/i.test(platform) || /macintosh|mac os x/i.test(ua)) {
    return {
      type: 'macos',
      name: 'macOS',
      fullLabel: 'macOS (Apple Silicon & Intel)',
      isSupported: false,
    }
  }

  // 5. Linux
  if (/linux/i.test(platform) || /linux/i.test(ua)) {
    return {
      type: 'linux',
      name: 'Linux',
      fullLabel: 'Linux',
      isSupported: false,
    }
  }

  return {
    type: 'unknown',
    name: 'Your Device',
    fullLabel: 'Desktop or Mobile Device',
    isSupported: false,
  }
}

export function usePlatformDetection(): PlatformDetails {
  const [platform, setPlatform] = useState<PlatformDetails>(() => detectPlatform())

  useEffect(() => {
    setPlatform(detectPlatform())
  }, [])

  return platform
}
