#!/usr/bin/env python3
"""Offline static checks for the Shooting Android project.

These are a supplement to (never a replacement for) `./gradlew lintDebug`,
`testDebugUnitTest` and `assembleDebug`. They run without an Android SDK and
catch the most common integration mistakes: missing resources, unresolved local
imports, composable misuse, destructive Room migrations and network leakage.

Usage: python3 scripts/check_sources.py
"""
from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MAIN = ROOT / "app/src/main"
JAVA = MAIN / "java"
RES = MAIN / "res"
errors: list[str] = []
warnings: list[str] = []


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))


kt_files = sorted(JAVA.rglob("*.kt"))
texts = {p: read(p) for p in kt_files}

# ---------------------------------------------------------------- resources --
string_names: set[str] = set()
for xml in sorted(RES.rglob("*.xml")):
    text = read(xml)
    names = re.findall(r'<string\s+name="([^"]+)"', text)
    repeated = [n for n, c in __import__("collections").Counter(names).items() if c > 1]
    for n in repeated:
        errors.append(f"duplicate <string name=\"{n}\"> in {rel(xml)}")
    string_names.update(names)

used_strings: dict[str, list[Path]] = defaultdict(list)
for p, text in texts.items():
    for m in re.finditer(r"\bR\.string\.(\w+)", text):
        used_strings[m.group(1)].append(p)
for name in sorted(set(used_strings) - string_names):
    errors.append(f"missing string resource R.string.{name} (used in {rel(used_strings[name][0])})")

# ------------------------------------------------------------------- imports --
FUN = re.compile(
    r"(?m)^(?:(?:@[^\n]*|private|internal|public|suspend|inline|abstract|open|override|operator)\s+)*"
    r"fun\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)\s*\("
)
TYPE = re.compile(
    r"(?m)^(?:(?:@[^\n]*|private|internal|public|abstract|open|sealed|data|enum|value)\s+)*"
    r"(?:data class|enum class|sealed class|sealed interface|class|interface|object|typealias|val|var)\s+"
    r"(?:[\w.]+\.)?(\w+)"
)
declared: set[str] = set()
for p, text in texts.items():
    pkg_m = re.search(r"^package\s+([\w.]+)", text, re.M)
    if not pkg_m:
        continue
    pkg = pkg_m.group(1)
    for regex in (FUN, TYPE):
        for m in regex.finditer(text):
            declared.add(f"{pkg}.{m.group(1)}")
    # extension functions with receivers: "fun Foo.bar("
    for m in re.finditer(r"(?m)^fun\s+[\w.<>, ?]+\.(\w+)\s*\(", text):
        declared.add(f"{pkg}.{m.group(1)}")
for p, text in texts.items():
    for m in re.finditer(r"^import\s+(ir\.hoboho\.shooting\.[\w.]+)", text, re.M):
        name = m.group(1)
        if name.startswith("ir.hoboho.shooting.R"):
            continue  # generated R class
        if name.startswith("ir.hoboho.shooting.BuildConfig"):
            continue  # generated BuildConfig class (AGP buildFeatures.buildConfig)
        if name not in declared:
            errors.append(f"unresolved local import {name} in {rel(p)}")

# --------------------------------------------------------------- composables --
FUN_HEAD = re.compile(
    r"(?m)^((?:@[^\n]*\n)*)(?:private |internal |public )?"
    r"fun\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)\s*\("
)
for p, text in texts.items():
    heads = list(FUN_HEAD.finditer(text))
    for i, m in enumerate(heads):
        end = heads[i + 1].start() if i + 1 < len(heads) else len(text)
        body = text[m.start():end]
        if "stringResource(" in body and "@Composable" not in m.group(1):
            errors.append(f"stringResource() inside non-composable {m.group(2)} in {rel(p)}")

# --------------------------------------------------------- hard business rules --
def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)

all_text = strip_comments("\n".join(texts.values()))
if re.search(r"\bfallbackToDestructiveMigration\s*\(", all_text):
    errors.append("destructive Room migration is forbidden")
if "android.permission.INTERNET" in read(MAIN / "AndroidManifest.xml"):
    updater = read(JAVA / "ir/hoboho/shooting/feature/settings/AppUpdater.kt")
    if "api.github.com/repos/hoboho/shooting/releases" not in updater:
        errors.append("network access must remain limited to the explicit GitHub updater")
if "LayoutDirection.Rtl" not in read(JAVA / "ir/hoboho/shooting/ui/theme/Theme.kt"):
    errors.append("RTL layout direction is not pinned in the theme")
entities = read(JAVA / "ir/hoboho/shooting/data/local/Entities.kt")
if not re.search(r"val amountToman:\s*Long", entities):
    errors.append("money must be stored as Long (integer Toman)")
if "Double" in entities or "Float" in entities:
    errors.append("floating-point fields found in the Room entities")

# Navigation destinations must have real composable screens (no placeholders).
nav = texts[JAVA / "ir/hoboho/shooting/ui/navigation/ShootingNavigation.kt"]
screen_calls = set(re.findall(r"\b(\w+Screen)\s*\(", nav))
screen_defs = set()
for p, text in texts.items():
    for m in re.finditer(r"(?m)^fun\s+(\w+Screen)\s*\(", text):
        prefix = text[max(0, m.start() - 240):m.start()]
        if "@Composable" in prefix:
            screen_defs.add(m.group(1))
missing_screens = sorted(screen_calls - screen_defs)
if missing_screens:
    errors.append(f"navigation references undefined screens: {missing_screens}")
for p, text in texts.items():
    if "TODO()" in text or "NotImplementedError" in text:
        errors.append(f"unfinished placeholder in {rel(p)}")


# --------------------------------------------------- local call signatures --
# Catches misspelled named arguments and missing required arguments on our own
# functions/classes (Compose trailing lambdas included). AndroidX APIs are
# intentionally out of scope here — the real compiler checks those.
def matching_paren(text: str, start: int) -> int:
    depth = 0
    quote: str | None = None
    i = start
    while i < len(text):
        c = text[i]
        if quote:
            if c == "\\":
                i += 2
                continue
            if c == quote:
                quote = None
        elif c in "\"'":
            quote = c
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def split_args(text: str) -> list[str]:
    parts: list[str] = []
    current = ""
    depth = 0
    quote: str | None = None
    i = 0
    while i < len(text):
        c = text[i]
        if quote:
            current += c
            if c == "\\" and i + 1 < len(text):
                current += text[i + 1]
                i += 2
                continue
            if c == quote:
                quote = None
        elif c in "\"'":
            quote = c
            current += c
        elif c in "([{":
            depth += 1
            current += c
        elif c in ")]}":
            depth -= 1
            current += c
        elif c == "," and depth == 0:
            parts.append(current.strip())
            current = ""
        else:
            current += c
        i += 1
    if current.strip():
        parts.append(current.strip())
    return parts


DECL = re.compile(
    r"(?m)^((?:@[^\n]*\n)*)(?:private |internal |public |data |enum |sealed |value |abstract |open )*"
    r"(?:fun|class)\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)\s*\("
)
declarations: dict[str, list[list[tuple[str, bool]]]] = defaultdict(list)
declaration_ranges: dict[Path, list[tuple[int, int]]] = defaultdict(list)
for path, text in texts.items():
    for m in DECL.finditer(text):
        open_at = text.find("(", m.end() - 1)
        close_at = matching_paren(text, open_at)
        if close_at < 0:
            continue
        params: list[tuple[str, bool]] = []
        for arg in split_args(text[open_at + 1:close_at]):
            head = arg.split(":", 1)[0].strip()
            name = re.search(r"(\w+)\s*$", head)
            if name:
                params.append((name.group(1), "=" in arg))
        declarations[m.group(2)].append(params)
        declaration_ranges[path].append((m.start(), m.end()))

CALL = re.compile(r"\b(\w+)\s*\(")
for path, text in texts.items():
    ranges = declaration_ranges[path]
    for m in CALL.finditer(text):
        name = m.group(1)
        if name not in declarations or name in ("if", "when", "for", "while", "return"):
            continue
        if any(a <= m.start() < b for a, b in ranges):
            continue
        open_at = text.find("(", m.end() - 1)
        close_at = matching_paren(text, open_at)
        if close_at < 0:
            continue
        args = split_args(text[open_at + 1:close_at])
        named: list[str] = []
        positional = 0
        for arg in args:
            nm = re.match(r"^(\w+)\s*=(?!=)", arg)
            if nm:
                named.append(nm.group(1))
            elif arg:
                positional += 1
        trailing = bool(re.match(r"\s*\{", text[close_at + 1:]))
        matched = False
        for params in declarations[name]:
            names = [n for n, _ in params]
            if any(n not in names for n in named):
                continue
            if positional > len(names):
                continue
            covered = set(named)
            remaining = [n for n in names if n not in covered]
            for k in range(min(positional, len(remaining))):
                covered.add(remaining[k])
            if trailing and names:
                covered.add(names[-1])
            if all(has_default or n in covered for n, has_default in params):
                matched = True
                break
        if not matched:
            line = text[:m.start()].count("\n") + 1
            errors.append(
                f"invalid call to {name}() at {rel(path)}:{line} "
                f"(named={named}, positional={positional}, trailing={trailing})"
            )

# ------------------------------------------------------------------- report --
print(f"Checked {len(kt_files)} Kotlin sources, {len(string_names)} string resources, "
      f"{len(screen_calls)} navigation destinations.")
for w in warnings:
    print(f"WARN  {w}")
for e in errors:
    print(f"ERROR {e}")
print(f"Static checks: {len(errors)} error(s), {len(warnings)} warning(s)")
sys.exit(1 if errors else 0)
