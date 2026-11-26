#!/usr/bin/env python3
"""Переносит строки Android (android/app/src/main/res/values*/strings.xml) в String Catalog iOS
(ios/LeDor/Resources/Localizable.xcstrings) и добавляет строки только для iOS из tools/strings-ios.json.
Ключи — имена Android; массивы — <имя>.<индекс>; форматы %s/%d → %@/%ld; непереводимые строки (translatable="false")
берутся из values/ и одинаковы во всех языках. Запуск: python3 ios/tools/strings.py"""
import json, re, sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "android/app/src/main/res"
OUT = ROOT / "ios/LeDor/Resources/Localizable.xcstrings"
LANGS = {"en": "values", "ru": "values-ru", "he": "values-iw", "yi": "values-ji", "fr": "values-fr", "es": "values-es"}
# Только для Android: батарея, точные будильники, подсказки производителей, каналы уведомлений, лицензии Java-библиотек,
# проверка обновлений (на iOS обновления приходят через App Store и TestFlight, D-56).
SKIP = re.compile(r"^(health_(banner|exact|bg|battery|no_reminders|disable|sheet|open_setting)\w*|hint_(samsung|xiaomi|huawei|oppo|vivo)"
                  r"|channel_\w+|update_\w+|licenses_text|feedback_meta_detail|set_health_warnings|locale_tag)$")


def unescape(s):
    s = re.sub(r"\\(.)", lambda m: {"n": "\n", "t": "\t"}.get(m.group(1), m.group(1)), s)
    s = re.sub(r"%(\d+\$)?s", lambda m: "%" + (m.group(1) or "") + "@", s)
    return re.sub(r"%(\d+\$)?(0?\d*)d", lambda m: "%" + (m.group(1) or "") + m.group(2) + "ld", s)


def text(el):
    return "".join(el.itertext())


def read(folder):
    strings, plurals = {}, {}
    for el in ET.parse(RES / folder / "strings.xml").getroot():
        name = el.get("name")
        if el.tag == "string" and el.get("translatable") != "false" and not SKIP.match(name):
            strings[name] = unescape(text(el))
        elif el.tag == "string-array":
            for i, item in enumerate(el.findall("item")):
                strings[f"{name}.{i}"] = unescape(text(item))
        elif el.tag == "plurals":
            plurals[name] = {item.get("quantity"): unescape(text(item)) for item in el.findall("item")}
    return strings, plurals


def unit(value):
    return {"stringUnit": {"state": "translated", "value": value}}


def main():
    data = {lang: read(folder) for lang, folder in LANGS.items()}
    fixed = {el.get("name"): unescape(text(el)) for el in ET.parse(RES / "values/strings.xml").getroot()
             if el.tag == "string" and el.get("translatable") == "false" and not SKIP.match(el.get("name"))}
    for strings, _ in data.values():
        strings.update(fixed)
    keys = sorted(set(data["en"][0]) | set(data["en"][1]))
    out = {"sourceLanguage": "en", "version": "1.0", "strings": {}}
    for key in keys:
        locs = {}
        for lang, (strings, plurals) in data.items():
            if key in strings:
                locs[lang] = unit(strings[key])
            elif key in plurals:
                locs[lang] = {"variations": {"plural": {q: unit(v) for q, v in plurals[key].items()}}}
            else:
                sys.exit(f"нет перевода {key} для {lang}")
        out["strings"][key] = {"extractionState": "manual", "localizations": locs}
    extra = json.loads((ROOT / "ios/tools/strings-ios.json").read_text())
    for key, langs in extra.items():
        if key.startswith("_"):
            continue
        missing = set(LANGS) - set(langs)
        if missing:
            sys.exit(f"{key}: нет языков {missing}")
        out["strings"][key] = {"extractionState": "manual",
                               "localizations": {lang: unit(unescape(v)) for lang, v in langs.items()}}
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2, sort_keys=True) + "\n")
    print(f"{len(out['strings'])} строк → {OUT.relative_to(ROOT)}")


main()
