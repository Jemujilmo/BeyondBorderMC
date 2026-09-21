#!/usr/bin/env python3
"""
Finds hard-coded world-size limits in the Yarn-mapped Minecraft jar that Loom downloads while building.

Writes two text files to ./inspection/:
  limits-report.txt  every numeric constant in a "world-size" range, with the class and method it sits in
  class-dumps.txt    bytecode of the handful of classes the mod cares about most

Run from the project root after `./gradlew build`. Set MC_JAR to skip the jar search (used for testing).
"""
import glob
import os
import re
import subprocess
import sys
import zipfile

MARKER = "net/minecraft/world/chunk/AbstractChunkHolder.class"
OUT_DIR = "inspection"

# (low, high, label): any numeric constant inside a range is reported
RANGES = [
    (1_800_000, 1_950_000, "chunk-scale (~30,000,000 / 16)"),
    (29_000_000, 31_000_000, "block-scale (~30,000,000)"),
    (59_000_000, 61_000_000, "border-scale (~60,000,000)"),
    (33_000_000, 34_000_000, "2^25 (33,554,432)"),
    (67_000_000, 68_000_000, "2^26 (67,108,864)"),
]

DUMP_CLASSES = [
    "net.minecraft.util.math.ChunkPos",
    "net.minecraft.util.math.ChunkSectionPos",
    "net.minecraft.util.collection.LinkedBlockPosHashSet$Storage",
]

NUM = re.compile(r"//\s*(int|long|float|double)\s+(-?[0-9.eE+\-]+)[dflDFL]?\s*$")
STATIC_FINAL = re.compile(r"^\s+(?:\w+\s+)*static\s+final\s+(int|long|float|double)\s+(\w+)\s*=\s*(-?[0-9.eE+\-]+)[dflDFL]?;")
CLASS_LINE = re.compile(r"^(?:\w+\s+)*?(?:class|interface|enum)\s+([\w.$]+)")
METHOD_LINE = re.compile(r"^  \S.*\);$|^  static \{\};$")
INSN = re.compile(r"^\s+\d+:\s+(\w+)\s*(.*)$")


def find_jar():
    override = os.environ.get("MC_JAR")
    if override:
        return override
    roots = [os.path.expanduser("~/.gradle"), os.getcwd(), os.path.expanduser("~/.cache")]
    seen = set()
    for root in roots:
        for path in glob.glob(os.path.join(root, "**", "*.jar"), recursive=True):
            if path in seen or "sources" in path or "minecraft" not in os.path.basename(path).lower():
                continue
            seen.add(path)
            try:
                with zipfile.ZipFile(path) as z:
                    if MARKER in z.namelist():
                        return path
            except (zipfile.BadZipFile, OSError):
                continue
    return None


def classes_in(jar):
    with zipfile.ZipFile(jar) as z:
        names = [n for n in z.namelist() if n.startswith("net/minecraft/") and n.endswith(".class")]
    return [n[:-6].replace("/", ".") for n in names]


def javap(jar, class_names):
    cmd = ["javap", "-c", "-p", "-constants", "-classpath", jar] + class_names
    return subprocess.run(cmd, capture_output=True, text=True, errors="replace").stdout


def in_range(value):
    for low, high, label in RANGES:
        if low <= abs(value) <= high:
            return label
    return None


def main():
    # Always leave a report behind, even if something below goes wrong.
    os.makedirs(OUT_DIR, exist_ok=True)
    try:
        return run()
    except Exception:
        import traceback
        with open(os.path.join(OUT_DIR, "limits-report.txt"), "w") as f:
            f.write("The scan crashed:\n" + traceback.format_exc())
        print("scan crashed; see inspection/limits-report.txt")
        return 0


def run():
    jar = find_jar()
    if not jar:
        with open(os.path.join(OUT_DIR, "limits-report.txt"), "w") as f:
            f.write("Could not find the mapped Minecraft jar (looked for %s).\n" % MARKER)
        print("no mapped jar found")
        return 0
    print("using", jar)

    names = classes_in(jar)
    hits = []
    strings = []
    cur_class = cur_method = None

    for i in range(0, len(names), 250):
        out = javap(jar, names[i:i + 250])
        for line in out.splitlines():
            m = CLASS_LINE.match(line)
            if m and not line.startswith(" "):
                cur_class, cur_method = m.group(1), None
                continue
            if METHOD_LINE.match(line):
                cur_method = line.strip()
                continue
            sf = STATIC_FINAL.match(line)
            if sf:
                try:
                    label = in_range(float(sf.group(3)))
                except ValueError:
                    label = None
                if label:
                    hits.append((label, cur_class, "field " + sf.group(2), "%s %s" % (sf.group(1), sf.group(3))))
                continue
            im = INSN.match(line)
            if not im:
                continue
            op, rest = im.group(1), im.group(2)
            value = None
            if op in ("bipush", "sipush"):
                try:
                    value = float(rest.split()[0])
                except (ValueError, IndexError):
                    pass
            else:
                nm = NUM.search(line)
                if nm:
                    try:
                        value = float(nm.group(2))
                    except ValueError:
                        pass
            if value is not None:
                label = in_range(value)
                if label:
                    hits.append((label, cur_class, cur_method or "?", "%s %s" % (op, value)))
            if "reasonable bounds" in line or "out of reasonable" in line:
                strings.append((cur_class, cur_method or "?", line.strip()))

    with open(os.path.join(OUT_DIR, "limits-report.txt"), "w") as f:
        f.write("jar: %s\nclasses scanned: %d\n\n" % (jar, len(names)))
        f.write("=== 'reasonable bounds' strings (only found if the message is not built by string concatenation) ===\n")
        for c, m, l in strings:
            f.write("%s\n    in %s\n    %s\n" % (c, m, l))
        f.write("\n=== constants in world-size ranges (grouped) ===\n")
        for label in sorted({h[0] for h in hits}):
            f.write("\n--- %s ---\n" % label)
            for _, c, m, v in sorted(h for h in hits if h[0] == label):
                f.write("%s\n    in %s\n    %s\n" % (c, m, v))

    with open(os.path.join(OUT_DIR, "class-dumps.txt"), "w") as f:
        # whole small classes
        f.write(javap(jar, DUMP_CLASSES))
        # just the constructor of AbstractChunkHolder (the class that crashed)
        text = javap(jar, ["net.minecraft.world.chunk.AbstractChunkHolder"])
        f.write("\n\n===== AbstractChunkHolder constructor =====\n")
        block, keep = [], False
        for line in text.splitlines():
            if re.match(r"^  \S.*AbstractChunkHolder\(.*\);$", line):
                keep = True
            elif keep and line.strip() == "":
                break
            if keep:
                block.append(line)
        f.write("\n".join(block) + "\n")
        # World's validity helpers
        text = javap(jar, ["net.minecraft.world.World"])
        f.write("\n\n===== World.isValidHorizontally / isValid =====\n")
        keep = False
        for line in text.splitlines():
            if re.match(r"^  \S.*(isValidHorizontally|isInvalidVertically|isValid)\(.*\);$", line):
                keep = True
            elif keep and line.strip() == "":
                keep = False
                f.write("\n")
            if keep:
                f.write(line + "\n")

    print("hits:", len(hits), "strings:", len(strings))
    return 0


if __name__ == "__main__":
    sys.exit(main())
