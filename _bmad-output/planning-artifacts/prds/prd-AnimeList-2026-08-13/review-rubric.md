# PRD Quality Review — AnimeTracker (prd-AnimeList-2026-08-13)

## Overall verdict

This PRD is well-earned for its stakes: the vision is specific rather than templated, trade-offs are named honestly (SM-C1 counter-metric, `[ASSUMPTION]`/`[NOTE FOR PM]` tags at real tensions), and 8 of 9 FRs have genuinely testable consequences. It is decision-ready for an architecture/epics pass with only minor cleanup. The main risks are a success metric (SM-2) with no target and not tracked as an open question, one FR (FR-6) whose acceptance criterion is stated as an adjective rather than a bound, an in-scope deliverable (MAL-Sync setup docs) with no FR ID or acceptance criteria, and a broken cross-reference to a section that doesn't exist in the document.

## Decision-readiness — strong

Trade-offs are surfaced rather than smoothed. SM-C1 explicitly names what's given up ("no se debe maximizar la frecuencia de Sync para inflar SM-3 a costa de acercarse a los rate limits de la API de AniList") instead of treating sync frequency as unambiguously good. The two `[NOTE FOR PM]` callouts (§4.1 on whether Whitelist needs an admin UI; §6.2 on deferring streak detection) sit at genuine unresolved tensions, not safe checkpoints. Open Questions (§10) are actually open — OQ-1 and OQ-2 have working assumptions but still ask the real question ("¿alcanza con...?", "¿cuál es el intervalo exacto...?") rather than answering themselves in the next sentence. §7's preamble ("no se definieron targets numéricos más duros en esta conversación") is an honest admission rather than a dodge.

No findings.

## Substance over theater — strong

Three UJs (Mica, Diego, invited user), each tied to a specific FR — not persona theater. Vision (§1) is specific to this product's narrow bet ("No es un tracker de anime desde cero") and would not swap into another PRD unchanged. NFRs in §8 carry actual numbers (~100-1000 users, ~30-60 min staleness) rather than boilerplate "scalable/secure/reliable" language. No differentiation section exists purely because a template expects one.

No findings.

## Strategic coherence — strong

The thesis is stated once and features follow from it: AnimeTracker is "la capa de visualización" over AniList/MAL-Sync data, not a tracker in its own right (§1). FR-3 (Dashboard "Hoy") and FR-5 (Tendencias) are the two features that directly deliver the two things AniList doesn't ("dashboard más directo" and "una serie de tiempo... que AniList no ofrece"); auth (FR-1/2) and sync (FR-6–9) are supporting plumbing. SM-2 avoids the DAU/MAU vanity-metric tell by measuring return-to-anchor-feature specifically. SM-C1 counter-metric is present and substantive.

### Findings
- **medium** SM-2 has no numeric target and isn't tracked as an open question (§7) — "los usuarios activos vuelven al Dashboard 'Hoy' con cierta regularidad (**frecuencia exacta a definir**)" is the only primary metric left fully undefined, and unlike the PRD's other deferred decisions it has no `[ASSUMPTION]` tag and no corresponding entry in §10 Open Questions. It will surface again in epics/stories with no home to resolve it. *Fix:* either add an `[ASSUMPTION: ...]` placeholder target (e.g., "N logins/week") or add it to §10 as OQ-4.

## Done-ness clarity — adequate

Most FRs are genuinely unforgiving-review-proof: FR-3's empty-state handling, FR-5's "no Snapshots = missing data, not false zero," and FR-9's explicit "datos de [fecha]" messaging are all concrete, verifiable conditions an engineer could write a test against. This is the PRD's strongest dimension in terms of effort invested.

### Findings
- **medium** FR-6's rate-limit consequence is stated as an adjective, not a bound (§4.5) — "El job respeta los rate limits de la API de AniList (no dispara una consulta por usuario en paralelo **sin control de tasa**)" gives no concurrency cap or requests/sec figure to test against. Contrast with FR-9's precise UI-text requirement a few lines later. *Fix:* either state a concrete bound now or explicitly defer it with an `[ASSUMPTION]`/OQ tag pointing to technical design (it's currently neither testable nor flagged as pending).
- **medium** MAL-Sync setup documentation is in MVP scope with no FR ID or acceptance criteria (§6.1) — "Documentación de setup de MAL-Sync para que un usuario invitado sepa instalarlo..." is listed as in-scope for V1 alongside FR-1–FR-9, but unlike them it has no testable consequence, so an engineer/writer has no way to know what "done" looks like for it. *Fix:* either fold it under FR-1/FR-2 with an explicit consequence, or give it its own FR with a testable condition (e.g., "a new invited user can complete MAL-Sync setup using only this doc, unassisted").

## Scope honesty — strong

§5 No-Objetivos does real work (7 concrete exclusions, each naming what's given up, e.g., "No hay escritura/edición hacia AniList... eso se sigue haciendo en AniList o vía MAL-Sync"). All 5 inline `[ASSUMPTION]` tags round-trip cleanly into the §11 index (verified: §4.1, §4.5, and the three in §8 all appear, and no index entry lacks an inline tag). De-scoping is proposed honestly rather than silently assumed — §6.2 explicitly calls out streak/alert detection as a Brief-mentioned pain point deliberately not committed to V1. Given the stated low stakes (personal/invite-only, ~100-1000 users), the open-items density (4 OQs + 5 assumptions + 2 NOTE FOR PM across a 9-FR PRD) is appropriate, not alarming.

No findings.

## Downstream usability — adequate

This is a chain-top PRD (§0 states it feeds `bmad-architecture` and `bmad-create-epics-and-stories` directly), so this dimension matters more than it would for a standalone PRD. Glossary (§3) covers the load-bearing nouns (AniList, MAL-Sync, Estado de seguimiento, Snapshot, Sincronización, Whitelist de Invitación, Dashboard "Hoy") and FR/UJ/SM IDs are contiguous and cross-references resolve (every "Realiza UJ-X" and "Valida FR-X" points to a real ID; no floating UJs — all three are referenced by at least one FR).

### Findings
- **medium** Broken cross-reference to a nonexistent section (§8) — "consistente con el frontend server-rendered elegido, **§Notas de Arquitectura**" points to a section that does not exist anywhere in this document. The actual stack decision lives in §9 Integraciones y Dependencias ("Stack de backend — Java Spring Boot... frontend server-rendered (Thymeleaf)"). A downstream architecture reader following this pointer will find nothing. *Fix:* change the reference to "§9" or remove it.

## Shape fit — strong

Single-role consumer product with meaningful UX (a dashboard people check daily) — three named-protagonist UJs is proportionate, not over-formalized for a ~100-1000-user invite-only tool, and not under-formalized (a UX-driven product with zero UJs would be a red flag; this PRD avoids that). SMs are appropriately a mix of adoption and behavioral-return rather than forcing enterprise-style OKR apparatus this project doesn't need. Rigor is calibrated correctly to the stated stakes throughout — no compliance/rollout/stakeholder-signoff apparatus was force-fitted in, consistent with the hobby/invite-only shape.

No findings.

## Mechanical notes

- **low** Glossary drift on the anchor feature's name — §3 defines the term as `Dashboard "Hoy"`, but §4.2's heading and §1's Vision text both call it `dashboard "hoy / seguí viendo"`. Not confusing in context, but a downstream reader doing literal term-matching against the Glossary would miss the variant. *Fix:* align the glossary entry or the feature heading to one canonical form.
- **low** Fourth Open Question is unlabeled — §10 numbers three questions as OQ-1/OQ-2/OQ-3 but the "Hosting" question has no ID, breaking the otherwise-clean ID scheme other sections rely on for cross-reference. *Fix:* label it OQ-4.
- Assumptions Index roundtrip: clean — all 5 inline `[ASSUMPTION]` tags (§4.1, §4.5, and three in §8) are indexed in §11, and no index entry lacks an inline source.
- ID continuity: FR-1 through FR-9 contiguous with no gaps/duplicates; UJ-1–3 and SM-1–3+SM-C1 likewise clean.
- UJ protagonist naming: all three UJs (Mica, Diego, "una nueva usuaria invitada") carry a named or clearly-scoped protagonist inline — no floating UJs.
