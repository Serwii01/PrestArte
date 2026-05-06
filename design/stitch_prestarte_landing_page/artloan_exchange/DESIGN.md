---
name: ArtLoan Exchange
colors:
  surface: '#faf8ff'
  surface-dim: '#d9d9e4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3fe'
  surface-container: '#ededf8'
  surface-container-high: '#e7e7f2'
  surface-container-highest: '#e2e1ed'
  on-surface: '#191b23'
  on-surface-variant: '#434654'
  inverse-surface: '#2e3038'
  inverse-on-surface: '#f0f0fb'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#1554d5'
  primary: '#003ca7'
  on-primary: '#ffffff'
  primary-container: '#1152d4'
  on-primary-container: '#cad5ff'
  inverse-primary: '#b4c5ff'
  secondary: '#515f78'
  on-secondary: '#ffffff'
  secondary-container: '#d2e0fe'
  on-secondary-container: '#55637d'
  tertiary: '#7e2900'
  on-tertiary: '#ffffff'
  tertiary-container: '#a63800'
  on-tertiary-container: '#ffcbb9'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174c'
  on-primary-fixed-variant: '#003da9'
  secondary-fixed: '#d6e3ff'
  secondary-fixed-dim: '#b9c7e4'
  on-secondary-fixed: '#0d1c32'
  on-secondary-fixed-variant: '#39475f'
  tertiary-fixed: '#ffdbcf'
  tertiary-fixed-dim: '#ffb59a'
  on-tertiary-fixed: '#380d00'
  on-tertiary-fixed-variant: '#802900'
  background: '#faf8ff'
  on-background: '#191b23'
  surface-variant: '#e2e1ed'
typography:
  display-xl:
    fontFamily: Manrope
    fontSize: 1.875rem
    fontWeight: '800'
    lineHeight: 2.25rem
    letterSpacing: -0.025em
  heading-lg:
    fontFamily: Manrope
    fontSize: 1.25rem
    fontWeight: '700'
    lineHeight: 1.75rem
  body-md:
    fontFamily: Manrope
    fontSize: 1rem
    fontWeight: '400'
    lineHeight: 1.5rem
  body-sm:
    fontFamily: Manrope
    fontSize: 0.875rem
    fontWeight: '400'
    lineHeight: 1.25rem
  label-xs:
    fontFamily: Manrope
    fontSize: 0.75rem
    fontWeight: '700'
    lineHeight: 1rem
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-max: 1280px
  gutter: 1.5rem
  section-gap: 2rem
  element-gap: 1rem
  card-padding: 1.25rem
---

## Brand & Style
The brand identity for ArtLoan Exchange is **Corporate / Modern** with a focus on trust, high-value asset management, and professional clarity. It targets museum curators, private collectors, and institutional administrators. 

The visual style is characterized by a "refined utilitarian" aesthetic. It utilizes a clean, systematic layout that prioritizes information density without sacrificing legibility. The interface evokes a sense of organized precision through the use of subtle borders, intentional whitespace, and a high-contrast primary color that signals action and authority. It feels like a high-end SaaS platform tailored for the art world—calm, professional, and trustworthy.

## Colors
The palette is built on a "Fidelity" color variant, using a deep, vibrant blue (`#1152d4`) as the primary driver for navigation and primary actions. 

- **Surface Strategy:** The system uses a layered approach with a cool-grey background (`#f6f6f8`) to provide contrast for white surface cards.
- **Typography Contrast:** Text follows a strict hierarchy using a near-black for main headings (`#111318`) and a slate-blue-grey (`#616f89`) for secondary metadata and placeholders.
- **Semantic Accents:** Status indicators use a standardized "pill" system with muted background tints and high-vibrancy text/icons to communicate urgency and state without overwhelming the user.

## Typography
The system uses **Manrope** exclusively to maintain a modern, geometric, yet highly readable feel across all interfaces. 

- **Headlines:** Use ExtraBold (`800`) weights for page titles to create a strong visual anchor.
- **Data Display:** Tables and lists utilize Bold (`700`) weights for primary identifiers (e.g., Artwork Names) and Regular (`400`) for supporting metadata.
- **Instructional Text:** Labels and table headers use a reduced font size (`0.75rem`) with increased letter spacing and uppercase styling to distinguish structural elements from user data.

## Layout & Spacing
The system utilizes a **Fixed Grid** approach for the main content area, centered with a maximum width of 1280px. 

- **Margins:** Horizontal page padding scales from 1.5rem on mobile to 5rem (px-20) on large desktop screens.
- **Rhythm:** A base-4 spacing system is used. Card gaps are consistently 1rem (`gap-4`), while major sections are separated by 2rem (`gap-8`). 
- **Tables:** Data tables use generous cell padding (`px-6 py-4`) to ensure touch targets are comfortable and data rows are distinct.

## Elevation & Depth
The system relies on **Tonal Layers** and **Low-contrast Outlines** rather than aggressive shadows.

- **Surface Hierarchy:** The primary background is tinted (`#f6f6f8`), while interactive containers and cards are pure white (`#ffffff`).
- **Shadows:** A single `shadow-sm` (soft, low-blur) is applied to cards and the navigation bar to provide a subtle lift from the background without creating a "floating" effect.
- **Interactivity:** Hover states are signaled by subtle background shifts (e.g., `bg-gray-50`) and 1px borders (`border-light`) that define the boundaries of interactive zones.

## Shapes
The shape language is **Rounded**, balancing a professional structure with approachable corners.

- **Small Components:** Inputs and buttons use a `0.5rem` (lg) radius.
- **Large Containers:** Content cards and data tables use a `0.75rem` (xl) radius.
- **Full Rounding:** Notification badges and status dots use `9999px` for a perfect circle.
- **Images:** Artwork thumbnails must use a `0.5rem` radius to mirror the button shapes, maintaining visual harmony.

## Components
- **Buttons:** Primary buttons feature solid `#1152d4` backgrounds with white text and a subtle `shadow-lg`. Secondary buttons (View Details) use a `primary/10` tinted background with primary-colored text for a softer hierarchy.
- **Status Chips:** Use a "dot + label" pattern. The background should be a 10% opacity tint of the status color, with a 100% opacity 6px circle (dot) and bold text.
- **Input Fields:** Search bars and text inputs are borderless with a light-grey background (`#f6f6f8`), transitioning to a white background with a 2px primary ring on focus.
- **Stat Cards:** Feature a vertical stack with a medium-weight label, a top-right aligned icon, and a large bold value.
- **Data Tables:** Headers should have a subtle background fill (`#f9fafb`) and a bottom border. Rows must include a hover state transition.
- **Navigation:** A sticky top bar with a persistent bottom border and subtle shadow for constant accessibility.