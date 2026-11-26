import Domain

/// Значения окна «Изменить ПИН-коды» (B-40), как `ChangePinsState` на Android. ПИН-код для чтения можно сделать простым
/// (например, «0770» для общины), Мастер-ПИН — 6–12 цифр и не равен ему. Если Мастер-ПИН на телефоне не сохранён,
/// окно спрашивает текущий отдельно.
public struct ChangePinsForm: Sendable, Equatable {
    public var read: String
    public var master: String
    public var current: String
    public let masterStored: Bool

    public init(readPin: String, masterPin: String, masterStored: Bool) {
        read = readPin
        master = masterPin
        current = masterStored ? masterPin : ""
        self.masterStored = masterStored
    }

    public var readOk: Bool { PinRules.isValidReadPin(read) }
    public var masterOk: Bool { PinRules.isValidMasterPin(master) && master != read }
    public var canSave: Bool { readOk && masterOk && PinRules.isValidMasterPin(current) }
}
