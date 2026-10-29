const appState = {
  clockId: "ND-CL01",
  myNdid: "ND-USER-01",
  ownerNdid: "ND-MASTER",
  secretToken50: "Nd9$kQx7#mP2vL8*zY4@wR1!bT5^hJ9~cF3&gK6_dE0+aU4-sM7=",
  emergencyPin8: "49201847",
  status: "idle",
  trustedNdids: ["ND-MASTER", "ND-FAMILY-01"],
  alarmLabel: "Báo thức ND Labs",
  alarmTime: "07:00"
};

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("clock-id-input").value = appState.clockId;
  document.getElementById("display-token-50").innerText = appState.secretToken50;
  document.getElementById("active-clock-id").innerText = appState.clockId;
  document.getElementById("active-owner-ndid").innerText = appState.ownerNdid;
  document.getElementById("active-pin-8").innerText = appState.emergencyPin8;
});

function handleLinkClock(event) {
  event.preventDefault();
  appState.clockId = document.getElementById("clock-id-input").value.trim();
  document.getElementById("active-clock-id").innerText = appState.clockId;
  alert(`Đã liên kết mã đồng hồ ${appState.clockId}!`);
}

function saveCredentialsToBrowser() {
  alert("Credential Management API: Đã gửi yêu cầu lưu mật khẩu 50 ký tự vào trình duyệt!");
}

function generateNewToken50() {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*-_=+";
  let newToken = "";
  for (let i = 0; i < 50; i++) {
    newToken += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  appState.secretToken50 = newToken;
  document.getElementById("display-token-50").innerText = newToken;
}

function copyToken50() {
  navigator.clipboard.writeText(appState.secretToken50).then(() => {
    alert("Đã sao chép mật khẩu 50 ký tự!");
  });
}

function simulateAlarmRinging() {
  appState.status = "ringing";
  openTurnOffModal();
}

function openTurnOffModal() {
  document.getElementById("turn-off-modal").classList.remove("hidden");
  document.getElementById("autofill-token-50").value = appState.secretToken50;
}

function closeTurnOffModal() {
  document.getElementById("turn-off-modal").classList.add("hidden");
}

function submitTurnOffAlarm(event) {
  event.preventDefault();
  const input = document.getElementById("autofill-token-50").value;
  if (input === appState.secretToken50) {
    alert("🎉 Đã tắt báo thức thành công từ Web Portal!");
    closeTurnOffModal();
  } else {
    alert("❌ Mật khẩu 50 ký tự không khớp!");
  }
}
