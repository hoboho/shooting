#!/usr/bin/env python3
"""Generates a temporary `R` stub from the real string resources.

Only used by the offline Kotlin compile check; the Android build uses the real
generated `R` class. Keeping it generated means new strings never drift.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
out = Path(sys.argv[2] if len(sys.argv) > 2 else "/tmp/shooting-r-stub").resolve()
strings = (root / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
names = sorted(set(re.findall(r'<string\s+name="([^"]+)"', strings)))
lines = ["package ir.hoboho.shooting", "", "object R {", "    object string {"]
lines += [f"        const val {n}: Int = {1000 + i}" for i, n in enumerate(names)]
lines += [
    "    }",
    "    object color { val purple_200: Int = 0 }",
    "    object font { val vazirmatn_regular: Int = 0 }",
    "    object drawable { val ic_launcher_foreground: Int = 0 }",
    "    object xml { val file_paths: Int = 0 }",
    "}",
]
target = out / "ir/hoboho/shooting/R.kt"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"generated {len(names)} R.string constants at {target}")
