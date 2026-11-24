#!/usr/bin/env python3
"""LeDor: пересылает новые отзывы (app.feedback, notified = false) в Telegram и отмечает отправленные.

Запускается таймером ledor-feedback-notify.timer от пользователя postgres (доступ к БД без пароля).
Токен бота и chat_id — в /etc/ledor/telegram.env. Пока они не заполнены, скрипт ничего не делает.
"""
import json
import os
import subprocess
import sys
import urllib.parse
import urllib.request

TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN", "").strip()
CHAT = os.environ.get("TELEGRAM_CHAT_ID", "").strip()
DB = os.environ.get("LEDOR_DB", "ledor")


def psql(sql: str) -> str:
    return subprocess.run(["psql", "-X", "-q", "-At", "-v", "ON_ERROR_STOP=1", "-d", DB, "-c", sql],
                          check=True, capture_output=True, text=True).stdout.strip()


def send(text: str) -> None:
    data = urllib.parse.urlencode({"chat_id": CHAT, "text": text, "disable_web_page_preview": "true"}).encode()
    with urllib.request.urlopen(f"https://api.telegram.org/bot{TOKEN}/sendMessage", data, timeout=20) as r:
        if json.load(r).get("ok") is not True:
            raise RuntimeError("Telegram не принял сообщение")


def main() -> int:
    if not TOKEN or not CHAT:
        return 0
    raw = psql("select coalesce(json_agg(f order by f.id), '[]') from (select id, created_at, message, contact, "
               "app_version, device, os_version, locale from app.feedback where not notified order by id limit 20) f")
    for f in json.loads(raw):
        meta = " · ".join(x for x in (f.get("app_version"), f.get("device"), f.get("os_version"), f.get("locale")) if x)
        lines = [f"Отзыв LeDor #{f['id']}", f["message"]]
        if f.get("contact"):
            lines.append(f"Контакт: {f['contact']}")
        if meta:
            lines.append(meta)
        send("\n\n".join(lines)[:4000])
        psql(f"update app.feedback set notified = true where id = {int(f['id'])}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
