#!/bin/sh
# Ждёт новую записку от второй сессии (docs/sync/README.md).
#
#   tools/sync/watch.sh to-server.md     # сессия на сервере
#   tools/sync/watch.sh to-mac.md        # сессия на Mac
#
# Раз в минуту делает `git fetch` и сравнивает входящий файл в origin/main с уже прочитанным (тем, что в HEAD).
# Нашёл отличие — печатает новые строки и завершается с кодом 0: для сессии это сигнал «пришла почта».
# Рабочую копию не трогает. Запускать в фоне; после обработки записки (git pull) запустить снова.
# SYNC_INTERVAL — пауза в секундах (по умолчанию 60). Если fetch не проходит 30 раз подряд — код 1.
set -u
cd "$(dirname "$0")/../.." || exit 2
file="docs/sync/${1:?укажите входящий файл: to-server.md или to-mac.md}"
interval="${SYNC_INTERVAL:-60}"
blob() { git rev-parse -q --verify "$1:$file" 2>/dev/null || echo none; }

fails=0
while :; do
  if git fetch -q origin main 2>/dev/null; then
    fails=0
    if [ "$(blob origin/main)" != "$(blob HEAD)" ]; then
      echo "Новая почта: $file изменился в origin/main ($(git rev-parse --short origin/main))."
      git diff --no-color HEAD origin/main -- "$file" | grep '^+' | grep -v '^+++' | cut -c2- | head -80
      exit 0
    fi
  else
    fails=$((fails + 1))
    if [ "$fails" -ge 30 ]; then
      echo "Почта не проверяется: git fetch не проходит $fails раз подряд."
      exit 1
    fi
  fi
  sleep "$interval"
done
