import { Routes, Route } from 'react-router-dom'
import { Navbar } from './components/Navbar'
import { Footer } from './components/Footer'
import { HomePage } from './pages/HomePage'
import { DownloadPage } from './pages/DownloadPage'
import { FeaturesPage } from './pages/FeaturesPage'
import { DocsPage } from './pages/DocsPage'

export default function App() {
  return (
    <div className="min-h-screen bg-black text-white">
      <Navbar />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/download" element={<DownloadPage />} />
        <Route path="/features" element={<FeaturesPage />} />
        <Route path="/docs" element={<DocsPage />} />
      </Routes>
      <Footer />
    </div>
  )
}
