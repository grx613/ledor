import Foundation

/// Не чаще раза в столько секунд синхронизировать при открытии приложения (B-39, как `SYNC_ON_OPEN_MIN_MS` в Android).
public let syncOnOpenMinInterval: TimeInterval = 5 * 60

/// Синхронизировать ли при открытии приложения: сервер настроен и с прошлой попытки прошло больше `syncOnOpenMinInterval`.
public func shouldSyncOnOpen(configured: Bool, now: Date, lastAttempt: Date?) -> Bool {
    guard configured else { return false }
    guard let lastAttempt else { return true }
    return now.timeIntervalSince(lastAttempt) > syncOnOpenMinInterval
}
