import AppCore
import Domain
import SwiftUI
import UIKit

func rpcErrorText<T>(_ r: RpcResult<T>) -> String {
    switch r {
    case .ok: return ""
    case .failure(let kind):
        switch kind {
        case .network: return L.t("err_network")
        case .server: return L.t("err_server")
        case .notConfigured: return L.t("err_cloud_not_configured")
        }
    case .error(let code, let attempts, let retry):
        let lockedMinutes = ((retry ?? 900) + 59) / 60
        switch code {
        case "NAME_TAKEN": return L.t("err_name_taken")
        case "NOT_FOUND": return L.t("err_not_found")
        case "WRONG_PIN": return (attempts ?? 1) > 0 ? L.f("err_wrong_pin", attempts ?? 0) : L.f("err_locked", lockedMinutes)
        case "LOCKED": return L.f("err_locked", lockedMinutes)
        case "RATE_LIMITED": return L.t("err_rate_limited")
        case "INVALID_NAME": return L.t("err_invalid_name")
        case "INVALID_PIN", "PINS_EQUAL": return L.t("err_invalid_pin")
        case "FORBIDDEN": return L.t("err_forbidden")
        case "UNAUTHORIZED": return L.t("err_unauthorized")
        case "LIST_DELETED": return L.t("err_list_deleted")
        default: return L.t("err_server")
        }
    }
}

private func roleText(_ l: ListRecord) -> String {
    if l.id == localListId { return L.t("role_personal") }
    if l.state == .accessLost { return L.t("role_no_access") }
    return L.t(l.role == .editor ? "role_editor" : "role_reader")
}

/// Поле ПИН-кода: только цифры, скрытие, дополнительная кнопка справа.
struct PinField<Trailing: View>: View {
    let label: String
    @Binding var value: String
    let maxLen: Int
    var visibleInitially = true
    var readOnly = false
    @ViewBuilder var trailing: Trailing
    @State private var visible: Bool?

    var body: some View {
        let shown = visible ?? visibleInitially
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
                if readOnly {
                    Text(shown ? value : String(repeating: "•", count: value.count)).font(Typography.rubik(18, .medium)).monospacedDigit()
                } else if shown {
                    TextField("", text: Binding(get: { value }, set: { value = String($0.filter(\.isNumber).prefix(maxLen)) }))
                        .keyboardType(.numberPad).font(Typography.rubik(18, .medium)).monospacedDigit()
                } else {
                    SecureField("", text: Binding(get: { value }, set: { value = String($0.filter(\.isNumber).prefix(maxLen)) }))
                        .keyboardType(.numberPad)
                }
            }
            Button { visible = !shown } label: { Image(systemName: shown ? "eye.slash" : "eye") }
                .buttonStyle(.borderless).accessibilityLabel(L.t(shown ? "hide" : "show"))
            trailing
        }
    }
}

/// Управление списками: личный, общие, встроенный список праздников.
struct ListsScreen: View {
    @Environment(AppModel.self) private var model
    @State private var toast: String?

    var body: some View {
        @Bindable var m = model
        let f = Formatters()
        List {
            Section {
                NavigationLink(value: Route.listCreate(moveLocal: false)) { Label(L.t("create_list"), systemImage: "plus") }
                NavigationLink(value: Route.listJoin(nil)) { Label(L.t("join_list"), systemImage: "person.badge.plus") }
                if !model.data.cloudConfigured { HighlightCard { Text(L.t("err_cloud_not_configured")) } }
            }
            Section {
                ForEach(model.lists) { l in
                    let count = model.db.count(in: l.id)
                    HStack(spacing: 12) {
                        NavigationLink(value: Route.listDetail(l.id)) {
                            HStack(spacing: 12) {
                                Image(systemName: l.isCloud ? "cloud" : "iphone").foregroundStyle(Palette.listColor(l.colorIndex))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(f.listName(l)).font(Typography.rubik(16, .medium))
                                    Text("\(roleText(l)) · \(L.p("events_count", count))").font(Typography.rubik(12))
                                        .foregroundStyle(l.state == .accessLost ? Palette.error : Palette.onSurfaceVariant)
                                    if let at = l.lastSyncAt {
                                        Text(L.f("synced_at", f.dateTime(at))).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Toggle("", isOn: Binding(get: { l.notifyEnabled }, set: { v in Task { await model.data.setNotifyEnabled(listId: l.id, v) } }))
                            .labelsHidden().fixedSize()
                    }
                }
                let h = model.settings.holidays
                HStack(spacing: 12) {
                    NavigationLink(value: Route.holidays) {
                        HStack(spacing: 12) {
                            MagenDavidShape().stroke(Palette.gold, lineWidth: 1.8).frame(width: 22, height: 22)
                            subtitled(L.t("holidays_title"), L.t("holidays_sub"))
                        }
                    }
                    // Главный выключатель списка: выключен — праздников нет ни в календаре, ни в событиях, ни в напоминаниях (D-53).
                    Toggle("", isOn: $m.settings.holidays.enabled).labelsHidden().fixedSize()
                }
            } footer: {
                Text(L.t("lists_hint_notify") + " " + L.t("lists_hint_holidays") + (model.lists.contains(where: \.isCloud) ? " " + L.t("pull_to_sync") : ""))
            }
        }
        .refreshable {
            let r = await model.data.syncAll()
            if r.networkError { toast = L.t("err_network") }
        }
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t("manage_lists"))
        .navigationBarTitleDisplayMode(.inline)
        .toast($toast)
        .ledorScreen()
    }
}

struct ListCreateScreen: View {
    @Environment(AppModel.self) private var model
    let moveLocal: Bool
    @Binding var path: [Route]
    @State private var loaded = false
    @State private var name = ""
    @State private var readPin = PinGenerator.generate(length: 6)
    @State private var masterPin = PinGenerator.generate(length: 8)
    @State private var move = false
    @State private var busy = false
    @State private var error: String?
    @State private var nameFree: Bool?

    var body: some View {
        let localCount = model.db.count(in: localListId)
        let nameOk = ListNameRules.isValid(name)
        let readOk = PinRules.isValidReadPin(readPin)
        let masterOk = PinRules.isValidMasterPin(masterPin) && masterPin != readPin
        Form {
            if !model.data.cloudConfigured { Section { Text(L.t("err_cloud_not_configured")) } }
            Section {
                TextField(L.t("list_name"), text: Binding(get: { name }, set: { name = String($0.prefix(ListNameRules.max)) }))
            } footer: {
                if !name.isEmpty && !nameOk { Text(L.t("err_invalid_name")) }
                else if nameFree == true { Text(L.t("name_free")).foregroundStyle(Palette.success) }
                else if nameFree == false { Text(L.t("err_name_taken")).foregroundStyle(Palette.error) }
            }
            Section {
                PinField(label: L.t("read_pin"), value: $readPin, maxLen: 8) {
                    Button(L.t("regenerate")) { readPin = PinGenerator.generate(length: 6) }.buttonStyle(.borderless)
                }
                if !readOk { Text(L.t("read_pin_hint")).font(Typography.rubik(12)).foregroundStyle(Palette.error) }
                PinField(label: L.t("master_pin"), value: $masterPin, maxLen: 12) {
                    Button(L.t("regenerate")) { masterPin = PinGenerator.generate(length: 8) }.buttonStyle(.borderless)
                }
                Text(L.t("master_pin_hint")).font(Typography.rubik(12)).foregroundStyle(masterOk ? Palette.onSurfaceVariant : Palette.error)
            } footer: { Text(L.t("pins_explain")) }
            if localCount > 0 {
                Section { Toggle(isOn: $move) { subtitled(L.t("move_local_events"), L.p("events_count", localCount)) } }
            }
            if let error { Section { Text(error).foregroundStyle(Palette.error) } }
            Section {
                Button {
                    busy = true
                    error = nil
                    Task {
                        let r = await model.data.create(name: name, readPin: readPin, masterPin: masterPin, moveLocalEvents: move)
                        // Как на Android: после создания — экран нового списка.
                        if case .ok(let list) = r { replaceTop(&path, with: .listDetail(list.id)) } else { error = rpcErrorText(r) }
                        busy = false
                    }
                } label: { if busy { ProgressView() } else { Text(L.t("create")).fontWeight(.semibold) } }
                .disabled(busy || !nameOk || !readOk || !masterOk || nameFree == false)
            }
        }
        .onAppear { if !loaded { loaded = true; move = moveLocal } }
        .task(id: name) {
            nameFree = nil
            guard ListNameRules.isValid(name), model.data.cloudConfigured else { return }
            try? await Task.sleep(for: .milliseconds(500))
            if !Task.isCancelled, case .ok(let exists) = await model.data.checkExists(name: name) { nameFree = !exists }
        }
        .navigationTitle(L.t("create_list"))
        .navigationBarTitleDisplayMode(.inline)
        .ledorScreen()
    }
}

struct ListJoinScreen: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    let initialName: String?
    @State private var name = ""
    @State private var found = false
    @State private var pin = ""
    @State private var busy = false
    @State private var error: String?
    @State private var loaded = false
    @State private var imported: String?

    var body: some View {
        Form {
            if !model.data.cloudConfigured { Section { Text(L.t("err_cloud_not_configured")) } }
            Section {
                HStack {
                    TextField(L.t("list_name"), text: Binding(get: { name }, set: { name = String($0.prefix(ListNameRules.max)); found = false; error = nil }))
                    if found { Image(systemName: "checkmark.circle.fill").foregroundStyle(Palette.success) }
                }
            } header: { Text(L.t("join_explain")).textCase(nil) }
            if found {
                Section {
                    PinField(label: L.t("pin_code"), value: $pin, maxLen: 12, visibleInitially: false) { EmptyView() }
                } footer: { Text(L.t("pin_any_hint")) }
            }
            if let error { Section { Text(error).foregroundStyle(Palette.error) } }
            Section {
                if !found {
                    Button { find() } label: { if busy { ProgressView() } else { Text(L.t("find")).fontWeight(.semibold) } }
                        .disabled(busy || !ListNameRules.isValid(name))
                } else {
                    Button { join() } label: { if busy { ProgressView() } else { Text(L.t("connect")).fontWeight(.semibold) } }
                        .disabled(busy || !PinRules.isValidAnyPin(pin))
                }
            }
        }
        .navigationTitle(L.t("join_list"))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if !loaded { loaded = true; name = initialName ?? "" } }
        .alert(imported ?? "", isPresented: Binding(get: { imported != nil }, set: { if !$0 { imported = nil; dismiss() } })) {
            Button(L.t("ok")) { imported = nil; dismiss() }
        }
        .ledorScreen()
    }

    private func find() {
        busy = true
        error = nil
        Task {
            let r = await model.data.checkExists(name: name)
            if case .ok(let exists) = r { if exists { found = true } else { error = L.t("err_not_found") } } else { error = rpcErrorText(r) }
            busy = false
        }
    }

    private func join() {
        busy = true
        error = nil
        Task {
            let r = await model.data.join(name: name, pin: pin)
            if case .ok(.joined(_, let n)) = r { imported = L.f("imported_n", n) } else { error = rpcErrorText(r) }
            busy = false
        }
    }
}

func shareText(listName: String, pin: String) -> String {
    L.f("share_message", listName, "LeDor", AppConfig.appLink, listName, pin)
}

struct ListDetailScreen: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    let id: String
    @Binding var path: [Route]
    @State private var confirmDisconnect = false
    @State private var deleteDialog = false
    @State private var deletePin = ""
    @State private var deleteError: String?
    @State private var shareMaster = false
    @State private var masterWarn = false
    @State private var syncing = false
    @State private var toast: String?

    var body: some View {
        if let l = model.list(id) { content(l) } else { Color.clear.onAppear { dismiss() } }
    }

    @ViewBuilder
    private func content(_ l: ListRecord) -> some View {
        let f = Formatters()
        let count = model.db.count(in: l.id)
        Form {
            Section {
                HStack(spacing: 10) {
                    Image(systemName: l.isCloud ? "cloud" : "iphone").foregroundStyle(Palette.listColor(l.colorIndex))
                    subtitled(roleText(l), L.p("events_count", count))
                }
                Toggle(L.t("list_notifications"), isOn: Binding(get: { l.notifyEnabled },
                                                               set: { v in Task { await model.data.setNotifyEnabled(listId: l.id, v) } }))
                if l.state == .accessLost {
                    Label(L.t("err_unauthorized"), systemImage: "exclamationmark.triangle").foregroundStyle(Palette.error)
                }
            }
            if !l.isCloud {
                Section { Text(L.t("personal_list_explain")).foregroundStyle(Palette.onSurfaceVariant) }
                let targets = model.lists.filter { $0.isCloud && $0.canEdit }
                // B-27: кнопка есть всегда, когда в личном списке есть события. Облачных списков нет — сразу «Создать список»
                // с включённым переносом; есть — выбор списка или новый; после переноса открывается экран списка.
                if count > 0 {
                    Section {
                        if targets.isEmpty {
                            Button(L.t("move_to_cloud")) { path.append(.listCreate(moveLocal: true)) }
                        } else {
                            Menu(L.t("move_to_cloud")) {
                                ForEach(targets) { t in
                                    Button(t.name) {
                                        Task {
                                            await model.data.moveLocalEvents(to: t.id)
                                            replaceTop(&path, with: .listDetail(t.id))
                                        }
                                    }
                                }
                                Button { path.append(.listCreate(moveLocal: true)) } label: { Label(L.t("create_list"), systemImage: "plus") }
                            }
                        }
                    }
                }
            } else {
                Section(L.t("pin_codes")) {
                    if let pin = l.readPin { pinRow(L.t("read_pin"), pin) }
                    if let pin = l.masterPin { pinRow(L.t("master_pin"), pin) }
                    if l.role == .reader { Text(L.t("reader_hint")).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant) }
                }
                Section(L.t("share")) {
                    if l.masterPin != nil {
                        Toggle(L.t("share_edit_rights"), isOn: Binding(get: { shareMaster }, set: { v in if v { masterWarn = true } else { shareMaster = false } }))
                    }
                    if let pin = shareMaster ? l.masterPin : l.readPin {
                        let text = shareText(listName: l.name, pin: pin)
                        HStack {
                            Button("Telegram") { open("https://t.me/share/url?url=\(enc(AppConfig.appLink))&text=\(enc(text))") }
                                .buttonStyle(.bordered).frame(maxWidth: .infinity)
                            Button("WhatsApp") { open("https://wa.me/?text=\(enc(text))") }.buttonStyle(.bordered).frame(maxWidth: .infinity)
                        }
                        ShareLink(item: text) { Label(L.t("share_other"), systemImage: "square.and.arrow.up") }
                    }
                }
                Section {
                    Button {
                        syncing = true
                        Task {
                            let r = await model.data.syncList(l.id)
                            syncing = false
                            toast = L.t(r.ok > 0 ? "synced" : r.networkError ? "err_network" : "err_server")
                        }
                    } label: { Label(L.t("sync_now"), systemImage: "arrow.triangle.2.circlepath") }
                    .disabled(syncing)
                    if l.role == .reader {
                        NavigationLink(value: Route.listJoin(l.name)) { Text(L.t("enter_master_pin")).foregroundStyle(Palette.primary) }
                    }
                    Button(L.t("disconnect_list")) { confirmDisconnect = true }
                    if l.role == .editor {
                        Button(L.t("delete_from_cloud"), role: .destructive) { deletePin = l.masterPin ?? ""; deleteError = nil; deleteDialog = true }
                    }
                    NavigationLink(value: Route.feedback(L.f("report_subject", l.name))) { Text(L.t("report_list")).foregroundStyle(Palette.primary) }
                }
            }
        }
        .navigationTitle(f.listName(l))
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(L.t("disconnect_list"), isPresented: $confirmDisconnect, titleVisibility: .visible) {
            Button(L.t("disconnect"), role: .destructive) { Task { await model.data.disconnect(listId: l.id) } }
        } message: { Text(L.t("disconnect_text")) }
        .alert(L.t("share_edit_rights"), isPresented: $masterWarn) {
            Button(L.t("ok")) { shareMaster = true }
            Button(L.t("cancel"), role: .cancel) {}
        } message: { Text(L.t("master_share_warning")) }
        .alert(L.t("delete_from_cloud"), isPresented: $deleteDialog) {
            SecureField(L.t("master_pin"), text: $deletePin).keyboardType(.numberPad)
            Button(L.t("delete"), role: .destructive) {
                Task {
                    let r = await model.data.deleteFromCloud(listId: l.id, masterPin: deletePin)
                    if case .ok = r { return }
                    deleteError = rpcErrorText(r)
                    toast = deleteError
                }
            }
            Button(L.t("cancel"), role: .cancel) {}
        } message: { Text(L.t("delete_cloud_text")) }
        .toast($toast)
        .ledorScreen()
    }

    private func pinRow(_ label: String, _ pin: String) -> some View {
        PinField(label: label, value: .constant(pin), maxLen: 12, visibleInitially: false, readOnly: true) {
            Button { UIPasteboard.general.string = pin; toast = L.t("copied") } label: { Image(systemName: "doc.on.doc") }
                .buttonStyle(.borderless).accessibilityLabel(L.t("copy"))
        }
    }

    private func enc(_ s: String) -> String { s.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? s }

    private func open(_ url: String) { if let u = URL(string: url) { openURL(u) } }
}

extension View {
    /// Короткое сообщение внизу экрана.
    func toast(_ text: Binding<String?>) -> some View {
        overlay(alignment: .bottom) {
            if let t = text.wrappedValue {
                Text(t).font(Typography.rubik(14)).padding(.horizontal, 16).padding(.vertical, 10)
                    .background(.thinMaterial, in: Capsule()).padding(.bottom, 24)
                    .task(id: t) { try? await Task.sleep(for: .seconds(2)); text.wrappedValue = nil }
            }
        }
    }
}

/// Заменить текущий экран другим (как `popUpTo` на Android): назад — к предыдущему.
func replaceTop(_ path: inout [Route], with route: Route) {
    if !path.isEmpty { path.removeLast() }
    path.append(route)
}
