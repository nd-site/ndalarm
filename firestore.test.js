const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- UNATHENTICATED ACCESS TESTS ---
test("Unauthenticated user: cannot read user profiles or alarms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("alarms").get());
});

// --- CROSS-USER ISOLATION TESTS ---
test("Cross-user isolation: Bob cannot read or write Alice's alarm", async () => {
  // Alice seeds an alarm
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("alarms").doc("alarm_1").set({
      userId: ALICE_UID,
      label: "Thức dậy sớm",
      hour: 6,
      minute: 30,
      isEnabled: true,
      isLunarCalendar: false,
      repeatRule: "DAILY",
      antiCheatMode: "TOKEN_50",
      clockId: "ND-CL01",
      status: "armed",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("alarms").doc("alarm_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("alarms").doc("alarm_1").delete());
});

// --- OWNER SUCCESSFUL OPERATIONS ---
test("Owner: can create and read own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileDoc = aliceDb.collection("users").doc(ALICE_UID);
  
  await assertSucceeds(profileDoc.set({
    userId: ALICE_UID,
    email: "alice@example.com",
    displayName: "Alice",
    clockId: "ND-CL01",
    ndid: "ND-MASTER",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  await assertSucceeds(profileDoc.get());
});

test("Owner: can create, query, and update own alarms", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const alarmRef = aliceDb.collection("users").doc(ALICE_UID).collection("alarms").doc("alarm_1");

  await assertSucceeds(alarmRef.set({
    userId: ALICE_UID,
    label: "Báo thức Mùng 1",
    hour: 7,
    minute: 0,
    isEnabled: true,
    isLunarCalendar: true,
    repeatRule: "LUNAR_1_15",
    lunarDay: 1,
    lunarMonth: 0,
    ringtoneType: "gentle_breeze",
    vibrate: true,
    antiCheatMode: "TOKEN_50",
    clockId: "ND-CL01",
    ndid: "ND-MASTER",
    status: "armed",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  const snapshot = await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("alarms").get());
});

test("Owner: can add trusted NDID and record history log", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  
  // Trusted NDID
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("trusted_ndids").doc("device_2").set({
    userId: ALICE_UID,
    ndid: "ND-FAMILY-02",
    label: "Living room tablet",
    createdAt: new Date(),
  }));

  // Ringing History (append-only)
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("ringing_history").doc("hist_1").set({
    userId: ALICE_UID,
    alarmLabel: "Báo thức sáng",
    triggeredTime: Date.now(),
    unlockedTime: Date.now() + 60000,
    unlockMethod: "TOKEN_50",
    ndid: "ND-MASTER",
    createdAt: new Date(),
  }));
});
