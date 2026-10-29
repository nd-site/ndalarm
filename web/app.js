/**
 * ND Alarm Web Portal — Client Logic & Firebase / Credential Management Integration
 * Ecosystem: ND Labs (ndsite.web.app)
 */

// Application State
const appState = {
  clockId: "ND-CL01",
  myNdid: "ND-USER-01",
  ownerNdid: "ND-MASTER",
  secretToken50: "Nd9$kQx7#mP2vL8*zY4@wR1!bT5^hJ9~cF3&gK6_dE0+aU4-sM7=",
  emergencyPin8: "49201847",
  status: "idle", // "idle" | "ringing" | "unlocked"
  trustedNdids: ["ND-MASTER", "ND-FAMILY-01"],
  alarmLabel: "Báo thức ND Labs",
  alarmTime: "07:00"
};

// Initialize on DOM Ready
document.addEventListener("DOMContentLoaded", () => {
  initClockUI();
  initAutoRefreshTime();
  checkBrowserCredentialSupport();
});

function initClockUI() {
  document.getElementById("clock-id-input").value = appState.clockId;
  document.getElementById("my-ndid-input").value = appState.myNdid;
  document.getElementById("display-token-50").innerText = appState.secretToken50;
  document.getElementById("active-clock-id").innerText = appState.clockId;
  document.getElementById("active-owner-ndid").innerText = appState.ownerNdid;
  document.getElementById("active-pin-8").innerText = appState.emergencyPin8;
  document.getElementById("live-time").innerText = appState.alarmTime;
  document.getElementById("live-alarm-label").innerText = appState.alarmLabel;
}

function initAutoRefreshTime() {
  setInterval(() => {
    if (appState.status === "idle") {
      const now = new Date();
      const hh = String(now.getHours()).padStart(2, "0");
      const mm = String(now.getMinutes()).padStart(2, "0");
      document.getElementById("live-time").innerText = `${hh}:${mm}`;
    }
  }, 1000);
}

// 1. LINK CLOCK ID & TRIGGER CREDENTIAL PROMPT
function handleLinkClock(event) {
  event.preventDefault();
  const inputClockId = document.getElementById("clock-id-input").value.trim();
  const inputMyNdid = document.getElementById("my-ndid-input").value.trim();

  if (!inputClockId) return;

  appState.clockId = inputClockId;
  appState.myNdid = inputMyNdid || "ND-USER-01";

  document.getElementById("active-clock-id").innerText = appState.clockId;
  document.getElementById("modal-clock-id").innerText = appState.clockId;

  showToast(`Đã liên kết mã đồng hồ ${appState.clockId}! Đang lưu mật khẩu 50 ký tự...`);

  // Prompt Credential Management API
  saveCredentialsToBrowser();
}

// 2. CREDENTIAL MANAGEMENT API INTEGRATION (`navigator.credentials.store`)
async function saveCredentialsToBrowser() {
  if (!('PasswordCredential' in window) && !('credentials' in navigator)) {
    alert("Trình duyệt không hỗ trợ Credential Management API trực tiếp, nhưng bạn có thể lưu thủ công bằng Autofill.");
    return;
  }

  try {
    const cred = new PasswordCredential({
      id: appState.clockId,
      password: appState.secretToken50,
      name: `ND Alarm (${appState.clockId})`
    });

    await navigator.credentials.store(cred);
    showToast("✅ Đã gửi yêu cầu lưu mật khẩu 50 ký tự vào trình duyệt!");
  } catch (error) {
    console.warn("Credential store error (often requires HTTPS user gesture):", error);
    showToast("⚠️ Lưu ý: Trình duyệt yêu cầu HTTPS để lưu mật khẩu tự động.");
  }
}

// 3. GENERATE NEW 50-CHAR TOKEN
function generateNewToken50() {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*-_=+";
  let newToken = "";
  for (let i = 0; i < 50; i++) {
    newToken += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  appState.secretToken50 = newToken;
  document.getElementById("display-token-50").innerText = newToken;
  showToast("Đã sinh mật khẩu 50 ký tự mới!");
  saveCredentialsToBrowser();
}

// 4. COPY TOKEN TO CLIPBOARD
function copyToken50() {
  navigator.clipboard.writeText(appState.secretToken50).then(() => {
    showToast("📋 Đã sao chép mật khẩu 50 ký tự vào bộ nhớ tạm!");
  });
}

// 5. SIMULATE ALARM RINGING & AUTO-POPUP TURN OFF DIALOG
function simulateAlarmRinging() {
  appState.status = "ringing";
  
  const box = document.getElementById("clock-status-box");
  box.className = "status-box status-ringing";
  document.getElementById("clock-state-text").innerText = "ĐANG REO CHUÔNG (RINGING)";
  
  showToast("🚨 BÁO THỨC ĐANG REO! Tự động bật Hộp thoại Tắt...");
  setTimeout(() => {
    openTurnOffModal();
  }, 400);
}

// 6. OPEN TURN OFF MODAL & AUTO-FILL
async function openTurnOffModal() {
  const modal = document.getElementById("turn-off-modal");
  const pinBlock = document.getElementById("pin-challenge-block");
  const errorMsg = document.getElementById("modal-error-msg");
  
  modal.classList.remove("hidden");
  errorMsg.classList.add("hidden");
  
  document.getElementById("modal-clock-id").innerText = appState.clockId;

  // Check if current user is in Trusted NDID list
  const isTrusted = appState.trustedNdids.includes(appState.myNdid) || (appState.myNdid === appState.ownerNdid);
  if (isTrusted) {
    pinBlock.classList.add("hidden");
    document.getElementById("modal-warning").innerText = 
      `NDID "${appState.myNdid}" nằm trong danh sách Tin Cậy (Bỏ qua PIN 8 số).`;
  } else {
    pinBlock.classList.remove("hidden");
    document.getElementById("modal-warning").innerText = 
      `NDID "${appState.myNdid}" là thiết bị khác: Yêu cầu cả Token 50 và PIN 8 số (${appState.emergencyPin8}).`;
  }

  // Attempt to autofill via Credential Management or state
  const passwordInput = document.getElementById("autofill-token-50");
  try {
    if (navigator.credentials && navigator.credentials.get) {
      const cred = await navigator.credentials.get({
        password: true,
        mediation: "optional"
      });
      if (cred && cred.password) {
        passwordInput.value = cred.password;
        return;
      }
    }
  } catch (e) {
    console.log("Credentials get mediation:", e);
  }

  // Fallback autofill
  passwordInput.value = appState.secretToken50;
}

function closeTurnOffModal() {
  document.getElementById("turn-off-modal").classList.add("hidden");
}

// 7. SUBMIT TURN OFF ALARM
function submitTurnOffAlarm(event) {
  event.preventDefault();
  const inputToken = document.getElementById("autofill-token-50").value.trim();
  const inputPin = document.getElementById("input-pin-8").value.trim();
  const errorMsg = document.getElementById("modal-error-msg");
  const isTrusted = appState.trustedNdids.includes(appState.myNdid) || (appState.myNdid === appState.ownerNdid);

  // Validate 50-char token
  if (inputToken !== appState.secretToken50) {
    errorMsg.innerText = "❌ Mật khẩu 50 ký tự không khớp với thiết bị!";
    errorMsg.classList.remove("hidden");
    return;
  }

  // Validate PIN if not trusted
  if (!isTrusted && inputPin !== appState.emergencyPin8) {
    errorMsg.innerText = "❌ Mã PIN 8 số không chính xác (Kiểm tra trên màn hình App)!";
    errorMsg.classList.remove("hidden");
    return;
  }

  // Success: update status to unlocked
  appState.status = "unlocked";
  closeTurnOffModal();

  const box = document.getElementById("clock-status-box");
  box.className = "status-box status-idle";
  document.getElementById("clock-state-text").innerText = "ĐÃ TẮT BÁO THỨC (UNLOCKED)";

  showToast("🎉 ĐÃ TẮT BÁO THỨC THÀNH CÔNG TỪ WEB PORTAL!");

  // In production with Firebase:
  // db.collection("alarms").doc(appState.clockId).update({ status: "unlocked", unlockedAt: firebase.firestore.FieldValue.serverTimestamp() });
}

// 8. TRUSTED NDID MANAGEMENT
function addTrustedNdid() {
  const input = document.getElementById("new-trusted-ndid");
  const newNdid = input.value.trim().toUpperCase();

  if (!newNdid) return;
  if (appState.trustedNdids.includes(newNdid)) {
    showToast("NDID này đã có trong danh sách!");
    return;
  }

  appState.trustedNdids.push(newNdid);

  const list = document.getElementById("trusted-ndid-list");
  const li = document.createElement("li");
  li.className = "pill-list-item";
  li.innerHTML = `
    <div>
      <strong>${newNdid}</strong>
      <span class="badge-trusted">Đã cấp phép (Bypass PIN)</span>
    </div>
    <button class="btn-remove" onclick="removeTrustedNdid(this)">Xóa</button>
  `;
  list.appendChild(li);

  input.value = "";
  showToast(`Đã thêm ${newNdid} vào danh sách Tin Cậy!`);
}

function removeTrustedNdid(btn) {
  const item = btn.closest(".pill-list-item");
  const ndidText = item.querySelector("strong").innerText;
  appState.trustedNdids = appState.trustedNdids.filter(id => id !== ndidText);
  item.remove();
  showToast(`Đã gỡ quyền của ${ndidText}!`);
}

// Simple Toast Notification
function showToast(message) {
  let toast = document.getElementById("nd-toast");
  if (!toast) {
    toast = document.createElement("div");
    toast.id = "nd-toast";
    toast.style.cssText = `
      position: fixed;
      bottom: 24px;
      right: 24px;
      background: #0f172a;
      color: #ffffff;
      padding: 14px 22px;
      border-radius: 9999px;
      font-size: 0.9rem;
      font-weight: 600;
      box-shadow: 0 10px 25px rgba(0,0,0,0.3);
      z-index: 10000;
      transition: opacity 0.3s ease;
    `;
    document.body.appendChild(toast);
  }
  toast.innerText = message;
  toast.style.opacity = "1";
  setTimeout(() => {
    toast.style.opacity = "0";
  }, 3500);
}

function checkBrowserCredentialSupport() {
  if (window.PasswordCredential || ('credentials' in navigator)) {
    console.log("Credential Management API is supported on this browser.");
  }
}
