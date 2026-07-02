# Galaxy Watch Tweaker Design System

## 1. Product Intent

Galaxy Watch Tweaker is an operational Android utility for owners running Samsung watches on non-Samsung phones. The interface should feel precise, technical, and calm: a control surface for risky hooks, not a marketing page.

## 2. Tokens

- Color role: use Material 3 dynamic color roles only. No feature-specific raw hex values in Compose code.
- Surface hierarchy: `surfaceContainerLowest` for the app background, `surfaceContainer` for grouped settings, `primaryContainer` for status panels.
- Radius: use Material 3 shape tokens. Preference groups use `extraLarge`; controls use platform defaults.
- Spacing: 4dp grid. Screen horizontal padding is 16dp; vertical item rhythm is 12dp or 18dp.

## 3. Typography

- App title: Material 3 large top app bar typography.
- Section labels: `titleSmall`, semibold, primary color.
- Preference titles: `bodyLarge` or list item headline with medium weight.
- Field values and version values: `bodyMedium`, never hero-sized.

## 4. Layout

- Three primary tabs: Hooks, Profile, Versions.
- Content is a centered single column with max width 680dp.
- Use list rows for switches and editable fields. Avoid nested cards.
- Keep advanced spoof fields scannable; labels stay visible beside or above editable text fields.

## 5. Components

- `SettingsGroup`: titled full-width surface containing related rows.
- `ToggleListItem`: row with label, description, and switch; the full row toggles.
- `ProfileTextField`: labeled editable text field for spoof profile values.
- `VersionListItem`: package/version row with fallback text when unavailable.

## 6. States

- Disabled hooks remain visible and readable.
- Missing package versions display `Not detected`.
- Custom profile fields fall back to the S25 Ultra US preset when blank.

## 7. Accessibility

- Switch rows use `Role.Switch`.
- Text fields keep explicit labels.
- No emoji icons. Use Material icons only.
