# Goza

Goza is the dispatcher and optional persistence layer for this personality collection.
Its portable dispatcher is `goza/SKILL.md`; it is separate from the selectable personality
profiles.

The selectable personalities live separately under `skills/`:

```text
skills/
├── nationalities/
│   ├── cubano/
│   │   └── SKILL.md
│   └── peruano/
│       └── SKILL.md
└── people/
    └── kirito/
        └── SKILL.md
```

Use the dispatcher to select one:

```text
/goza cubano
```

Goza loads the selected `SKILL.md` profiles and leaves the underlying task, tools, and
technical reasoning unchanged. Agent-specific command and hook files remain outside
the personality directories.

Supported forms:

```text
/goza kirito
/goza peruano
/goza kirito peruano
```

The two-profile form composes one person layer with one nationality layer. The person
controls temperament; the nationality controls language and local register.

## Adapter Runtime

The Goza command definitions are Markdown files, not JavaScript. Claude's optional hooks
are JavaScript because Claude executes them directly with Node and the adapter has no
build step or runtime dependency. They can be rewritten in TypeScript, but that would
require a TypeScript runtime or a compile step and would make installation less portable.
