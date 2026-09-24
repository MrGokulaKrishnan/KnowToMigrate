export function DocsPage() {
  return (
    <main className="min-h-screen bg-black py-16 px-4">
      <div className="max-w-3xl mx-auto">
        <div className="mb-12">
          <span className="inline-block px-4 py-1.5 rounded-full text-xs font-bold tracking-widest text-[#FF5A00] border border-[#FF5A00]/30 bg-[#FF5A00]/10 mb-6">
            DOCUMENTATION
          </span>
          <h1 className="text-4xl font-extrabold text-white mb-4">Getting Started</h1>
          <p className="text-[#8A8A8A]">Everything you need to install, set up, and use KnowToMigrate.</p>
        </div>

        <DocSection title="Installation">
          <DocStep num={1} title="Download the app for your platform">
            <p>Visit the <a href="/download" className="text-[#FF5A00] hover:underline">Download page</a> and download the installer for your platform.</p>
            <ul className="list-disc pl-5 mt-2 space-y-1 text-[#8A8A8A]">
              <li><strong className="text-white">Windows</strong>: Download <code className="text-[#FF8A00]">KnowToMigrate.exe</code> or <code className="text-[#FF8A00]">KnowToMigrate.msi</code></li>
              <li><strong className="text-white">Android</strong>: Download <code className="text-[#FF8A00]">KnowToMigrate.apk</code> and install via Settings → Install unknown apps</li>
            </ul>
          </DocStep>
          <DocStep num={2} title="Install the application">
            <ul className="list-disc pl-5 space-y-1 text-[#8A8A8A]">
              <li><strong className="text-white">Windows MSI</strong>: Run the installer and follow the setup wizard</li>
              <li><strong className="text-white">Windows EXE</strong>: No installation needed  -  run directly</li>
              <li><strong className="text-white">Android APK</strong>: Tap the file in Downloads and tap Install</li>
            </ul>
          </DocStep>
          <DocStep num={3} title="Launch and set your device name">
            Open KnowToMigrate on each device. Go to <strong>Settings</strong> and set a recognizable device name.
          </DocStep>
        </DocSection>

        <DocSection title="Your First Transfer">
          <DocStep num={1} title="Connect both devices to the same Wi-Fi network">
            No internet required  -  just the same local network.
          </DocStep>
          <DocStep num={2} title="Open KnowToMigrate on both devices">
            Both devices will automatically appear in each other's Nearby Devices list within seconds.
          </DocStep>
          <DocStep num={3} title="Select files and choose destination">
            On the sending device: tap <strong>Send Files</strong>, select your files, and tap the destination device.
          </DocStep>
          <DocStep num={4} title="Accept on the receiving device">
            The receiving device shows an incoming transfer request. Tap <strong>Accept</strong>.
          </DocStep>
          <DocStep num={5} title="Wait for verification">
            After transfer completes, KnowToMigrate verifies SHA-256 checksums on both ends. You'll see <span className="text-[#22C55E] font-semibold">Integrity Verified</span>.
          </DocStep>
        </DocSection>

        <DocSection title="QR Code Pairing">
          <p className="text-[#8A8A8A] mb-4">
            If devices don't appear automatically (different subnets, firewall), use QR pairing:
          </p>
          <ol className="list-decimal pl-5 space-y-2 text-[#8A8A8A]">
            <li>On the receiving device, go to <strong className="text-white">Receive</strong> and tap <strong className="text-white">Show QR Code</strong></li>
            <li>On the sending device, tap <strong className="text-white">Scan QR Code</strong> and point at the screen</li>
            <li>Devices are paired instantly</li>
          </ol>
        </DocSection>

        <DocSection title="Security Model">
          <div className="space-y-4 text-[#8A8A8A]">
            <p>KnowToMigrate uses a layered security model:</p>
            <ul className="list-disc pl-5 space-y-2">
              <li><strong className="text-white">Transport Encryption</strong>: AES-256-GCM with per-session keys derived via X25519 Ephemeral Diffie-Hellman</li>
              <li><strong className="text-white">Integrity Verification</strong>: SHA-256 Merkle tree computed over all chunks  -  verified after transfer</li>
              <li><strong className="text-white">Device Identity</strong>: Persistent device keypair stored locally  -  no central authority</li>
              <li><strong className="text-white">Path Traversal Protection</strong>: All received file paths are sanitized  -  no <code>../</code> escapes possible</li>
              <li><strong className="text-white">No Cloud</strong>: Data never leaves your local network (for local transfers)</li>
            </ul>
          </div>
        </DocSection>

        <DocSection title="FAQ">
          <FaqItem q="Does KnowToMigrate require an account?">
            No. KnowToMigrate has zero accounts, zero sign-ups, and zero cloud dependency for local transfers.
          </FaqItem>
          <FaqItem q="Does it work without internet?">
            Yes  -  local transfers work completely offline as long as both devices are on the same Wi-Fi network.
          </FaqItem>
          <FaqItem q="What is the maximum file size?">
            There is no limit. KnowToMigrate streams files in 8 MB chunks and never loads the entire file into memory.
          </FaqItem>
          <FaqItem q="What happens if transfer is interrupted?">
            KnowToMigrate saves a checkpoint. When you reconnect, it resumes from the last verified chunk.
          </FaqItem>
          <FaqItem q="Is the website required for transfers?">
            Absolutely not. The website is only for downloads and documentation. The installed application works independently.
          </FaqItem>
        </DocSection>
      </div>
    </main>
  )
}

function DocSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-12">
      <h2 className="text-2xl font-bold text-white mb-6 pb-3 border-b border-white/[0.08]">{title}</h2>
      <div className="space-y-6">{children}</div>
    </section>
  )
}

function DocStep({ num, title, children }: { num: number; title: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-4">
      <div className="w-8 h-8 rounded-full bg-[#FF5A00]/15 border border-[#FF5A00]/30 flex items-center justify-center flex-shrink-0 mt-0.5">
        <span className="text-[#FF5A00] text-sm font-bold">{num}</span>
      </div>
      <div>
        <h3 className="font-semibold text-white mb-2">{title}</h3>
        <div className="text-[#8A8A8A] text-sm leading-relaxed">{children}</div>
      </div>
    </div>
  )
}

function FaqItem({ q, children }: { q: string; children: React.ReactNode }) {
  return (
    <div className="border border-white/[0.06] rounded-xl p-5">
      <h3 className="font-semibold text-white mb-2">{q}</h3>
      <p className="text-[#8A8A8A] text-sm">{children}</p>
    </div>
  )
}
