// KnowToMigrate Design System — Windows / WinUI 3 Tokens
// Move Anything. Anywhere. Seamlessly.
//
// Generated from design/tokens/tokens.json v1.0.0
//
// Usage:
//   using KnowToMigrate.Design.Tokens;
//   myGrid.Background = new SolidColorBrush(KtmColors.BackgroundPrimary);
//   myText.Foreground = new SolidColorBrush(KtmColors.TextPrimary);
//   myGrid.CornerRadius = new CornerRadius(KtmRadius.Lg);

using System;
using Microsoft.UI;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace KnowToMigrate.Design.Tokens
{
    // ============================================================
    // COLORS
    // ============================================================

    /// <summary>All KnowToMigrate color constants.</summary>
    public static class KtmColors
    {
        // --------------------------------------------------------
        // Background
        // --------------------------------------------------------
        public static readonly Color BackgroundPrimary   = Color.FromArgb(255, 0,   0,   0);
        public static readonly Color BackgroundSecondary = Color.FromArgb(255, 5,   5,   5);
        public static readonly Color BackgroundTertiary  = Color.FromArgb(255, 8,   8,   8);
        public static readonly Color BackgroundCard      = Color.FromArgb(255, 12,  12,  12);
        public static readonly Color BackgroundElevated  = Color.FromArgb(255, 17,  17,  17);

        // --------------------------------------------------------
        // Brand
        // --------------------------------------------------------
        public static readonly Color BrandPrimary    = Color.FromArgb(255, 255, 90,  0);
        public static readonly Color BrandGradient1  = Color.FromArgb(255, 255, 77,  0);
        public static readonly Color BrandGradient2  = Color.FromArgb(255, 255, 90,  0);
        public static readonly Color BrandGradient3  = Color.FromArgb(255, 255, 106, 0);
        public static readonly Color BrandGradient4  = Color.FromArgb(255, 255, 138, 0);
        public static readonly Color BrandGradient5  = Color.FromArgb(255, 255, 176, 102);

        // --------------------------------------------------------
        // Text
        // --------------------------------------------------------
        public static readonly Color TextPrimary   = Color.FromArgb(255, 255, 255, 255);
        public static readonly Color TextSecondary = Color.FromArgb(255, 212, 212, 212);
        public static readonly Color TextMuted     = Color.FromArgb(255, 138, 138, 138);
        public static readonly Color TextDisabled  = Color.FromArgb(255, 85,  85,  85);

        // --------------------------------------------------------
        // Status
        // --------------------------------------------------------
        public static readonly Color StatusSuccess = Color.FromArgb(255, 34,  197, 94);
        public static readonly Color StatusWarning = Color.FromArgb(255, 245, 158, 11);
        public static readonly Color StatusError   = Color.FromArgb(255, 239, 68,  68);
        public static readonly Color StatusInfo    = Color.FromArgb(255, 56,  189, 248);

        // --------------------------------------------------------
        // Glass  (Alpha = 0–255, derived from opacity fractions)
        // --------------------------------------------------------
        /// <summary>rgba(255,255,255,0.04) — glass surface background</summary>
        public static readonly Color GlassBackground       = Color.FromArgb(10,  255, 255, 255);
        /// <summary>rgba(255,255,255,0.08) — glass border</summary>
        public static readonly Color GlassBorder           = Color.FromArgb(20,  255, 255, 255);
        /// <summary>rgba(255,255,255,0.12) — glass highlight</summary>
        public static readonly Color GlassHighlight        = Color.FromArgb(31,  255, 255, 255);
        /// <summary>rgba(255,90,0,0.15) — orange glow overlay</summary>
        public static readonly Color GlassOrangeGlow       = Color.FromArgb(38,  255, 90,  0);
        /// <summary>rgba(255,90,0,0.25) — stronger orange glow</summary>
        public static readonly Color GlassOrangeGlowStrong = Color.FromArgb(64,  255, 90,  0);

        // --------------------------------------------------------
        // Solid Brushes (convenience wrappers)
        // --------------------------------------------------------
        public static SolidColorBrush BrushBackgroundPrimary   => new(BackgroundPrimary);
        public static SolidColorBrush BrushBackgroundCard      => new(BackgroundCard);
        public static SolidColorBrush BrushBrandPrimary        => new(BrandPrimary);
        public static SolidColorBrush BrushTextPrimary         => new(TextPrimary);
        public static SolidColorBrush BrushTextSecondary       => new(TextSecondary);
        public static SolidColorBrush BrushTextMuted           => new(TextMuted);
        public static SolidColorBrush BrushStatusSuccess       => new(StatusSuccess);
        public static SolidColorBrush BrushStatusError         => new(StatusError);
        public static SolidColorBrush BrushStatusWarning       => new(StatusWarning);
        public static SolidColorBrush BrushStatusInfo          => new(StatusInfo);

        // --------------------------------------------------------
        // Gradient Brushes
        // --------------------------------------------------------

        /// <summary>Primary brand gradient (135 deg) — use for primary buttons, accents.</summary>
        public static LinearGradientBrush BrandGradientBrush =>
            new()
            {
                StartPoint = new Windows.Foundation.Point(0, 0),
                EndPoint   = new Windows.Foundation.Point(1, 1),
                GradientStops =
                {
                    new GradientStop { Color = BrandGradient1, Offset = 0.0 },
                    new GradientStop { Color = BrandGradient2, Offset = 0.3 },
                    new GradientStop { Color = BrandGradient3, Offset = 0.6 },
                    new GradientStop { Color = BrandGradient4, Offset = 1.0 },
                }
            };

        /// <summary>Horizontal gradient for progress bars.</summary>
        public static LinearGradientBrush ProgressFillBrush =>
            new()
            {
                StartPoint = new Windows.Foundation.Point(0, 0.5),
                EndPoint   = new Windows.Foundation.Point(1, 0.5),
                GradientStops =
                {
                    new GradientStop { Color = BrandGradient1, Offset = 0.0 },
                    new GradientStop { Color = BrandGradient4, Offset = 1.0 },
                }
            };

        /// <summary>Glass card surface gradient.</summary>
        public static LinearGradientBrush CardGradientBrush =>
            new()
            {
                StartPoint = new Windows.Foundation.Point(0, 0),
                EndPoint   = new Windows.Foundation.Point(1, 1),
                GradientStops =
                {
                    new GradientStop { Color = Color.FromArgb(13, 255, 255, 255), Offset = 0.0 },
                    new GradientStop { Color = Color.FromArgb(5,  255, 255, 255), Offset = 1.0 },
                }
            };

        /// <summary>Surface darkening gradient (top → bottom).</summary>
        public static LinearGradientBrush SurfaceGradientBrush =>
            new()
            {
                StartPoint = new Windows.Foundation.Point(0.5, 0),
                EndPoint   = new Windows.Foundation.Point(0.5, 1),
                GradientStops =
                {
                    new GradientStop { Color = BackgroundElevated, Offset = 0.0 },
                    new GradientStop { Color = BackgroundTertiary, Offset = 1.0 },
                }
            };

        /// <summary>Gloss highlight — use as an overlay at the top of glass cards.</summary>
        public static LinearGradientBrush GlossBrush =>
            new()
            {
                StartPoint = new Windows.Foundation.Point(0.5, 0),
                EndPoint   = new Windows.Foundation.Point(0.5, 1),
                GradientStops =
                {
                    new GradientStop { Color = Color.FromArgb(31, 255, 255, 255), Offset = 0.0 },
                    new GradientStop { Color = Color.FromArgb(0,  255, 255, 255), Offset = 0.5 },
                }
            };
    }

    // ============================================================
    // TYPOGRAPHY
    // ============================================================

    /// <summary>Font sizes and weights for KnowToMigrate UI.</summary>
    public static class KtmTypography
    {
        // Scale (device-independent pixels, matching sp/px from tokens)
        public const double TextXs   = 11;
        public const double TextSm   = 13;
        public const double TextBase = 15;
        public const double TextMd   = 17;
        public const double TextLg   = 20;
        public const double TextXl   = 24;
        public const double Text2Xl  = 30;
        public const double Text3Xl  = 36;
        public const double Text4Xl  = 48;
        public const double TextHero = 64;

        // Weights
        public const int WeightRegular   = 400;
        public const int WeightMedium    = 500;
        public const int WeightSemibold  = 600;
        public const int WeightBold      = 700;
        public const int WeightExtrabold = 800;

        // Line-height multipliers
        public const double LineHeightTight   = 1.2;
        public const double LineHeightSnug    = 1.35;
        public const double LineHeightNormal  = 1.5;
        public const double LineHeightRelaxed = 1.65;

        // Font families
        public const string FontPrimary = "Segoe UI Variable";
        public const string FontMono    = "Cascadia Code";
    }

    // ============================================================
    // SPACING
    // ============================================================

    /// <summary>Spacing values in device-independent pixels (dp).</summary>
    public static class KtmSpacing
    {
        public const double Space1  = 4;
        public const double Space2  = 8;
        public const double Space3  = 12;
        public const double Space4  = 16;
        public const double Space5  = 20;
        public const double Space6  = 24;
        public const double Space8  = 32;
        public const double Space10 = 40;
        public const double Space12 = 48;
        public const double Space16 = 64;
        public const double Space20 = 80;
        public const double Space24 = 96;
    }

    // ============================================================
    // BORDER RADIUS
    // ============================================================

    /// <summary>CornerRadius values for UI controls.</summary>
    public static class KtmRadius
    {
        public const double SmValue   = 8;
        public const double MdValue   = 12;
        public const double LgValue   = 18;
        public const double XlValue   = 24;
        public const double XxlValue  = 32;
        public const double FullValue = 9999;

        public static CornerRadius Sm   => new(SmValue);
        public static CornerRadius Md   => new(MdValue);
        public static CornerRadius Lg   => new(LgValue);
        public static CornerRadius Xl   => new(XlValue);
        public static CornerRadius Xxl  => new(XxlValue);
        public static CornerRadius Full => new(FullValue);
    }

    // ============================================================
    // ANIMATION DURATIONS (milliseconds)
    // ============================================================

    /// <summary>Animation timing constants.</summary>
    public static class KtmDuration
    {
        public static readonly TimeSpan Instant   = TimeSpan.FromMilliseconds(80);
        public static readonly TimeSpan Fast      = TimeSpan.FromMilliseconds(150);
        public static readonly TimeSpan Normal    = TimeSpan.FromMilliseconds(250);
        public static readonly TimeSpan Slow      = TimeSpan.FromMilliseconds(400);
        public static readonly TimeSpan Slower    = TimeSpan.FromMilliseconds(600);
        public static readonly TimeSpan Discovery = TimeSpan.FromMilliseconds(2000);
        public static readonly TimeSpan Pulse     = TimeSpan.FromMilliseconds(1500);
    }

    // ============================================================
    // ICONS
    // ============================================================

    /// <summary>Icon size constants.</summary>
    public static class KtmIcons
    {
        public const double StrokeWidth = 1.75;

        public const double SizeXs  = 14;
        public const double SizeSm  = 16;
        public const double SizeMd  = 20;
        public const double SizeLg  = 24;
        public const double SizeXl  = 32;
        public const double Size2Xl = 48;
    }

    // ============================================================
    // COMPONENTS
    // ============================================================

    /// <summary>Button size and style constants.</summary>
    public static class KtmButton
    {
        public const double Height     = 48;
        public const double PaddingX   = 24;
        public const double FontSize   = KtmTypography.TextBase;
        public const int    FontWeight = KtmTypography.WeightSemibold;
        public static CornerRadius Radius => KtmRadius.Md;
        public const double MinTapTarget = 44;
    }

    /// <summary>Card style constants.</summary>
    public static class KtmCard
    {
        public static CornerRadius Radius  => KtmRadius.Lg;
        public const  double       Padding = KtmSpacing.Space5;
    }

    /// <summary>Device card style constants.</summary>
    public static class KtmDeviceCard
    {
        public static CornerRadius Radius => KtmRadius.Lg;
        public static readonly Color ActiveBorderColor = Color.FromArgb(102, 255, 90, 0);
        public static readonly Color ActiveGlowColor   = Color.FromArgb(64,  255, 90, 0);
    }

    /// <summary>Progress bar style constants.</summary>
    public static class KtmProgressBar
    {
        public const double Height = 6;
        public static CornerRadius Radius => KtmRadius.Full;
        public static readonly Color Background = KtmColors.GlassBorder;
        public static LinearGradientBrush FillBrush => KtmColors.ProgressFillBrush;
    }

    /// <summary>Input field style constants.</summary>
    public static class KtmInput
    {
        public const  double Height = 48;
        public static CornerRadius Radius => KtmRadius.Md;
        public static readonly Color Background  = KtmColors.GlassBackground;
        public static readonly Color BorderColor = KtmColors.GlassBorder;
        public static readonly Color FocusBorderColor = Color.FromArgb(128, 255, 90, 0);
    }

    /// <summary>Badge style constants.</summary>
    public static class KtmBadge
    {
        public static CornerRadius Radius     => KtmRadius.Full;
        public const  double       PaddingX   = 10;
        public const  double       PaddingY   = 3;
        public const  double       FontSize   = 12;
        public const  int          FontWeight = KtmTypography.WeightSemibold;
    }

    /// <summary>Accessibility constants.</summary>
    public static class KtmAccessibility
    {
        public const  double MinTapTarget    = 44;
        public const  double FocusRingWidth  = 3;
        public static readonly Color FocusRingColor = Color.FromArgb(153, 255, 90, 0);
        public const  float   MinContrastRatio = 4.5f;
    }
}
