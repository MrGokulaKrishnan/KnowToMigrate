/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        km: {
          orange: '#FF5A00',
          bright: '#FF6A00',
          amber: '#FF8A00',
          soft: '#FFB066',
          bg: '#000000',
          card: 'rgba(255, 255, 255, 0.035)',
          border: 'rgba(255, 255, 255, 0.08)'
        }
      },
      boxShadow: {
        'km-glow': '0 0 24px rgba(255, 90, 0, 0.25)',
        'km-glow-lg': '0 0 40px rgba(255, 90, 0, 0.40)',
        'glass-inset': 'inset 0 1px 1px rgba(255, 255, 255, 0.1)'
      }
    },
  },
  plugins: [],
}
