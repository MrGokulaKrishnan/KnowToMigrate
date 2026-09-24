import { useState } from 'react'
import { Download, Shield, CheckCircle, Monitor, Smartphone, Copy, Check, AlertTriangle, Eye, EyeOff } from 'lucide-react'

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
    size: '62.7 MB',
    sha256: '28F8A7D4D41024762D88DB24DD27AA3F1A14A37E2B1B483AF30A033474557BDF',
    requirements: 'Windows 10 / 11, 64-bit (Official MSI Installer)',
    downloadUrl: '/download/KnowToMigrate-1.0.0-x64.msi',
  },
  {
    filename: 'KnowToMigrate-1.0.0-x64.exe',
    version: '1.0.0',
    size: '68.7 MB',
    sha256: '2B888126CE08F5C8F52BD7CAC775DA2C6FCA25475F1B29CA9B3609F367DAADE9',
    requirements: 'Windows 10 / 11, 64-bit (Portable Standalone EXE)',
    downloadUrl: '/download/KnowToMigrate-1.0.0-x64.exe',
  },
]

const ANDROID_DOWNLOADS: DownloadEntry[] = [
  {
    filename: 'KnowToMigrate-1.0.0.apk',
    version: '1.0.0',
    size: '17.8 MB',
    sha256: '239D0A3FD506EAFE5A09ECC92F7E4815171B197C55A95B7BB73FC14A2FD1EADA',
    requirements: 'Android 8.0+ (API 26) - APK Package',
    downloadUrl: '/download/KnowToMigrate-1.0.0.apk',
  },
]

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

      {/* Password-style Hidden SHA-256 */}
      <HiddenHash hash={entry.sha256} />

      {/* Direct Download Button */}
      <a
        href={entry.downloadUrl}
        download={entry.filename}
        className="mt-4 flex items-center justify-center gap-2 w-full py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 cursor-pointer shadow-lg hover:brightness-110 active:scale-[0.98]"
        style={{
          background: 'linear-gradient(135deg, #FF4D00, #FF8A00)',
          color: '#fff',
        }}
      >
        <Download className="w-4 h-4" />
        Download {entry.filename.split('.').pop()?.toUpperCase()}
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
            A standalone application - no account, no cloud, no browser required.
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
                <p className="text-xs text-[#555]">Native Windows App · Windows 10/11 x64</p>
              </div>
              <img src="/logo.jpg" alt="KM" className="w-8 h-8 rounded-lg ml-auto border border-[#FF5A00]/20" />
            </div>
            <div className="space-y-4">
              {WINDOWS_DOWNLOADS.map(entry => (
                <DownloadCard key={entry.filename} entry={entry} icon={Monitor} />
              ))}
            </div>
            <div className="mt-4 p-4 rounded-xl border border-white/[0.05] bg-white/[0.02]">
              <p className="text-xs text-[#555555] leading-relaxed">
                <span className="text-[#8A8A8A] font-medium">Installation:</span> Run the official MSI installer
                or double-click the portable EXE version. No administrator privileges needed for the portable EXE.
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
                <p className="text-xs text-[#555]">Kotlin + Jetpack Compose · Android 8.0+ (API 26)</p>
              </div>
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
                in Settings &rarr; Security if prompted. Tap the APK file in Downloads to install.
                The <strong className="text-[#FF5A00]">KM orange-on-black icon</strong> will appear on your home screen.
              </p>
            </div>
          </div>
        </div>

        {/* System Requirements */}
        <div className="mt-12 grid md:grid-cols-2 gap-6">
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
