import { useState } from 'react'
import { Download, Shield, CheckCircle, Monitor, Smartphone, Copy, Check, AlertTriangle } from 'lucide-react'

interface DownloadEntry {
  filename: string
  version: string
  size: string
  sha256: string
  requirements: string
  downloadUrl: string
}

const WINDOWS_DOWNLOADS: DownloadEntry[] = [
  {
    filename: 'KnowToMigrate-1.0.0-x64.msi',
    version: '1.0.0',
    size: '~28 MB',
    sha256: 'BUILD_PENDING_SHA256_MSI_AAAAAABBBBBBCCCCCCDDDDDDEEEEEEFFFFFFFF000000111111222222333333',
    requirements: 'Windows 10 / 11, 64-bit',
    downloadUrl: '#',
  },
  {
    filename: 'KnowToMigrate-1.0.0-x64.exe',
    version: '1.0.0',
    size: '~25 MB',
    sha256: 'BUILD_PENDING_SHA256_EXE_AAAAAABBBBBBCCCCCCDDDDDDEEEEEEFFFFFFFF000000111111222222333333',
    requirements: 'Windows 10 / 11, 64-bit (portable)',
    downloadUrl: '#',
  },
]

const ANDROID_DOWNLOADS: DownloadEntry[] = [
  {
    filename: 'KnowToMigrate-1.0.0.apk',
    version: '1.0.0',
    size: '~18 MB',
    sha256: 'BUILD_PENDING_SHA256_APK_AAAAAABBBBBBCCCCCCDDDDDDEEEEEEFFFFFFFF000000111111222222333333',
    requirements: 'Android 8.0+ (API 26)',
    downloadUrl: '#',
  },
]

function CopyHash({ hash }: { hash: string }) {
  const [copied, setCopied] = useState(false)
  const short = hash.startsWith('BUILD_PENDING') ? 'Pending build...' : `${hash.slice(0, 16)}…`
  const handleCopy = () => {
    navigator.clipboard.writeText(hash)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }
  return (
    <div className="flex items-center gap-2 mt-2">
      <code className="text-xs text-[#8A8A8A] font-mono bg-[#111111] px-2 py-1 rounded-md flex-1 truncate">
        SHA-256: {short}
      </code>
      <button onClick={handleCopy} className="p-1.5 rounded-md hover:bg-white/5 transition-colors text-[#555]">
        {copied ? <Check className="w-3.5 h-3.5 text-[#22C55E]" /> : <Copy className="w-3.5 h-3.5" />}
      </button>
    </div>
  )
}

function DownloadCard({ entry, icon: Icon }: { entry: DownloadEntry; icon: React.ElementType }) {
  return (
    <div className="rounded-2xl border border-white/[0.08] bg-white/[0.03] p-5 hover:border-[#FF5A00]/30 hover:bg-[#FF5A00]/[0.03] transition-all duration-300">
      <div className="flex items-start gap-3 mb-3">
        <div className="w-9 h-9 rounded-xl bg-[#FF5A00]/15 flex items-center justify-center flex-shrink-0">
          <Icon className="w-4.5 h-4.5 text-[#FF5A00]" strokeWidth={1.75} />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-white truncate">{entry.filename}</p>
          <p className="text-xs text-[#555555] mt-0.5">{entry.requirements} · {entry.size}</p>
        </div>
        <span className="text-xs bg-[#FF5A00]/10 text-[#FF5A00] border border-[#FF5A00]/20 rounded-full px-2 py-0.5 flex-shrink-0">
          v{entry.version}
        </span>
      </div>

      <CopyHash hash={entry.sha256} />

      <a
        href={entry.downloadUrl}
        className="mt-4 flex items-center justify-center gap-2 w-full py-2.5 rounded-xl text-sm font-semibold transition-all duration-200"
        style={{
          background: entry.downloadUrl === '#'
            ? 'transparent'
            : 'linear-gradient(135deg, #FF4D00, #FF8A00)',
          color: entry.downloadUrl === '#' ? '#555' : '#fff',
          border: entry.downloadUrl === '#' ? '1px solid #2A2A2A' : 'none',
          cursor: entry.downloadUrl === '#' ? 'default' : 'pointer',
        }}
      >
        <Download className="w-4 h-4" />
        {entry.downloadUrl === '#' ? 'Coming Soon' : `Download ${entry.filename.split('.').pop()?.toUpperCase()}`}
      </a>
    </div>
  )
}

export function DownloadPage() {
  return (
    <main className="min-h-screen bg-black">
      {/* Hero with Logo */}
      <section className="py-24 px-4 text-center relative overflow-hidden">
        {/* Glow effect */}
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background: 'radial-gradient(ellipse 60% 40% at 50% 0%, rgba(255,90,0,0.12) 0%, transparent 70%)',
          }}
        />

        <div className="relative max-w-2xl mx-auto">
          {/* KM Logo */}
          <div className="flex justify-center mb-8">
            <div className="relative">
              <div
                className="w-32 h-32 rounded-3xl overflow-hidden border-2 border-[#FF5A00]/40"
                style={{ boxShadow: '0 0 60px rgba(255,90,0,0.25), 0 0 120px rgba(255,90,0,0.08)' }}
              >
                <img
                  src="/logo.jpg"
                  alt="KnowToMigrate Logo"
                  className="w-full h-full object-cover"
                />
              </div>
              {/* Orange glow ring */}
              <div
                className="absolute inset-0 rounded-3xl pointer-events-none"
                style={{
                  background: 'linear-gradient(135deg, rgba(255,90,0,0.15), transparent)',
                  border: '1px solid rgba(255,90,0,0.2)',
                }}
              />
            </div>
          </div>

          <span className="inline-block px-4 py-1.5 rounded-full text-xs font-bold tracking-widest text-[#FF5A00] border border-[#FF5A00]/30 bg-[#FF5A00]/10 mb-5">
            FREE DOWNLOAD
          </span>

          <h1 className="text-5xl font-extrabold text-white mb-4 leading-tight">
            Download{' '}
            <span className="bg-gradient-to-r from-[#FF4D00] to-[#FF8A00] bg-clip-text text-transparent">
              KnowToMigrate
            </span>
          </h1>
          <p className="text-[#8A8A8A] text-lg max-w-xl mx-auto">
            A standalone application — no account, no cloud, no browser required.
            Install once. Transfer forever.
          </p>

          {/* Trust badges */}
          <div className="flex flex-wrap justify-center gap-4 mt-8">
            {[
              { icon: Shield, text: 'AES-256-GCM Encrypted' },
              { icon: CheckCircle, text: 'SHA-256 Verified' },
              { icon: CheckCircle, text: 'No Account Required' },
              { icon: CheckCircle, text: 'Works Offline' },
            ].map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-center gap-1.5 text-xs text-[#8A8A8A]">
                <Icon className="w-3.5 h-3.5 text-[#FF5A00]" />
                <span>{text}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Download sections */}
      <section className="py-12 px-4 max-w-5xl mx-auto">
        <div className="grid md:grid-cols-2 gap-12">

          {/* Windows */}
          <div>
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/15 flex items-center justify-center">
                <Monitor className="w-5 h-5 text-[#FF5A00]" strokeWidth={1.75} />
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">Windows</h2>
                <p className="text-xs text-[#555]">WinUI 3 native app · Windows 10/11 x64</p>
              </div>
              {/* Logo shown on card */}
              <img src="/logo.jpg" alt="KM" className="w-8 h-8 rounded-lg ml-auto border border-[#FF5A00]/20" />
            </div>
            <div className="space-y-4">
              {WINDOWS_DOWNLOADS.map(entry => (
                <DownloadCard key={entry.filename} entry={entry} icon={Monitor} />
              ))}
            </div>
            <div className="mt-4 p-4 rounded-xl border border-white/[0.05] bg-white/[0.02]">
              <p className="text-xs text-[#555555] leading-relaxed">
                <span className="text-[#8A8A8A] font-medium">Installation:</span> Run the .msi installer or
                double-click the .exe portable version. No administrator account needed for the portable .exe.
                The <strong className="text-[#FF5A00]">KM lightning-bolt icon</strong> will appear in your taskbar and Start menu.
              </p>
            </div>
          </div>

          {/* Android */}
          <div>
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/15 flex items-center justify-center">
                <Smartphone className="w-5 h-5 text-[#FF5A00]" strokeWidth={1.75} />
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">Android</h2>
                <p className="text-xs text-[#555]">Kotlin + Compose · Android 8.0+ (API 26)</p>
              </div>
              {/* Logo shown on card */}
              <img src="/logo.jpg" alt="KM" className="w-8 h-8 rounded-lg ml-auto border border-[#FF5A00]/20" />
            </div>
            <div className="space-y-4">
              {ANDROID_DOWNLOADS.map(entry => (
                <DownloadCard key={entry.filename} entry={entry} icon={Smartphone} />
              ))}
            </div>
            <div className="mt-4 p-4 rounded-xl border border-white/[0.05] bg-white/[0.02]">
              <p className="text-xs text-[#555555] leading-relaxed">
                <span className="text-[#8A8A8A] font-medium">Installation:</span> Enable "Install unknown apps"
                in Settings → Security. Tap the .apk file in Downloads to install.
                The <strong className="text-[#FF5A00]">KM orange-on-black icon</strong> will appear on your home screen.
              </p>
            </div>
          </div>
        </div>

        {/* SHA-256 verification note */}
        <div className="mt-12 p-5 rounded-2xl border border-[#FF5A00]/20 bg-[#FF5A00]/[0.04]">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-[#FF5A00] flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-white mb-1">Verify before installing</p>
              <p className="text-xs text-[#8A8A8A] leading-relaxed">
                Always verify the SHA-256 hash of your download before installing. On Windows:
                <code className="mx-1 px-1.5 py-0.5 bg-black/40 rounded text-[#FF8A00] text-xs">
                  Get-FileHash KnowToMigrate.exe -Algorithm SHA256
                </code>
                . On Android, use a hash checker app.
              </p>
            </div>
          </div>
        </div>

        {/* System Requirements */}
        <div className="mt-8 grid md:grid-cols-2 gap-6">
          {[
            {
              platform: 'Windows Requirements',
              icon: Monitor,
              items: ['Windows 10 version 1809+ or Windows 11', '64-bit (x64) processor', '100 MB free disk space', '4 GB RAM recommended', 'Wi-Fi adapter for local transfers'],
            },
            {
              platform: 'Android Requirements',
              icon: Smartphone,
              items: ['Android 8.0 (Oreo) API 26+', 'ARM64 / x86_64 / ARMv7 processor', '50 MB free storage', '2 GB RAM recommended', 'Wi-Fi for local transfers'],
            },
          ].map(({ platform, icon: Icon, items }) => (
            <div key={platform} className="rounded-2xl border border-white/[0.06] bg-white/[0.02] p-5">
              <div className="flex items-center gap-2 mb-4">
                <Icon className="w-4 h-4 text-[#FF5A00]" />
                <h3 className="text-sm font-semibold text-white">{platform}</h3>
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
