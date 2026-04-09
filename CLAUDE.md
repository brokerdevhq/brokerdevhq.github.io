# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

BrokerDev marketing website — a static site deployed to GitHub Pages at brokerdev.ca.

Built with a Clojure Polylith static site generator. Top namespace: `ca.brokerdev`.

## Development Environment

Uses `shell.nix` + direnv (`use nix` in `.envrc`).

```bash
direnv allow   # load the Nix shell automatically
```

## Common Commands

```bash
clj -M:dev:nrepl                               # Start nREPL server
clj -M:dev -m ca.brokerdev.generate-site.core  # Generate site to public/
poly info                                       # Show workspace structure
poly test                                       # Run tests
poly check                                      # Check workspace
```

From the REPL (load `development/src/brokerdev.clj`):

```clojure
(require 'brokerdev)
(generator/generate-site {})
```

## Architecture

Polylith workspace with:

- **`components/`** — `markdown-parser`, `html-renderer`, `template`, `file-utils`, `site-generator`
- **`bases/generate-site`** — CLI entry point
- **`content/`** — Markdown source files with EDN frontmatter
- **`static/`** — CSS, JS, images, CNAME
- **`public/`** — Generated output (gitignored)

## Content

Markdown files use EDN frontmatter:

```markdown
{:title "Page Title"
 :description "Meta description"
 :date #inst "2025-12-01"
 :layout "post"}   ; or "page"

# Content here
```

Pages: `index.md`, `services.md`, `about.md`, `posts.md`
Blog posts: `content/posts/*.md`

## Deployment

GitHub Actions (`.github/workflows/deploy.yml`) builds and deploys to GitHub Pages on push to `main`.
