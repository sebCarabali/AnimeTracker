# Spine Pair Review — AnimeTracker

## Overall verdict

The spine pair is structurally sound: section order in both files matches canonical shape exactly, all three PRD user journeys have complete Key Flows, and every `{colors.*}`/`{typography.*}` cross-reference in EXPERIENCE.md resolves to a real DESIGN.md token. The weak spot is component coverage — the global nav and the primary/secondary buttons are visually specified in DESIGN.md but never get a behavioral row in EXPERIENCE.md § Component Patterns, and Status badge's three non-accent state colors (success/info/ink-disabled) have no defined foreground pairing or explicit contrast callout, unlike accent-dark. None of this is broken, but a downstream consumer building the nav, the buttons, or a non-"viendo" badge would have to guess.

## 1. Flow coverage — strong

Checked all three PRD §2.3 journeys against EXPERIENCE.md § Key Flows. All three titles match PRD wording verbatim ("Mica revisa qué le toca ver hoy (UJ-1)", "Diego nota que bajó el ritmo (UJ-2)", "Una nueva usuaria invitada entra por primera vez (UJ-3)"). Each flow has a named protagonist (Mica, Diego, Vale — Vale is a name EXPERIENCE.md introduces for UJ-3's unnamed PRD protagonist, which is reasonable), numbered steps, a bolded **Climax**, and an explicit failure/edge path (Flow 1 "Fallback:", Flow 2 "Caso de borde:", Flow 3 "Caso de borde:").

### Findings
None.

## 2. Token completeness — adequate

Extracted all 30 color tokens, 7 typography roles, 5 `rounded` values, and spacing scale from DESIGN.md frontmatter, plus every `{path.to.token}` reference in both files' prose (`{colors.accent-dark}`, `{colors.ink-disabled-dark}`, `{colors.surface-base-dark}`, `{typography.numeric}`, and all references inside the `components:` frontmatter block). Every reference resolves to a defined token — no broken cross-refs. All colors have both a light and dark hex value.

### Findings
- **high** Status badge (DESIGN.md frontmatter `components.status-badge`, lines 103–105, and § Components line 177) defines only `radius` and `type` — no background/foreground token at all. Prose assigns `accent-dark` / `success-dark` / `info-dark` / `ink-disabled-dark` per state but never states whether these are a fill, a text color, or a border, and only `accent-dark` gets a paired `-foreground` token (`accent-foreground-dark`). `success-dark` and `ink-disabled-dark` have no foreground counterpart defined anywhere in the frontmatter. *Fix:* either add per-state foreground tokens to `components.status-badge` (mirroring `button-primary`'s background/foreground pair) or state explicitly in § Components that badge color is applied to text/border only, never as a fill, so no foreground token is needed.
- **high** EXPERIENCE.md § Accessibility Floor (line 89) states the WCAG AA contrast requirement only for `{colors.accent-dark}` over `{colors.surface-base-dark}` "en botones y badges." The other three badge colors (`success-dark`, `info-dark`, `ink-disabled-dark`) are load-bearing for the same Status badge component (DESIGN.md § Components, line 177) but get no explicit contrast statement. *Fix:* extend the Accessibility Floor line to name all four badge colors, or state a general rule ("all badge fill/text colors verified at AA against their surface") instead of singling out accent-dark.
- **medium** `danger` / `danger-dark` (DESIGN.md frontmatter lines 22, 37) are defined with full light/dark hex pairs but never referenced by any component, state, or prose section in either file. *Fix:* either wire `danger` to a real use (e.g., the "Acceso denegado" state, which currently has no color spec at all — see Component coverage) or remove the token.
- **low** `info-foreground` / `info-foreground-dark` (lines 18, 33) are defined but never referenced — `onboarding-banner` uses `info-dark` for border and accent-text only, no foreground use. Likely dead weight; confirm before final.

## 3. Component coverage — thin

Extracted every component name appearing anywhere in DESIGN.md or EXPERIENCE.md: Poster card, Status badge, Stale banner, Trend bar, Onboarding banner, Theme toggle, Button primary/secondary, Empty state (DESIGN.md § Components), plus nav principal, skeleton, and a week/month period toggle named only in prose.

### Findings
- **high** "Nav principal" is a global, every-surface UI element (referenced in EXPERIENCE.md § Information Architecture line 32 and § Responsive & Platform lines 97–101) but has no entry in DESIGN.md § Components (no visual spec: layout, active-state indication, spacing) and no row in EXPERIENCE.md § Component Patterns (no behavioral spec: what "active" looks like, whether it's sticky, hamburger vs. bottom-bar collapse behavior beyond the `[ASSUMPTION]` tag). *Fix:* add a Nav component row to both files.
- **high** Button primary/secondary has a full visual spec in DESIGN.md (frontmatter `components.button-primary`/`button-secondary`, lines 106–114, and § Components line 182) but no corresponding row in EXPERIENCE.md § Component Patterns (line 54–61 table). Behavioral questions — disabled/loading state on "reintentar sync," what happens on click for "Ver instrucciones de MAL-Sync" — are unanswered. *Fix:* add a Button row to § Component Patterns.
- **medium** The Trend bar's week/month toggle (EXPERIENCE.md § Component Patterns line 59: "Por período (semana, con toggle a mes)") is a real interactive control but has no visual spec in DESIGN.md § Components, no frontmatter token entry, and no mention in § Interaction Primitives (lines 79–83), which otherwise enumerates every click/tap interaction. *Fix:* add the toggle to Interaction Primitives and give it a component entry (or fold it explicitly into the Trend bar row with its own behavioral rule).
- **medium** "Skeleton" loading treatment is named in EXPERIENCE.md § State Patterns (line 73: "Skeleton de tarjetas/barras") but has no visual spec anywhere in DESIGN.md § Components. *Fix:* add a minimal Skeleton entry (shape, color/shimmer treatment) since it appears on every surface's cold load.
- **low** "Empty state" has a DESIGN.md § Components entry (line 183) but its behavior is covered only in EXPERIENCE.md § State Patterns, not § Component Patterns. Functionally covered, but technically inconsistent with the pattern every other DESIGN.md component follows (a matching Component Patterns row). Low severity since the content isn't actually missing, just filed under a different heading than DESIGN.md implies.

## 4. State coverage — adequate

Walked each IA surface (Login, Acceso denegado, Hoy/Seguí Viendo, Por Estado, Tendencias, Configurar MAL-Sync) against plausible states given FR-9 (degradation) and FR-3/4/5 (empty-data consequences), then checked § State Patterns (lines 65–73).

### Findings
- **medium** Login has no defined state for an OAuth failure/cancellation that isn't a whitelist rejection (e.g., AniList itself is down, or the user cancels consent mid-flow). Only "Acceso denegado (no en Whitelist)" is covered (line 71); a generic OAuth-level failure is a distinct, plausible failure mode of FR-1 with no treatment. *Fix:* add a row (or fold into the existing Degradación de sync pattern with a note that it applies pre-login too).
- **medium** The onboarding banner ("Sin Snapshots todavía," line 68) is explicitly scoped to Hoy only (§ Component Patterns line 60: "Hoy, solo si el usuario no tiene ningún Snapshot todavía"). A brand-new UJ-3 user (Vale) who navigates directly to Por Estado or Tendencias before her first sync would hit whichever generic empty-state copy those surfaces use ("Nada en *planeado* por ahora" / a Tendencias-empty state) — which is misleading, since the real cause is "never synced," not "empty by choice." No state row addresses a Tendencias or Por Estado view with zero Snapshots ever recorded. *Fix:* either state that the onboarding banner is global (contradicting Component Patterns line 60) or define what Por Estado/Tendencias show pre-first-sync.

## 5. Visual reference coverage

`_bmad-output/planning-artifacts/ux-designs/ux-AnimeList-2026-08-13/mockups/`, `/wireframes/`, and `/imports/` do not exist — the directory contains only `DESIGN.md`, `EXPERIENCE.md`, and `.memlog.md`. Per task framing, this is expected at this stage (key-screen mocks are being rendered in a later pass) and is not treated as a finding. Noted for completeness: EXPERIENCE.md § Information Architecture (line 34) already flags this correctly with "→ Referencia de composición: pendiente de key-screen mocks."

## 6. Bloat & overspecification — strong

DESIGN.md's Colors/Typography/Shapes prose ties every claim to a usage decision (FR references, "nunca decorativo," explicit exclusions) rather than restating pixel values decoratively; where it does restate a token's raw value inline (e.g., "`rounded.md` (10px)"), that mirrors the shape examples' own convention and isn't bloat. EXPERIENCE.md prose stays behavioral throughout — no editorial voice leaking in, consistent with the rule that only DESIGN.md may carry that tone. § Inspiration & Anti-patterns' three "Rechazado" entries are each tied to a specific PRD citation (§6.2, §5), not decorative narrative. No section reads as pure source-restatement or as something a downstream consumer wouldn't read.

### Findings
- **low** `danger`/`danger-dark` and `info-foreground`/`info-foreground-dark` tokens are defined but unused (see Token completeness §2) — minor dead weight, flagged there to avoid duplication.

## 7. Inheritance discipline — adequate

`sources` frontmatter in EXPERIENCE.md (lines 6–8: `../../prds/prd-AnimeList-2026-08-13/prd.md`, `../../briefs/brief-AnimeList-2026-08-13/brief.md`) resolves correctly from the file's directory to the actual PRD and brief. UJ names are verbatim from PRD §2.3 (verified in §1 above). Glossary terms (Snapshot, Sincronización/Sync, Whitelist de Invitación) are used consistently. Spot-checked "Poster card," "Trend bar," and "Status badge" — all three are named identically everywhere they appear in both files.

### Findings
- **high** The onboarding banner component is named three different ways across the two files: DESIGN.md § Components calls it "**Onboarding banner**" (line 180, frontmatter key `onboarding-banner`); EXPERIENCE.md § Component Patterns calls it "MAL-Sync onboarding banner" (line 60); EXPERIENCE.md § State Patterns calls it "Banner de onboarding MAL-Sync" (line 68). A downstream consumer grepping for the component name by any single one of these strings misses the other two references. *Fix:* pick one name and use it verbatim in all three locations.
- **low** PRD glossary term "Estado de seguimiento" is used in full in EXPERIENCE.md § Information Architecture (line 28: "Estado de seguimiento de AniList") but shortened to "Estado de AniList" in § Component Patterns (line 57, Status badge row). Minor drift, unlikely to cause confusion given context, but inconsistent with the glossary-verbatim discipline applied elsewhere.

## 8. Shape fit — strong

DESIGN.md section order: Brand & Style → Colors → Typography → Layout & Spacing → Elevation & Depth → Shapes → Components → Do's and Don'ts — exact canonical order, nothing omitted or reordered. EXPERIENCE.md section order: Foundation → Information Architecture → Voice and Tone → Component Patterns → State Patterns → Interaction Primitives → Accessibility Floor → Responsive & Platform → Inspiration & Anti-patterns → Key Flows — matches the required-defaults order with both triggered sections (Responsive & Platform, Inspiration & Anti-patterns) correctly placed before Key Flows, which is last, exactly matching the shadcn/mobile example shape.

### Findings
- **low** EXPERIENCE.md frontmatter uses `title: AnimeTracker — Experience Spine` (line 2) where both shape examples (`experience-example-shadcn.md`, `experience-example-mobile.md`) use `name: <Product>` instead. Not necessarily wrong, but it's a schema deviation — if any downstream tooling keys off `name` in EXPERIENCE.md frontmatter (as it does for DESIGN.md), it won't find it. *Fix:* rename the key to `name` for consistency, or confirm `title` is an intentional, tooling-safe choice.

## Mechanical notes

- Both `sources` paths in EXPERIENCE.md frontmatter resolve to real files: `_bmad-output/planning-artifacts/prds/prd-AnimeList-2026-08-13/prd.md` and `_bmad-output/planning-artifacts/briefs/brief-AnimeList-2026-08-13/brief.md`.
- No broken `{path.to.token}` references found in either file — every cross-reference resolves.
- Name inconsistency: "Onboarding banner" / "MAL-Sync onboarding banner" / "Banner de onboarding MAL-Sync" (see §7).
- Glossary term shortening: "Estado de seguimiento" → "Estado de AniList" in one location (see §7).
- EXPERIENCE.md frontmatter uses `title` instead of the `name` key both shape examples use (see §8).
- DESIGN.md frontmatter correctly omits `sources` (not part of the DESIGN.md schema per `design-md-spec.md`).
