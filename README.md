# ND Alarm — Hệ Sinh Thái ND Labs (ndsite.web.app)

**ND Alarm** là ứng dụng báo thức thông minh đa nền tảng (Android Jetpack Compose + Web Portal Credential API + Firebase) với phong cách giao diện **Soft Sky Blue**, bo tròn pill-shaped mềm mại, hỗ trợ đầy đủ Lịch Âm - Dương Việt Nam và cơ chế chống ngủ quên Anti-Cheat bảo mật cao.

---

## 🌟 Tính Năng Nổi Bật

### 1. Phong Cách Thiết Kế (UI/UX)
- **Bảng màu chủ đạo**: Xanh dương nhạt (*Soft Sky Blue* `#0284C7`, `#E0F2FE`, `#F0F9FF`) kết hợp nền xám/trắng thanh lịch.
- **Pill-shaped & Bo tròn**: Tất cả các Cards, Buttons, Dialogs, Input Fields đều sử dụng bo góc lớn mềm mại.
- **Chuyển động mượt mà**: Hiệu ứng chuyển cảnh Tab, hiệu ứng nhịp thở (Pulse) khi chuông reo, ripple feedback.

### 2. Âm Lịch & Dương Lịch Song Hành
- Tích hợp thuật toán chuyển đổi Thiên văn Lịch Âm - Dương Việt Nam (GMT+7).
- Hỗ trợ chu kỳ báo thức định kỳ ngày **Mùng 1 (Sóc)** và **Rằm 15 (Vọng)** hàng tháng.
- Lưới lịch tương tác hiển thị song song ngày Dương và ngày Âm, Can Chi (Giáp Thìn, Ất Tỵ,...).

### 3. Hệ Thống Chống Ngủ Quên (Anti-Cheat & Bảo Mật)
- **Màn hình khóa Full-Screen Intent (`AlarmRingingActivity`)**: Hiển thị đè màn hình khóa với cờ `setShowWhenLocked(true)` và `setTurnScreenOn(true)`.
- **Mật khẩu 50 ký tự (Secret Token 50)**: Tạo ngẫu nhiên bằng `SecureRandom` với độ an toàn mật mã cao.
- **Tích hợp Android Autofill / iOS Keychain / Browser Credential Management API (`navigator.credentials.store`)**: Cho phép tự động lưu và điền mật khẩu 50 ký tự vào trình duyệt.
- **Mã PIN khẩn cấp 8 số**: Dùng để tắt chéo từ thiết bị khác khi chưa được cấp quyền Tin cậy.
- **Danh sách NDID Tin cậy (Trusted NDID)**: Bỏ qua mã PIN 8 số khi tắt từ thiết bị trong gia đình đã được phê duyệt.
- **Thử thách Toán học Tỉnh táo**: Đánh thức não bộ bằng phép tính ngẫu nhiên.

### 4. Dịch Vụ Nền Android & Khả Năng Khôi Phục
- **`AlarmScheduler`**: Lên lịch chính xác bằng `AlarmManager.setExactAndAllowWhileIdle()`.
- **`AlarmRingtoneService`**: Foreground Service đảm bảo phát chuông và rung liên tục mà không bị hệ điều hành đóng.
- **`SoundEngine`**: Bộ tổng hợp âm thanh đa dải tần (Gentle Breeze, Morning Ocean, Digital Pulse, Synth Chords).
- **`BootReceiver`**: Tự động khôi phục và lên lịch lại toàn bộ báo thức sau khi khởi động máy (`BOOT_COMPLETED`, `TIME_SET`).
- **Room Database**: Lưu trữ bền vững (`alarms`, `trusted_ndids`, `ringing_history`).

### 5. Web Portal (ndsite.web.app Integration)
- Nằm trong thư mục `web/` (`index.html`, `styles.css`, `app.js`, `firebase-rules.json`) và `app/src/main/assets/web/`.
- Tự động bắt tín hiệu chuông reo qua Firebase / Local Event và tự động mở Hộp thoại tắt chuông kèm Autofill Token 50.

---

## 🚀 Cấu Trúc Mã Nguồn

```
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt               # Điều hướng chính M3 Pill Navigation
│   │   │   ├── NDAlarmApp.kt                 # Khởi tạo Notification Channels & Room DB
│   │   │   ├── alarm/
│   │   │   │   ├── AlarmScheduler.kt         # Tính toán & Lên lịch AlarmManager
│   │   │   │   ├── AlarmReceiver.kt          # BroadcastReceiver kích hoạt chuông
│   │   │   │   ├── AlarmRingtoneService.kt   # Foreground Service phát chuông & rung
│   │   │   │   ├── AlarmRingingActivity.kt   # Màn hình Anti-Cheat Full-screen
│   │   │   │   ├── BootReceiver.kt           # Khôi phục báo thức sau reboot
│   │   │   │   └── SoundEngine.kt            # Bộ tổng hợp âm thanh đa tần
│   │   │   ├── data/
│   │   │   │   ├── dao/                      # AlarmDao, TrustedNdidDao, RingingHistoryDao
│   │   │   │   ├── model/                    # AlarmEntity, TrustedNdidEntity, RingingHistoryEntity
│   │   │   │   ├── database/                 # NDAlarmDatabase (Room)
│   │   │   │   └── repository/               # AlarmRepository
│   │   │   ├── ui/
│   │   │   │   ├── components/               # AlarmCard, AddEditAlarmDialog
│   │   │   │   ├── screens/                  # AlarmsScreen, LunarCalendarScreen, WebPortalSyncScreen, HistorySettingsScreen
│   │   │   │   ├── theme/                    # Color (Soft Sky Blue), Theme, Shapes, Type
│   │   │   │   └── viewmodel/                # AlarmViewModel
│   │   │   └── util/
│   │   │       ├── LunarCalendarHelper.kt    # Thuật toán chuyển đổi Âm - Dương lịch
│   │   │       └── SecurityHelper.kt         # Sinh & xác thực Token 50 / PIN 8
│   │   └── assets/web/                       # Web Portal Companion
│   └── src/test/java/com/example/            # Unit & Robolectric Tests
├── web/                                      # Web Portal độc lập (HTML5/CSS3/ES6/Firebase)
│   ├── index.html
│   ├── styles.css
│   ├── app.js
│   └── firebase-rules.json
└── metadata.json
```
