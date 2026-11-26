import AppCore
import Domain
import SwiftUI

/// Название города на языке интерфейса.
func cityName(_ c: City) -> String {
    switch L.langTag {
    case "ru": c.nameRu
    case "he", "yi": c.nameHe
    default: c.nameEn
    }
}

/// «До захода / После захода» со значками солнца и луны.
struct SunsetToggle: View {
    @Binding var afterSunset: Bool
    var body: some View {
        Picker("", selection: $afterSunset) {
            Text(L.t("before_sunset")).tag(false)
            Text(L.t("after_sunset")).tag(true)
        }
        .pickerStyle(.segmented)
    }
}

/// Подсказка «до или после захода» (B-24): по местному времени и городу ставит переключатель сама; в сумерках — только предупреждает.
struct SunsetHintView: View {
    @Environment(AppModel.self) private var model
    let date: CivilDate?
    let onResult: (Bool) -> Void
    @State private var open = false
    @State private var time: Date?
    @State private var cityId: String?
    @State private var query = ""
    @State private var picking = false

    var body: some View {
        if !open {
            Button(L.t("sunset_hint_open")) { open = true }.font(Typography.rubik(14))
        } else {
            let city = model.catalog.byId(cityId ?? model.settings.cityId)
            let tod = time.map { Zoned.timeOfDay($0, .current) }
            let hint = date.flatMap { d in tod.flatMap { t in city.flatMap { SunsetHints.evaluate(date: d, time: t, city: $0) } } }
            let complete = date != nil && tod != nil && city != nil
            let f = Formatters()
            VStack(alignment: .leading, spacing: 8) {
                DatePicker(tod.map { L.f("sunset_hint_time", f.time($0)) } ?? L.t("sunset_hint_set_time"),
                           selection: Binding(get: { time ?? Zoned.instant(model.today, .noon, .current) }, set: { time = $0 }),
                           displayedComponents: .hourAndMinute)
                Button(city.map { L.f("sunset_hint_city", cityName($0)) } ?? L.t("sunset_hint_set_city")) { picking.toggle() }
                if picking {
                    TextField(L.t("city_search"), text: $query).textFieldStyle(.roundedBorder)
                    ForEach(model.catalog.search(query).prefix(5)) { c in
                        Button(cityName(c)) { cityId = c.id; picking = false; query = "" }.foregroundStyle(Palette.onSurface)
                    }
                }
                if complete && hint == nil {
                    Text(L.t("sunset_hint_none")).font(Typography.rubik(13)).foregroundStyle(Palette.error)
                } else if let hint, let tod {
                    let t = f.time(tod)
                    Text(L.f("sunset_hint_sunset", f.time(hint.sunset))).font(Typography.rubik(13))
                    switch hint.verdict {
                    case .before: Text(L.f("sunset_hint_before", t)).font(Typography.rubik(14))
                    case .after: Text(L.f("sunset_hint_after", t)).font(Typography.rubik(14))
                    case .twilight: Text(L.f("sunset_hint_twilight", t, f.time(hint.nightfall))).font(Typography.rubik(14)).foregroundStyle(Palette.error)
                    }
                }
                Text(L.t("sunset_hint_local")).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
            }
            .onChange(of: hint) { _, h in
                switch h?.verdict {
                case .before: onResult(false)
                case .after: onResult(true)
                default: break
                }
            }
        }
    }
}
