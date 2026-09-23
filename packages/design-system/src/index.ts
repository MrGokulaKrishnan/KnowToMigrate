/**
 * KnowToMigrate - Design System Tokens
 * Theme: AMOLED Pure Black + KM Electric Orange + Liquid Glass
 */

export const KM_COLORS = {
  // Surface
  bgPure: '#000000',
  bgSecondary: '#050505',
  bgTertiary: '#0A0A0A',
  bgElevated: '#111111',

  // Glass
  glassSurface: 'rgba(255, 255, 255, 0.035)',
  glassSurfaceHover: 'rgba(255, 255, 255, 0.065)',
  glassSurfaceActive: 'rgba(255, 255, 255, 0.090)',
  glassBorder: 'rgba(255, 255, 255, 0.08)',
  glassBorderBright: 'rgba(255, 255, 255, 0.16)',
  glassHighlight: 'rgba(255, 255, 255, 0.05)',

  // Brand KM Electric Orange
  orange: '#FF5A00',
  orangeBright: '#FF6A00',
  orangeAmber: '#FF8A00',
  orangeSoft: '#FFB066',
  orangeGlow: 'rgba(255, 90, 0, 0.22)',
  orangeGlowIntense: 'rgba(255, 90, 0, 0.45)',

  // Semantic Status
  statusSuccess: '#22C55E',
  statusWarning: '#F59E0B',
  statusError: '#EF4444',
  statusInfo: '#38BDF8',
} as const;

export const KM_GRADIENTS = {
  energy: 'linear-gradient(135deg, #FF4D00 0%, #FF6500 50%, #FF9A3D 100%)',
  glass: 'linear-gradient(135deg, rgba(255, 90, 0, 0.15) 0%, rgba(255, 77, 0, 0.02) 100%)',
} as const;

export const KM_TYPOGRAPHY = {
  fontDisplay: '40px',
  fontH1: '28px',
  fontH2: '22px',
  fontTitle: '18px',
  fontBody: '15px',
  fontCaption: '13px',
  fontTiny: '11px',
} as const;

export const KM_TOKENS = {
  colors: KM_COLORS,
  gradients: KM_GRADIENTS,
  typography: KM_TYPOGRAPHY,
} as const;

export default KM_TOKENS;
