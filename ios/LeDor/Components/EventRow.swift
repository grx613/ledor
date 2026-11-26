import AppCore
import Domain
import SwiftUI

/// Значок типа события в цветном кружке.
struct EventTypeIcon: View {
    let type: EventType
    var size: CGFloat = 40
    var body: some View {
        let color = Palette.forType(type)
        Circle().fill(color.opacity(0.16)).frame(width: size, height: size)
            .overlay(EventTypeGlyph(type: type, color: color).frame(width: size * 0.6, height: size * 0.6))
    }
}

/// Значок события из встроенного списка праздников.
struct HolidayIcon: View {
    var size: CGFloat = 40
    var body: some View {
        Circle().fill(Palette.gold.opacity(0.16)).frame(width: size, height: size)
            .overlay(MagenDavidShape().stroke(Palette.gold, style: StrokeStyle(lineWidth: 1.8, lineJoin: .round))
                .frame(width: size * 0.55, height: size * 0.55))
    }
}

/// ✡ — отмечается по еврейскому календарю, ○ — по григорианскому.
struct MarkObserve: View {
    let kind: CalendarKind
    var body: some View {
        Group {
            if kind == .hebrew {
                MagenDavidShape().stroke(Palette.gold, style: StrokeStyle(lineWidth: 1.2, lineJoin: .round))
            } else {
                Circle().stroke(Palette.observeHint, lineWidth: 1.2).padding(1.4)
            }
        }
        .frame(width: 12, height: 12)
        .accessibilityHidden(true)
    }
}

/// Цветная метка списка.
struct ListBadge: View {
    let name: String
    let colorIndex: Int
    var body: some View {
        HStack(spacing: 5) {
            Circle().fill(Palette.listColor(colorIndex)).frame(width: 8, height: 8)
            Text(name).font(Typography.rubik(12, .medium)).foregroundStyle(Palette.onSurfaceVariant).lineLimit(1)
        }
    }
}

/// Строка события: значок, имя, тип, даты в двух календарях, «через N дней», возраст, метка списка.
struct EventRow: View {
    @Environment(AppModel.self) private var model
    let event: Event
    let occurrence: Occurrence?
    let daysUntil: Int?
    var primary: CalendarKind = .hebrew
    var showList = true

    var body: some View {
        let f = Formatters()
        let typeLabel = f.typeLabel(event, occurrence?.yearsSince)
        // Главная дата крупно, тот же день в другом календаре — мелко (D-54).
        let dates = occurrence.map { f.occurrenceDates($0, first: primary) } ?? []
        let first = dates.first
        let second = dates.count > 1 ? dates[1] : nil
        let age = f.agePhrase(event.type, occurrence?.yearsSince)
        let isHoliday = event.listId == holidaysListId
        let holidayLine = isHoliday ? HolidayEvents.rowLine(event) : nil
        let list = model.list(event.listId)
        let rel: String? = daysUntil.map { d in
            // Пост «с рассвета» накануне вечером ещё не начинается.
            let fromEve = occurrence?.startsEveBefore == true && HolidayEvents.holidayOf(event.id)?.dawn != true
            return d == 1 && fromEve ? L.t("rel_tomorrow_eve") : f.relativeDays(d)
        }
        HStack(alignment: .center, spacing: 12) {
            if isHoliday { HolidayIcon() } else { EventTypeIcon(type: event.type) }
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(event.title).font(Typography.rubik(16, .medium)).foregroundStyle(Palette.onSurface).lineLimit(1)
                    if model.isPending(event.id) {
                        Image(systemName: "clock").font(.system(size: 11)).foregroundStyle(Palette.onSurfaceVariant)
                            .accessibilityLabel(L.t("pending_upload"))
                    }
                }
                Text([typeLabel, age].compactMap { $0 }.joined(separator: " · "))
                    .font(Typography.rubik(12)).foregroundStyle(Palette.forType(event.type)).lineLimit(1)
                if let first {
                    HStack(spacing: 4) {
                        MarkObserve(kind: occurrence?.calendar ?? event.observeBy)
                        Text(first).font(Typography.rubik(14)).foregroundStyle(Palette.onSurface).lineLimit(1)
                    }
                    if let second { Text(second).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant).lineLimit(1) }
                }
                if let holidayLine {
                    Text(holidayLine).font(Typography.rubik(12, .medium)).foregroundStyle(Palette.primary)
                }
                if showList, let list, list.id != localListId { ListBadge(name: f.listName(list), colorIndex: list.colorIndex) }
            }
            Spacer(minLength: 0)
            if let rel {
                let soon = (daysUntil ?? 99) <= 1
                Text(rel).font(Typography.rubik(12, soon ? .bold : .medium))
                    .foregroundStyle(soon ? Palette.secondary : Palette.primary)
                    .multilineTextAlignment(.trailing).lineLimit(3).fixedSize(horizontal: false, vertical: true).frame(maxWidth: 112, alignment: .trailing)
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

/// Золотая карточка результата.
struct HighlightCard<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 4) { content }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Palette.secondaryContainer, in: RoundedRectangle(cornerRadius: 14))
            .foregroundStyle(Palette.onSecondaryContainer)
    }
}

/// Пустое состояние с действиями.
struct EmptyState<Actions: View>: View {
    let text: String
    @ViewBuilder let actions: Actions
    var body: some View {
        VStack(spacing: 14) {
            MagenDavidShape().stroke(Palette.gold, style: StrokeStyle(lineWidth: 2, lineJoin: .round)).frame(width: 56, height: 56)
            Text(text).font(Typography.rubik(15)).multilineTextAlignment(.center).foregroundStyle(Palette.onSurfaceVariant)
            actions
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
    }
}

extension View {
    /// Основной шрифт приложения для экранов на системных компонентах.
    func ledorScreen() -> some View {
        self.font(Typography.rubik(16)).tint(Palette.primary)
    }
}
