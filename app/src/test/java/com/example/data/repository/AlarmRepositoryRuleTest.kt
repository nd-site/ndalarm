package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FirestoreAlarmRepository
import com.example.data.firebase.model.CloudAlarm
import com.example.data.firebase.model.CloudRingingHistory
import com.example.data.firebase.model.CloudTrustedNdid
import com.example.data.firebase.model.CloudUserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AlarmRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun authenticatedUser_canSaveAndObserveAlarms() = runBlocking {
    val email = "alarm_owner_${System.currentTimeMillis()}@test.com"
    val uid = signInTestUser(email)
    val repo = FirestoreAlarmRepository(firestore, auth)

    // Save profile
    val profile = CloudUserProfile(
      userId = uid,
      email = email,
      displayName = "ND Owner",
      clockId = "ND-CL01",
      ndid = "ND-MASTER"
    )
    repo.saveUserProfile(profile)

    // Save alarm
    val alarm = CloudAlarm(
      userId = uid,
      label = "Báo thức Mùng 1",
      hour = 7,
      minute = 0,
      isEnabled = true,
      isLunarCalendar = true,
      repeatRule = "LUNAR_1_15",
      lunarDay = 1,
      lunarMonth = 0,
      ringtoneType = "gentle_breeze",
      vibrate = true,
      antiCheatMode = "TOKEN_50",
      clockId = "ND-CL01",
      ndid = "ND-MASTER",
      status = "armed"
    )
    val alarmId = repo.saveAlarm(alarm)
    assertTrue(alarmId.isNotBlank())

    // Observe alarms
    val alarms = repo.observeAlarms(uid).first()
    assertTrue(alarms.isNotEmpty())
    val savedAlarm = alarms.find { it.id == alarmId }
    assertNotNull(savedAlarm)
    assertEquals("Báo thức Mùng 1", savedAlarm?.label)
    assertEquals(7, savedAlarm?.hour)
  }

  @Test
  fun authenticatedUser_canSaveTrustedNdidAndHistory() = runBlocking {
    val email = "user_sync_${System.currentTimeMillis()}@test.com"
    val uid = signInTestUser(email)
    val repo = FirestoreAlarmRepository(firestore, auth)

    // Save trusted NDID
    val trustedNdid = CloudTrustedNdid(
      userId = uid,
      ndid = "ND-FAMILY-01",
      label = "Tablet phòng khách"
    )
    val ndidDocId = repo.saveTrustedNdid(trustedNdid)
    assertTrue(ndidDocId.isNotBlank())

    val ndids = repo.observeTrustedNdids(uid).first()
    assertTrue(ndids.isNotEmpty())
    val savedNdid = ndids.find { it.ndid == "ND-FAMILY-01" }
    assertNotNull(savedNdid)

    // Record history
    val history = CloudRingingHistory(
      userId = uid,
      alarmLabel = "Báo thức sáng",
      triggeredTime = 1700000000000L,
      unlockedTime = 1700000060000L,
      unlockMethod = "TOKEN_50",
      ndid = "ND-MASTER"
    )
    val histDocId = repo.recordHistory(history)
    assertTrue(histDocId.isNotBlank())

    val histories = repo.observeRingingHistory(uid).first()
    assertEquals(1, histories.size)
    assertEquals("TOKEN_50", histories[0].unlockMethod)
  }

  @Test
  fun unauthenticatedUser_cannotWriteToFirestore() = runBlocking {
    auth.signOut()
    val repo = FirestoreAlarmRepository(firestore, auth)

    try {
      repo.saveAlarm(
        CloudAlarm(
          userId = "unauthenticated_user",
          label = "Hacked Alarm",
          hour = 8,
          minute = 0,
          clockId = "ND-HACK",
          status = "armed"
        )
      )
      fail("Expected IllegalStateException or FirebaseFirestoreException for unauthenticated write")
    } catch (e: IllegalStateException) {
      // Direct fail-fast check in repository
      assertTrue(e.message?.contains("User must be signed in") == true)
    }
  }

  @Test
  fun crossUser_cannotAccessOtherUserData() = runBlocking {
    val aliceUid = signInTestUser("alice@test.com")
    val aliceRepo = FirestoreAlarmRepository(firestore, auth)
    val alarmId = aliceRepo.saveAlarm(
      CloudAlarm(
        userId = aliceUid,
        label = "Alice Secret Alarm",
        hour = 6,
        minute = 30,
        clockId = "ND-ALICE",
        status = "armed"
      )
    )

    // Sign in as Bob
    val bobUid = signInTestUser("bob@test.com")
    try {
      // Bob tries to read Alice's alarm document directly
      val doc = firestore.collection("users").document(aliceUid).collection("alarms").document(alarmId)
      doc.get().await()
      fail("Expected PERMISSION_DENIED when Bob tries to read Alice's alarm")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      if (firestoreException != null) {
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
      } else {
        assertTrue(e.message?.contains("PERMISSION_DENIED") == true || e is FirebaseFirestoreException)
      }
    }
  }
}
