"""Genera las notas de version de AdMobKMP a partir de los commits del release.

Lo usa `.github/workflows/release.yml`, y es el mismo archivo que se puede ejecutar en
local para ver lo que se va a publicar:

    python .github/scripts/release_notes.py
    python .github/scripts/release_notes.py 1.0.0..HEAD

Sale del de ListaCompra, con dos cambios: los tags van sin `v` (JitPack usa el nombre
del tag como version), y en vez de la tabla de versionCode y build de iOS, que en una
libreria no existen, las notas acaban con como instalar esa version exacta.
"""

import os
import re
import subprocess
import sys

# Prefijo de conventional commit -> titulo de seccion. `chore` no esta a proposito:
# "subir version a 1.0.1" no es una nota de version, el numero ya sale en el titulo.
SECTIONS = [
    ("feat", "Novedades"),
    ("fix", "Arreglos"),
    ("perf", "Rendimiento"),
    ("refactor", "Interno"),
    ("docs", "Documentación"),
]

CONVENTIONAL = re.compile(r"(\w+)(\([^)]*\))?(!?):\s*(.+)")


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args],
        capture_output=True,
        text=True,
        check=True,
        encoding="utf-8",
        errors="replace",
    ).stdout.strip()


def read(path: str) -> str:
    try:
        with open(path, encoding="utf-8") as handle:
            return handle.read()
    except OSError:
        return ""


def previous_tag(new: str) -> str:
    """El tag de version mas alto que no sea el que se esta publicando."""
    for tag in git("tag", "--sort=-v:refname").splitlines():
        if re.match(r"^\d+\.\d+", tag) and tag != new:
            return tag
    return ""


def build_notes(rng: str, new: str, prev: str, repo: str) -> str:
    buckets: dict[str, list[str]] = {key: [] for key, _ in SECTIONS}
    breaking: list[str] = []
    others: list[str] = []
    seen: set[str] = set()

    for line in git("log", "--no-merges", "--format=%s", rng).splitlines():
        subject = line.strip()
        if not subject or subject.lower() in seen:
            continue
        seen.add(subject.lower())

        match = CONVENTIONAL.match(subject)
        if not match:
            # En este repo no todos los commits siguen conventional commits (el primero
            # es "New Admob library"). Mejor listarlos que publicar unas notas vacias.
            others.append(subject)
            continue
        kind, _, bang, message = match.groups()
        entry = message.strip()
        entry = entry[0].upper() + entry[1:]
        if bang:
            breaking.append(entry)
        if kind in buckets:
            buckets[kind].append(entry)

    out: list[str] = []

    if breaking:
        out.append("### ⚠️ Cambios que rompen compatibilidad\n")
        out += [f"- {e}" for e in breaking]
        out.append("")

    for key, title in SECTIONS:
        if buckets[key]:
            out.append(f"### {title}\n")
            out += [f"- {e}" for e in buckets[key]]
            out.append("")

    if others:
        out.append("### Otros cambios\n")
        out += [f"- {e}" for e in others]
        out.append("")

    if not out:
        out += [f"- Version {new}", ""]

    owner, _, name = repo.partition("/")
    out += [
        "---",
        "",
        "### Instalación",
        "",
        "```toml",
        f'admob-kmp = "{new}"',
        f'bonygod-admobkmp = {{ module = "com.github.{owner}.{name}:admob-kmp", version.ref = "admob-kmp" }}',
        "```",
        "",
        f"Swift Package Manager: `https://github.com/{repo}`, versión `{new}`, producto `AdMobKMPSwift`.",
    ]

    if prev:
        out += ["", f"**Full Changelog**: https://github.com/{repo}/compare/{prev}...{new}"]

    return "\n".join(out)


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")

    gradle = read("admob-kmp/build.gradle.kts")
    if not gradle:
        raise SystemExit("Ejecuta esto desde la raiz del repo: no veo admob-kmp/build.gradle.kts")

    match = re.search(r'^version\s*=\s*"([^"]+)"', gradle, re.MULTILINE)
    new = os.environ.get("NEW_VERSION") or (match.group(1) if match else "")
    repo = os.environ.get("REPO") or "BonyGoD/AdMobKMP"

    if len(sys.argv) > 1:
        rng = sys.argv[1]
        prev = rng.split("..")[0]
    else:
        prev = previous_tag(new)
        rng = f"{prev}..HEAD" if prev else "HEAD"
        print(f"# rango: {rng}", file=sys.stderr)

    print(build_notes(rng, new, prev, repo))


if __name__ == "__main__":
    main()
