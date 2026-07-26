---
name: Solo Leveling Dungeon Builder Studio
description: A precise blueprint workspace for authoring, simulating, and exporting Minecraft dungeons.
colors:
  system-cyan: "#3FC6FF"
  system-cyan-dim: "#2273A8"
  system-cyan-soft: "#183548"
  void-panel: "#060A16"
  panel-raised: "#0C1423"
  text-main: "#E8F6FF"
  text-subdued: "#8FB8D8"
  status-success: "#5FE38C"
  status-warning: "#FFD166"
  status-error: "#FF5D6C"
typography:
  title:
    fontFamily: "Minecraft, sans-serif"
    fontSize: "10px"
    fontWeight: 400
    lineHeight: 1.2
  body:
    fontFamily: "Minecraft, sans-serif"
    fontSize: "9px"
    fontWeight: 400
    lineHeight: 1.35
  label:
    fontFamily: "Minecraft, sans-serif"
    fontSize: "8px"
    fontWeight: 400
    lineHeight: 1.25
rounded:
  none: "0px"
spacing:
  xs: "2px"
  sm: "4px"
  md: "8px"
  lg: "12px"
components:
  button-primary:
    backgroundColor: "{colors.system-cyan-dim}"
    textColor: "{colors.text-main}"
    rounded: "{rounded.none}"
    padding: "4px 8px"
  button-secondary:
    backgroundColor: "{colors.panel-raised}"
    textColor: "{colors.system-cyan}"
    rounded: "{rounded.none}"
    padding: "4px 8px"
  panel:
    backgroundColor: "{colors.void-panel}"
    textColor: "{colors.text-main}"
    rounded: "{rounded.none}"
    padding: "8px"
---

# Design System: Solo Leveling Dungeon Builder Studio

## Overview

**Creative North Star: "The Operator Blueprint"**

The Studio is a dense but calm technical workspace inside the existing Solo Leveling System. Its atmosphere comes from restrained cyan line work, dark layered surfaces, and a measured blueprint grid—not from ornamental framing that competes with the task. The interface should disappear behind the creator's room, graph, and validation work.

Structure is responsive, not merely scaled. The room list and inspector flank the main canvas on wide screens; the inspector becomes a switchable drawer when logical width is limited. Every dense region scrolls independently, every clipped value has a tooltip, and the central canvas always retains a useful minimum area.

It explicitly rejects command-heavy workflows, decorative game menus that sacrifice usable space, unexplained icons, color-only status, overlapping labels, and simulations that disagree with runtime generation.

**Key Characteristics:**

- Dense, legible, and task-first.
- Blueprint geometry over decorative texture.
- One consistent component vocabulary.
- State is visible, labeled, and actionable.
- Motion communicates state and never delays editing.

## Colors

The palette is a restrained cold-blue operator console with semantic colors reserved for validation.

### Primary

- **System Cyan:** Primary actions, selected tabs, focus outlines, sockets, and active canvas geometry.
- **Deep Console Cyan:** Resting button fills and secondary emphasis that must not compete with selected state.
- **Blueprint Wash:** Subtle selected rows and canvas guides; never used behind long prose.

### Neutral

- **Void Panel:** Main background and deepest canvas surface.
- **Raised Console:** Lists, inspectors, fields, and nested panels.
- **Frost Text:** Primary labels and values.
- **Muted Telemetry:** Supporting copy, metadata, and inactive labels.

### Tertiary

- **Pass Green:** Complete and valid states.
- **Action Amber:** Incomplete but recoverable work and unsaved snapshots.
- **Fault Red:** Blocking validation and destructive actions.

**The Semantic Rarity Rule.** Green, amber, and red appear only for status and consequences; they are never decoration.

**The Two-Signal Rule.** Every colored status also has a word, symbol, or shape.

## Typography

**Display Font:** Minecraft UI font
**Body Font:** Minecraft UI font
**Label/Mono Font:** Minecraft UI font

**Character:** One familiar in-game typeface keeps the Studio native and predictable. Hierarchy comes from placement, contrast, and measured spacing—not oversized text.

### Hierarchy

- **Title** (regular, 10px, 1.2): Screen title and major workspace context.
- **Body** (regular, 9px, 1.35): Values, instructions, diagnostics, and button labels.
- **Label** (regular, 8px, 1.25): Field labels, tabs, chips, and compact metadata.

**The No-Overlap Rule.** A label may wrap, scroll, or truncate with a tooltip; it may never collide with another value or control.

## Elevation

The Studio is flat by default. Depth is communicated with nested tonal surfaces, one-pixel borders, dividers, and selection fills. It does not use conventional drop shadows; the existing contained void background and restrained cyan edge glow provide atmosphere without reducing clarity.

**The Structural Depth Rule.** If a surface has no distinct navigation, scrolling, or inspection purpose, it does not earn another layer.

## Components

### Buttons

- **Shape:** Square operator controls (0px radius) with one-pixel borders.
- **Primary:** Deep cyan fill, frost text, compact 4px by 8px padding.
- **Hover / Focus:** Brighter cyan border and clearly visible focus outline; active state darkens briefly.
- **Secondary / Ghost:** Raised-console fill or transparent fill with cyan text. Destructive actions use fault red only after selection makes the target explicit.

### Chips

- **Style:** Compact bordered labels for roles, ranks, socket types, and required/optional state.
- **State:** Selected chips use blueprint wash plus a bright border; unselected chips retain readable muted text.

### Cards / Containers

- **Corner Style:** Square.
- **Background:** Void panel for the workspace and raised console for nested content.
- **Shadow Strategy:** No drop shadows; use tonal layering.
- **Border:** One-pixel dim cyan or low-contrast neutral divider.
- **Internal Padding:** 8px normally, 4px for dense rows.

### Inputs / Fields

- **Style:** Dark filled field, one-pixel subdued border, persistent label above or beside the value.
- **Focus:** System-cyan border plus visible caret/focus outline.
- **Error / Disabled:** Fault-red border with adjacent error text; disabled controls remain readable and explain their prerequisite in a tooltip.

### Navigation

Tabs use equal-height rectangular targets with a textual label and optional compact count. The active tab uses bright cyan text and a bottom rule. On narrow layouts, secondary panels switch through labeled tabs or drawers instead of shrinking text.

### Blueprint Canvas

Rooms are filled footprints with labeled IDs; sockets use facing arrows, anchors use role-specific shapes, and connections use orthogonal lines. Zoom centers on the pointer, panning never changes selection, and a floor selector prevents vertically separated rooms from appearing as false collisions.

## Do's and Don'ts

### Do:

- **Do** preserve at least 16 logical pixels for interactive row height and at least 120 by 100 logical pixels for the canvas.
- **Do** pair status colors with PASS, TODO, or ERROR labels and a distinct symbol.
- **Do** keep the current room, dungeon, revision, and validation summary visible.
- **Do** use inline inspectors and progressive disclosure before introducing a modal.
- **Do** make simulation and runtime generation consume the same canonical planner.

### Don't:

- **Don't** recreate a command-heavy workflow inside a collection of unlabeled icon buttons.
- **Don't** use decorative game menus that sacrifice usable space or text clarity for atmosphere.
- **Don't** use unexplained icons, color-only status, overlapping labels, or clipped configuration values without a tooltip.
- **Don't** animate decorative entrances or delay access to the editing task.
- **Don't** let live block edits silently alter a captured room snapshot.
- **Don't** show a simulation produced by rules that differ from real dungeon generation.
