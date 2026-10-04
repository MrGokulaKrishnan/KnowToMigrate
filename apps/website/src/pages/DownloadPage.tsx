import { useState, useEffect } from 'react'
import {
  Download,
  Shield,
  CheckCircle,
  Monitor,
  Smartphone,
  Copy,
  Check,
  Eye,
  EyeOff,
  Sparkles,
  Laptop,
  ArrowRight,
  Zap,
  Globe
} from 'lucide-react'
import { usePlatformDetection, PlatformType } from '../utils/platform'

export interface DownloadEntry {
  id: string
  filename: string
  version: string
  size: string
  sha256: string
  requirements: string
  downloadUrl: string
  recommended?: boolean
  platform: 'windows' | 'android'
}

export const RELEASE_DATE = '04 October 2026'

const DEFAULT_WINDOWS_DOWNLOADS: DownloadEntry[] = [
  {
    id: 'win-setup',
    filename: 'KnowToMigrate-Setup.exe',
    version: '1.0.5',
    size: '63.4 MB',
    sha256: '0af66ecff7a114d2b66bd17b1814ec1f612c54dd0ab78199e7060efcb315e135',
    requirements: 'Windows 10 / 11, 64-bit (Official Setup Installer with Start Menu & Desktop Shortcut)',
    downloadUrl: '/download/KnowToMigrate-Setup.exe.bin',
    recommended: true,
    platform: 'windows',
  },
  {
    id: 'win-msi',
    filename: 'KnowToMigrate-1.0.5-x64.msi',
    version: '1.0.5',
    size: '63.4 MB',
    sha256: '88a452e85e7e4cb8b194b9b3dd7feb9e17326af076417315c3dccf26817a2e25',
    requirements: 'Windows 10 / 11, 64-bit (Standard Enterprise Windows Installer Package)',
    downloadUrl: '/download/KnowToMigrate-1.0.5-x64.msi.bin',
    platform: 'windows',
  },
  {
    id: 'win-portable',
    filename: 'KnowToMigrate.exe',
    version: '1.0.5',
    size: '69.1 MB',
    sha256: '9ecea10a8d2c5807dfaae0684f090b657de55c73d6c5708c980c1184a67454cd',
    requirements: 'Windows 10 / 11, 64-bit (Portable Compressed Standalone Executable)',
    downloadUrl: '/download/KnowToMigrate.exe.bin',
    platform: 'windows',
  },
]

const DEFAULT_ANDROID_DOWNLOADS: DownloadEntry[] = [
  {
    id: 'android-apk',
    filename: 'KnowToMigrate-1.0.4.apk',
    version: '1.0.5',
    size: '16.8 MB',
    sha256: '7789deaa4baa5ee1d2aad8f0a50a7a823beef1d0a0a6707885af0bb7a60cc48a',
    requirements: 'Android 8.0+ (API 26+) — Independent APK with built-in self-updater',
    downloadUrl: '/download/KnowToMigrate-1.0.4.apk.bin',
    recommended: true,
    platform: 'android',
  },
]

function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B'
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${sizes[i]}`
}

function HiddenHash({ hash }: { hash: string }) {
  const [showHash, setShowHash] = useState(false)
  const [copied, setCopied] = useState(false)

  const handleCopy = () => {
    navigator.clipboard.writeText(hash)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const masked = '••••••••••••••••••••••••••••••••'

  return (
    <div className="mt-3 p-2.5 rounded-xl bg-black/40 border border-white/[0.06]">
      <div className="flex items-center justify-between text-[11px] text-[#777] mb-1.5 font-medium">
        <span className="flex items-center gap-1.5">
          <Shield className="w-3 h-3 text-[#FF5A00]" />
          SHA-256 Checksum
        </span>
        <button
          type="button"
          onClick={() => setShowHash(!showHash)}
          className="flex items-center gap-1 text-[#FF8A00] hover:text-[#FFA033] transition-colors cursor-pointer"
        >
          {showHash ? (
            <>
              <EyeOff className="w-3.5 h-3.5" />
              <span>Hide</span>
            </>
          ) : (
            <>
              <Eye className="w-3.5 h-3.5" />
              <span>Unhide</span>
            </>
          )}
        </button>
      </div>

      <div className="flex items-center gap-2">
        <code className="text-xs text-[#AAA] font-mono bg-white/[0.03] px-2 py-1 rounded flex-1 truncate select-all">
          {showHash ? hash : masked}
        </code>
        {showHash && (
          <button
            type="button"
            onClick={handleCopy}
            title="Copy SHA-256 hash"
            className="p-1 rounded hover:bg-white/10 transition-colors text-[#888] hover:text-white cursor-pointer"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-[#22C55E]" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
        )}
      </div>
    </div>
  )
}

type DownloadStatus = 'idle' | 'connecting' | 'preparing' | 'ready' | 'downloading' | 'complete'

function DownloadCard({
  entry,
  icon: Icon,
  isRecommendedDevice = false,
}: {
  entry: DownloadEntry
  icon: React.ElementType
  isRecommendedDevice?: boolean
}) {
  const [status, setStatus] = useState<DownloadStatus>('idle')
  const [progress, setProgress] = useState<number | null>(null)

  const handleDownload = async () => {
    if (status !== 'idle' && status !== 'complete') return
    setStatus('connecting')
    setProgress(0)

    const downloadUrl = entry.downloadUrl

    try {
      await new Promise(r => setTimeout(r, 200))
      setStatus('preparing')

      const response = await fetch(downloadUrl)
      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      await new Promise(r => setTimeout(r, 150))
      setStatus('downloading')

      const contentLength = response.headers.get('content-length')
      const total = contentLength ? parseInt(contentLength, 10) : 0
      let loaded = 0

      const reader = response.body?.getReader()
      if (!reader) throw new Error('No body reader available')

      const chunks: BlobPart[] = []
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        if (value) {
          chunks.push(value)
          loaded += value.length
          if (total > 0) {
            setProgress(Math.round((loaded / total) * 100))
          }
        }
      }

      const mimeType = entry.filename.endsWith('.apk')
        ? 'application/vnd.android.package-archive'
        : entry.filename.endsWith('.msi')
        ? 'application/x-msi'
        : 'application/octet-stream'

      const blob = new Blob(chunks, { type: mimeType })
      const blobUrl = window.URL.createObjectURL(blob)

      const a = document.createElement('a')
      a.href = blobUrl
      a.download = entry.filename
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 10000)

      setStatus('complete')
      setProgress(100)
      setTimeout(() => {
        setStatus('idle')
        setProgress(null)
      }, 3500)
    } catch (_err) {
      // Direct fallback
      const a = document.createElement('a')
      a.href = entry.downloadUrl
      a.download = entry.filename
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      setStatus('complete')
      setTimeout(() => {
        setStatus('idle')
        setProgress(null)
      }, 3500)
    }
  }

  const isWindows = entry.filename.endsWith('.exe') || entry.filename.endsWith('.msi')
  const defaultButtonLabel = isWindows
    ? entry.filename.includes('Setup')
      ? 'Download for Windows (Setup)'
      : entry.filename.includes('msi')
      ? 'Download for Windows (MSI)'
      : 'Download for Windows (Portable)'
    : 'Download for Android (.apk)'

  return (
    <div
      className={`rounded-2xl border transition-all duration-300 p-5 ${
        isRecommendedDevice
          ? 'border-[#FF5A00]/50 bg-gradient-to-b from-[#FF5A00]/[0.07] to-white/[0.03] shadow-[0_0_30px_rgba(255,90,0,0.12)]'
          : 'border-white/[0.08] bg-white/[0.03] hover:border-[#FF5A00]/30 hover:bg-[#FF5A00]/[0.03]'
      }`}
    >
      <div className="flex items-start gap-3 mb-3">
        <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/15 border border-[#FF5A00]/25 flex items-center justify-center flex-shrink-0">
          <Icon className="w-5 h-5 text-[#FF5A00]" strokeWidth={1.75} />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <p className="text-sm font-semibold text-white truncate">{entry.filename}</p>
            {isRecommendedDevice && (
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-[#FF5A00]/20 text-[#FF8A00] border border-[#FF5A00]/40 flex items-center gap-1">
                <Sparkles className="w-2.5 h-2.5" /> Recommended
              </span>
            )}
          </div>
          <p className="text-xs text-[#666666] mt-0.5">{entry.requirements} · {entry.size}</p>
        </div>
        <span className="text-xs bg-[#FF5A00]/10 text-[#FF5A00] border border-[#FF5A00]/20 rounded-full px-2 py-0.5 flex-shrink-0 font-medium">
          v{entry.version}
        </span>
      </div>

      {/* Password-style Hidden SHA-256 */}
      <HiddenHash hash={entry.sha256} />

      {/* Direct In-Browser Download Button */}
      <button
        type="button"
        onClick={handleDownload}
        disabled={status !== 'idle' && status !== 'complete'}
        className={`mt-4 flex items-center justify-center gap-2 w-full min-h-[46px] px-4 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 cursor-pointer shadow-lg hover:brightness-110 active:scale-[0.98] disabled:opacity-90 leading-snug text-center ${
          isRecommendedDevice ? 'ring-2 ring-[#FF5A00]/30' : ''
        }`}
        style={{
          background:
            status === 'complete'
              ? 'linear-gradient(135deg, #16A34A, #22C55E)'
              : isRecommendedDevice
              ? 'linear-gradient(135deg, #FF4500, #FF5A00, #FF8A00)'
              : 'linear-gradient(135deg, #E64500, #FF5A00, #FF7A00)',
          color: '#fff',
        }}
      >
        {status === 'connecting' ? (
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            <span>Connecting...</span>
          </div>
        ) : status === 'preparing' ? (
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            <span>Preparing package...</span>
          </div>
        ) : status === 'downloading' ? (
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            <span>{progress !== null && progress > 0 ? `Downloading... ${progress}%` : 'Downloading...'}</span>
          </div>
        ) : status === 'complete' ? (
          <div className="flex items-center gap-2">
            <Check className="w-4 h-4 text-white" />
            <span>Download Complete!</span>
          </div>
        ) : (
          <>
            <Download className="w-4 h-4" />
            <span>{defaultButtonLabel}</span>
          </>
        )}
      </button>

      {/* Download Progress Bar */}
      {status === 'downloading' && progress !== null && (
        <div className="mt-2 w-full bg-white/[0.08] rounded-full h-1.5 overflow-hidden">
          <div
            className="bg-gradient-to-r from-[#FF4D00] to-[#FF8A00] h-full transition-all duration-150"
            style={{ width: `${progress}%` }}
          />
        </div>
      )}
    </div>
  )
}

export function DownloadPage() {
  const platform = usePlatformDetection()
  const [activeTab, setActiveTab] = useState<'all' | 'windows' | 'android'>('all')
  const [windowsDownloads, setWindowsDownloads] = useState<DownloadEntry[]>(DEFAULT_WINDOWS_DOWNLOADS)
  const [androidDownloads, setAndroidDownloads] = useState<DownloadEntry[]>(DEFAULT_ANDROID_DOWNLOADS)
  const [lastUpdated, setLastUpdated] = useState<string>(RELEASE_DATE)

  // Dynamically sync latest sizes, versions, and hashes from /update-manifest.json
  useEffect(() => {
    fetch('/update-manifest.json?_cb=' + Date.now())
      .then(res => res.json())
      .then(manifest => {
        if (manifest?.publishedAt) {
          try {
            const d = new Date(manifest.publishedAt)
            if (!isNaN(d.getTime())) {
              setLastUpdated(d.toLocaleDateString('en-GB', { day: '2-digit', month: 'long', year: 'numeric' }))
            }
          } catch {}
        }
        if (manifest?.windows) {
          const win = manifest.windows
          setWindowsDownloads([
            {
              id: 'win-setup',
              filename: win.filename || 'KnowToMigrate-Setup.exe',
              version: win.version || '1.0.2',
              size: win.size ? formatBytes(win.size) : '63.4 MB',
              sha256: win.sha256 || DEFAULT_WINDOWS_DOWNLOADS[0].sha256,
              requirements: 'Windows 10 / 11, 64-bit (Official Setup Installer with Start Menu & Desktop Shortcut)',
              downloadUrl: '/download/KnowToMigrate-Setup.exe.bin',
              recommended: true,
              platform: 'windows',
            },
            {
              id: 'win-msi',
              filename: win.msi?.filename || 'KnowToMigrate-1.0.5-x64.msi',
              version: win.version || '1.0.2',
              size: win.msi?.size ? formatBytes(win.msi.size) : '63.4 MB',
              sha256: win.msi?.sha256 || DEFAULT_WINDOWS_DOWNLOADS[1].sha256,
              requirements: 'Windows 10 / 11, 64-bit (Standard Enterprise Windows Installer Package)',
              downloadUrl: win.msi?.filename ? `/download/${win.msi.filename}.bin` : '/download/KnowToMigrate-1.0.5-x64.msi.bin',
              platform: 'windows',
            },
            {
              id: 'win-portable',
              filename: win.standalone?.filename || 'KnowToMigrate.exe',
              version: win.version || '1.0.2',
              size: win.standalone?.size ? formatBytes(win.standalone.size) : '69.1 MB',
              sha256: win.standalone?.sha256 || DEFAULT_WINDOWS_DOWNLOADS[2].sha256,
              requirements: 'Windows 10 / 11, 64-bit (Portable Compressed Standalone Executable)',
              downloadUrl: '/download/KnowToMigrate.exe.bin',
              platform: 'windows',
            },
          ])
        }

        if (manifest?.android) {
          const and = manifest.android
          setAndroidDownloads([
            {
              id: 'android-apk',
              filename: and.filename || 'KnowToMigrate-1.0.0.apk',
              version: and.versionName || '1.0.0',
              size: and.size ? formatBytes(and.size) : '11.0 MB',
              sha256: and.sha256 || DEFAULT_ANDROID_DOWNLOADS[0].sha256,
              requirements: 'Android 8.0+ (API 26+) — Enable "Install from unknown sources" in Settings',
              downloadUrl: '/download/KnowToMigrate-1.0.0.apk.bin',
              recommended: true,
              platform: 'android',
            },
          ])
        }
      })
      .catch(() => {
        // Safe fallback to defaults
      })
  }, [])

  // Auto-select recommended entry based on platform
  const suggestedEntry: DownloadEntry | null =
    platform.type === 'windows'
      ? windowsDownloads.find(d => d.recommended) || windowsDownloads[0]
      : platform.type === 'android'
      ? androidDownloads.find(d => d.recommended) || androidDownloads[0]
      : null

  const isWindowsDetected = platform.type === 'windows'
  const isAndroidDetected = platform.type === 'android'

  return (
    <main className="min-h-screen bg-black">
      {/* Hero with Logo & Platform Detection */}
      <section className="py-20 px-4 text-center relative overflow-hidden">
        {/* Glow effect */}
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background: 'radial-gradient(ellipse 65% 45% at 50% 0%, rgba(255,90,0,0.14) 0%, transparent 75%)',
          }}
        />

        <div className="relative max-w-3xl mx-auto">
          {/* KM Logo */}
          <div className="flex justify-center mb-6">
            <div className="relative">
              <div
                className="w-28 h-28 rounded-3xl overflow-hidden border-2 border-[#FF5A00]/40"
                style={{ boxShadow: '0 0 60px rgba(255,90,0,0.25), 0 0 120px rgba(255,90,0,0.08)' }}
              >
                <img
                  src="/logo.jpg"
                  alt="KnowToMigrate Logo"
                  className="w-full h-full object-cover"
                />
              </div>
              <div
                className="absolute inset-0 rounded-3xl pointer-events-none"
                style={{
                  background: 'linear-gradient(135deg, rgba(255,90,0,0.15), transparent)',
                  border: '1px solid rgba(255,90,0,0.2)',
                }}
              />
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-2.5 mb-5">
            <span className="inline-block px-4 py-1.5 rounded-full text-xs font-bold tracking-widest text-[#FF5A00] border border-[#FF5A00]/30 bg-[#FF5A00]/10">
              FREE &amp; OPEN SOURCE
            </span>
            <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold text-[#D4D4D4] border border-white/[0.08] bg-white/[0.03]">
              <span className="w-2 h-2 rounded-full bg-[#22C55E] animate-pulse" />
              Date Updated: {lastUpdated}
            </span>
          </div>

          <h1 className="text-4xl sm:text-5xl font-extrabold text-white mb-4 leading-tight">
            Download{' '}
            <span className="bg-gradient-to-r from-[#FF4D00] to-[#FF8A00] bg-clip-text text-transparent">
              KnowToMigrate
            </span>
          </h1>
          <p className="text-[#8A8A8A] text-base sm:text-lg max-w-xl mx-auto mb-8">
            High-speed, encrypted peer-to-peer file transfer. No cloud account required.
            Direct hardware-accelerated local transfers.
          </p>

          {/* ── FEATURED SUGGESTED PLATFORM CARD ── */}
          <div className="max-w-xl mx-auto mb-10 text-left">
            <div className="relative rounded-3xl p-6 sm:p-7 border border-[#FF5A00]/40 bg-gradient-to-b from-[#160A02] via-[#0E0601] to-[#0A0A0A] shadow-[0_0_50px_rgba(255,90,0,0.18)]">
              {/* Beacon Tag */}
              <div className="flex items-center justify-between gap-2 mb-4">
                <div className="flex items-center gap-2">
                  <span className="relative flex h-2.5 w-2.5">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#FF5A00] opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-[#FF5A00]"></span>
                  </span>
                  <span className="text-xs font-bold uppercase tracking-wider text-[#FF8A00]">
                    Suggested for Your Device
                  </span>
                </div>
                <span className="text-[11px] font-semibold text-white/50 bg-white/[0.05] border border-white/[0.08] px-2.5 py-0.5 rounded-full">
                  Detected: {platform.fullLabel}
                </span>
              </div>

              {suggestedEntry ? (
                <div>
                  <div className="flex items-start gap-4 mb-4">
                    <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-[#FF5A00]/25 to-[#FF5A00]/10 border border-[#FF5A00]/30 flex items-center justify-center flex-shrink-0 shadow-[0_0_20px_rgba(255,90,0,0.2)]">
                      {platform.type === 'windows' ? (
                        <Monitor className="w-6 h-6 text-[#FF5A00]" />
                      ) : (
                        <Smartphone className="w-6 h-6 text-[#FF5A00]" />
                      )}
                    </div>
                    <div>
                      <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        KnowToMigrate for {platform.name}
                        <span className="text-xs bg-[#FF5A00]/15 text-[#FF8A00] border border-[#FF5A00]/30 font-semibold px-2 py-0.5 rounded-md">
                          v{suggestedEntry.version}
                        </span>
                      </h2>
                      <p className="text-xs text-[#888888] mt-1 leading-relaxed">
                        {suggestedEntry.requirements}
                      </p>
                      <p className="text-xs text-[#FF8A00] mt-1 font-medium">
                        File: {suggestedEntry.filename} · {suggestedEntry.size}
                      </p>
                    </div>
                  </div>

                  {/* Primary Download Component */}
                  <DownloadCard
                    entry={suggestedEntry}
                    icon={platform.type === 'windows' ? Monitor : Smartphone}
                    isRecommendedDevice={true}
                  />

                  {/* Quick secondary links */}
                  <div className="mt-3.5 flex flex-wrap items-center justify-between gap-2 text-xs text-[#777]">
                    {platform.type === 'windows' ? (
                      <>
                        <span>Other Windows formats:</span>
                        <div className="flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => {
                              setActiveTab('windows')
                              const el = document.getElementById('win-msi')
                              el?.scrollIntoView({ behavior: 'smooth' })
                            }}
                            className="text-[#FF8A00] hover:underline cursor-pointer"
                          >
                            Enterprise .MSI Package
                          </button>
                          <span>·</span>
                          <button
                            type="button"
                            onClick={() => {
                              setActiveTab('windows')
                              const el = document.getElementById('win-portable')
                              el?.scrollIntoView({ behavior: 'smooth' })
                            }}
                            className="text-[#FF8A00] hover:underline cursor-pointer"
                          >
                            Portable .EXE
                          </button>
                        </div>
                      </>
                    ) : (
                      <span>Direct Android APK package with background transfer service.</span>
                    )}
                  </div>
                </div>
              ) : (
                /* Non-supported native platform (macOS / Linux / iOS) */
                <div className="text-center py-3">
                  <div className="w-12 h-12 rounded-2xl bg-white/[0.05] border border-white/[0.1] flex items-center justify-center mx-auto mb-3">
                    <Laptop className="w-6 h-6 text-[#FF8A00]" />
                  </div>
                  <h2 className="text-lg font-bold text-white mb-1">
                    {platform.name} Client Coming Soon
                  </h2>
                  <p className="text-xs text-[#8A8A8A] max-w-md mx-auto mb-5 leading-relaxed">
                    We detected you are visiting from <strong>{platform.name}</strong>. Native client builds for {platform.name} are in active development. You can transfer files right now using our zero-install Web Receiver, or download for Windows and Android.
                  </p>
                  <div className="flex flex-wrap items-center justify-center gap-3">
                    <a
                      href="https://knowtomigrate.web.app"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-4 py-2 rounded-xl text-xs font-semibold bg-gradient-to-r from-[#FF4D00] to-[#FF8A00] text-white flex items-center gap-1.5 shadow-md hover:brightness-110"
                    >
                      <Globe className="w-3.5 h-3.5" /> Launch Web Receiver
                    </a>
                    <button
                      type="button"
                      onClick={() => setActiveTab('windows')}
                      className="px-4 py-2 rounded-xl text-xs font-semibold bg-white/[0.05] hover:bg-white/[0.1] text-white border border-white/[0.1] cursor-pointer"
                    >
                      Windows Downloads
                    </button>
                    <button
                      type="button"
                      onClick={() => setActiveTab('android')}
                      className="px-4 py-2 rounded-xl text-xs font-semibold bg-white/[0.05] hover:bg-white/[0.1] text-white border border-white/[0.1] cursor-pointer"
                    >
                      Android APK
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Trust badges */}
          <div className="flex flex-wrap justify-center gap-5 text-xs text-[#8A8A8A]">
            {[
              { icon: Shield, text: 'AES-256-GCM Encrypted' },
              { icon: CheckCircle, text: 'SHA-256 Integrity Verified' },
              { icon: CheckCircle, text: 'Zero Cloud Storage' },
              { icon: CheckCircle, text: 'Works 100% Offline' },
            ].map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-center gap-1.5">
                <Icon className="w-3.5 h-3.5 text-[#FF5A00]" />
                <span>{text}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Filter Tabs */}
      <section className="px-4 max-w-5xl mx-auto mb-8">
        <div className="flex items-center justify-center gap-2 flex-wrap">
          <button
            type="button"
            onClick={() => setActiveTab('all')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
              activeTab === 'all'
                ? 'bg-[#FF5A00] text-white shadow-[0_0_16px_rgba(255,90,0,0.35)]'
                : 'bg-white/[0.04] text-[#888] hover:text-white hover:bg-white/[0.08] border border-white/[0.06]'
            }`}
          >
            All Platforms
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('windows')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer flex items-center gap-2 ${
              activeTab === 'windows'
                ? 'bg-[#FF5A00] text-white shadow-[0_0_16px_rgba(255,90,0,0.35)]'
                : 'bg-white/[0.04] text-[#888] hover:text-white hover:bg-white/[0.08] border border-white/[0.06]'
            }`}
          >
            <Monitor className="w-3.5 h-3.5" />
            <span>Windows (x64)</span>
            {isWindowsDetected && (
              <span className="text-[10px] bg-white/20 text-white px-1.5 py-0.2 rounded-full font-bold">
                Detected
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('android')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer flex items-center gap-2 ${
              activeTab === 'android'
                ? 'bg-[#FF5A00] text-white shadow-[0_0_16px_rgba(255,90,0,0.35)]'
                : 'bg-white/[0.04] text-[#888] hover:text-white hover:bg-white/[0.08] border border-white/[0.06]'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>Android (APK)</span>
            {isAndroidDetected && (
              <span className="text-[10px] bg-white/20 text-white px-1.5 py-0.2 rounded-full font-bold">
                Detected
              </span>
            )}
          </button>
        </div>
      </section>

      {/* Download Cards Grid */}
      <section className="pb-20 px-4 max-w-5xl mx-auto">
        <div className="grid md:grid-cols-2 gap-10">

          {/* Windows Column */}
          {(activeTab === 'all' || activeTab === 'windows') && (
            <div
              className={`rounded-3xl p-6 transition-all duration-300 ${
                isWindowsDetected
                  ? 'border border-[#FF5A00]/30 bg-white/[0.015] shadow-[0_0_40px_rgba(255,90,0,0.06)]'
                  : 'border border-white/[0.06] bg-transparent'
              }`}
            >
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/15 border border-[#FF5A00]/30 flex items-center justify-center">
                  <Monitor className="w-5 h-5 text-[#FF5A00]" strokeWidth={1.75} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-xl font-bold text-white">Windows</h2>
                    {isWindowsDetected && (
                      <span className="text-[11px] font-bold text-[#FF8A00] bg-[#FF5A00]/15 border border-[#FF5A00]/30 rounded-full px-2 py-0.5">
                        ✦ Your Device
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-[#666]">Native C# / WinUI 3 App · Windows 10/11 x64</p>
                </div>
                <img src="/logo.jpg" alt="KM" className="w-8 h-8 rounded-lg ml-auto border border-[#FF5A00]/20" />
              </div>

              <div className="space-y-4">
                {windowsDownloads.map(entry => (
                  <div key={entry.id} id={entry.id}>
                    <DownloadCard
                      entry={entry}
                      icon={Monitor}
                      isRecommendedDevice={isWindowsDetected && entry.recommended}
                    />
                  </div>
                ))}
              </div>

              <div className="mt-4 p-4 rounded-xl border border-white/[0.05] bg-white/[0.02]">
                <p className="text-xs text-[#555555] leading-relaxed">
                  <span className="text-[#8A8A8A] font-medium">Installation:</span> Run the official Setup installer
                  or double-click the portable EXE version. No administrator privileges needed for the portable EXE.
                  The <strong className="text-[#FF5A00]">KM lightning-bolt icon</strong> will appear in your taskbar and Start menu.
                </p>
              </div>
            </div>
          )}

          {/* Android Column */}
          {(activeTab === 'all' || activeTab === 'android') && (
            <div
              className={`rounded-3xl p-6 transition-all duration-300 ${
                isAndroidDetected
                  ? 'border border-[#FF5A00]/30 bg-white/[0.015] shadow-[0_0_40px_rgba(255,90,0,0.06)]'
                  : 'border border-white/[0.06] bg-transparent'
              }`}
            >
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/15 border border-[#FF5A00]/30 flex items-center justify-center">
                  <Smartphone className="w-5 h-5 text-[#FF5A00]" strokeWidth={1.75} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-xl font-bold text-white">Android</h2>
                    {isAndroidDetected && (
                      <span className="text-[11px] font-bold text-[#FF8A00] bg-[#FF5A00]/15 border border-[#FF5A00]/30 rounded-full px-2 py-0.5">
                        ✦ Your Device
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-[#666]">Kotlin + Jetpack Compose · Android 8.0+ (API 26)</p>
                </div>
                <img src="/logo.jpg" alt="KM" className="w-8 h-8 rounded-lg ml-auto border border-[#FF5A00]/20" />
              </div>

              <div className="space-y-4">
                {androidDownloads.map(entry => (
                  <div key={entry.id} id={entry.id}>
                    <DownloadCard
                      entry={entry}
                      icon={Smartphone}
                      isRecommendedDevice={isAndroidDetected && entry.recommended}
                    />
                  </div>
                ))}
              </div>

              <div className="mt-4 p-4 rounded-xl border border-white/[0.05] bg-white/[0.02]">
                <p className="text-xs text-[#555555] leading-relaxed">
                  <span className="text-[#8A8A8A] font-medium">Installation:</span> Enable "Install unknown apps"
                  in Settings &rarr; Security if prompted. Tap the APK file in Downloads to install.
                  The <strong className="text-[#FF5A00]">KM orange-on-black icon</strong> will appear on your home screen.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* System Requirements */}
        <div className="mt-12 grid md:grid-cols-2 gap-6">
          {[
            {
              platform: 'Windows Requirements',
              icon: Monitor,
              items: [
                'Windows 10 version 1809+ or Windows 11',
                '64-bit (x64) processor',
                '100 MB free disk space',
                '4 GB RAM recommended',
                'Wi-Fi adapter for local transfers',
              ],
            },
            {
              platform: 'Android Requirements',
              icon: Smartphone,
              items: [
                'Android 8.0 (Oreo) API 26+',
                'ARM64 / x86_64 / ARMv7 processor',
                '50 MB free storage',
                '2 GB RAM recommended',
                'Wi-Fi for local transfers',
              ],
            },
          ].map(({ platform: pName, icon: Icon, items }) => (
            <div key={pName} className="rounded-2xl border border-white/[0.06] bg-white/[0.02] p-5">
              <div className="flex items-center gap-2 mb-4">
                <Icon className="w-4 h-4 text-[#FF5A00]" />
                <h3 className="text-sm font-semibold text-white">{pName}</h3>
              </div>
              <ul className="space-y-2">
                {items.map(item => (
                  <li key={item} className="flex items-center gap-2 text-xs text-[#8A8A8A]">
                    <div className="w-1 h-1 rounded-full bg-[#FF5A00] flex-shrink-0" />
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>
    </main>
  )
}
