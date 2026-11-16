const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowDate() {
  return new Date(Date.now() - 1000);
}

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

test("Unauthenticated caller: cannot read publicProfiles, chats, or private user docs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("publicProfiles").get());
  await assertFails(unauthDb.collection("chats").get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/private/info`).get());
});

test("Public profile & unique username reservation: owner can create; cross-user spoof fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowDate();

  await assertSucceeds(
    aliceDb.doc(`usernames/alice_limon`).set({
      uid: ALICE_UID,
      username: "Alice_Limon",
      createdAt: ts,
    })
  );

  await assertFails(
    bobDb.doc(`usernames/alice_limon`).set({
      uid: BOB_UID,
      username: "alice_limon",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`publicProfiles/${ALICE_UID}`).set({
      uid: ALICE_UID,
      username: "Alice_Limon",
      usernameLowercase: "alice_limon",
      displayName: "Alice",
      bio: "Hey there!",
      online: true,
      lastSeen: ts,
      privacyShowLastSeen: "everyone",
      privacyShowOnline: "everyone",
      privacyShowPhoto: "everyone",
      privacyReadReceipts: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    bobDb.doc(`publicProfiles/${ALICE_UID}`).update({
      displayName: "Hacked",
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.doc(`publicProfiles/${ALICE_UID}`).update({
      isAdmin: true,
      updatedAt: ts,
    })
  );
});

test("Private PII isolation: only owner can read/write users/{uid}/private/{docId}", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowDate();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/private/info`).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      emailVerified: true,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.doc(`users/${ALICE_UID}/private/info`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/private/info`).set({
      userId: BOB_UID,
      email: "spoof@example.com",
      updatedAt: ts,
    })
  );
});

test("1:1 Chats, Messages & Shared Wallpaper: members can update sharedWallpaper; non-members blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowDate();
  const chatId = "chat_alice_bob";

  await assertSucceeds(
    aliceDb.doc(`chats/${chatId}`).set({
      memberUids: [ALICE_UID, BOB_UID],
      lastMessageText: "Hello Bob!",
      lastMessageType: "text",
      lastMessageSenderId: ALICE_UID,
      lastMessageAt: ts,
      unreadBy: { [BOB_UID]: 1 },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(charlieDb.doc(`chats/${chatId}`).get());

  // Member Alice can set shared wallpaper when Show User is enabled
  await assertSucceeds(
    aliceDb.doc(`chats/${chatId}`).update({
      sharedWallpaperPreset: "CUSTOM",
      sharedWallpaperUrl: "rtdb://wallpapers/chat_alice_bob?v=12345",
      sharedWallpaperBlur: true,
      sharedWallpaperBy: ALICE_UID,
      updatedAt: ts,
    })
  );

  // Member Alice cannot spoof sharedWallpaperBy as Bob
  await assertFails(
    aliceDb.doc(`chats/${chatId}`).update({
      sharedWallpaperPreset: "LAVENDER_SKY",
      sharedWallpaperBy: BOB_UID,
      updatedAt: ts,
    })
  );

  // Non-member Charlie cannot update shared wallpaper
  await assertFails(
    charlieDb.doc(`chats/${chatId}`).update({
      sharedWallpaperPreset: "MIDNIGHT_AURORA",
      sharedWallpaperBy: CHARLIE_UID,
      updatedAt: ts,
    })
  );

  // Alice sends message (after wallpaper was set)
  await assertSucceeds(
    aliceDb.doc(`chats/${chatId}/messages/msg_1`).set({
      chatId,
      senderId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      type: "text",
      text: "Hello Bob!",
      deliveryStatus: "sent",
      deletedForEveryone: false,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    bobDb
      .collection(`chats/${chatId}/messages`)
      .where("memberUids", "array-contains", BOB_UID)
      .orderBy("createdAt", "desc")
      .limit(40)
      .get()
  );

  await assertSucceeds(
    bobDb.doc(`chats/${chatId}/messages/msg_1`).update({
      deliveryStatus: "read",
    })
  );

  await assertFails(
    bobDb.doc(`chats/${chatId}/messages/msg_1`).update({
      text: "Tampered text",
    })
  );
});

test("Blocking enforcement: blocked user cannot create chat, message, or call", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowDate();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/blocks/${BOB_UID}`).set({
      blockedUid: BOB_UID,
      blockedDisplayName: "Bob",
      blockedUsername: "bob",
      createdAt: ts,
    })
  );

  await assertFails(
    bobDb.doc(`chats/chat_bob_alice`).set({
      memberUids: [BOB_UID, ALICE_UID],
      lastMessageText: "Hey",
      lastMessageType: "text",
      lastMessageSenderId: BOB_UID,
      lastMessageAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    bobDb.doc(`calls/call_1`).set({
      callerId: BOB_UID,
      receiverId: ALICE_UID,
      memberUids: [BOB_UID, ALICE_UID],
      callerName: "Bob",
      receiverName: "Alice",
      callType: "voice",
      status: "ringing",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Call signaling, terminal state lock, and Delete for Me on ended calls", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowDate();

  await assertSucceeds(
    aliceDb.doc(`calls/call_abc`).set({
      callerId: ALICE_UID,
      receiverId: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID],
      callerName: "Alice",
      receiverName: "Bob",
      callType: "video",
      status: "ringing",
      offerSdp: "v=0...",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.doc(`calls/call_abc`).update({
      status: "accepted",
      answerSdp: "v=0...",
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`calls/call_abc`).update({
      status: "ended",
      durationSeconds: 42,
      endedAt: ts,
      updatedAt: ts,
    })
  );

  // Terminal call cannot be reopened
  await assertFails(
    bobDb.doc(`calls/call_abc`).update({
      status: "ringing",
      updatedAt: ts,
    })
  );

  // Members can soft-delete the ended call log for themselves
  await assertSucceeds(
    aliceDb.doc(`calls/call_abc`).update({
      deletedForUsers: [ALICE_UID],
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.doc(`calls/call_abc`).update({
      deletedForUsers: [ALICE_UID, BOB_UID],
      updatedAt: ts,
    })
  );

  // Bob cannot strip Alice out of deletedForUsers
  await assertFails(
    bobDb.doc(`calls/call_abc`).update({
      deletedForUsers: [BOB_UID],
      updatedAt: ts,
    })
  );

  // Non-member Charlie cannot touch deletedForUsers
  await assertFails(
    charlieDb.doc(`calls/call_abc`).update({
      deletedForUsers: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: ts,
    })
  );
});

test("Chat chunked media & publicAvatars: member-only media isolation and owner-only avatars", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowDate();
  const chatId = "chat_alice_bob_media";

  await assertSucceeds(
    aliceDb.doc(`chats/${chatId}`).set({
      memberUids: [ALICE_UID, BOB_UID],
      lastMessageText: "",
      lastMessageType: "text",
      lastMessageSenderId: ALICE_UID,
      lastMessageAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`chats/${chatId}/media/med_1`).set({
      chatId,
      senderId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      fileName: "voice_1.m4a",
      mimeType: "audio/mp4",
      sizeBytes: 1024,
      chunkCount: 1,
      createdAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.doc(`chats/${chatId}/media/med_1/chunks/0`).set({
      senderId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      index: 0,
      data: "AQIDBA==",
    })
  );

  await assertSucceeds(bobDb.doc(`chats/${chatId}/media/med_1`).get());
  await assertSucceeds(bobDb.doc(`chats/${chatId}/media/med_1/chunks/0`).get());
  await assertFails(charlieDb.doc(`chats/${chatId}/media/med_1`).get());

  await assertSucceeds(
    aliceDb.doc(`publicAvatars/${ALICE_UID}`).set({
      uid: ALICE_UID,
      mimeType: "image/jpeg",
      data: "AQIDBA==",
      updatedAt: ts,
    })
  );
  await assertSucceeds(bobDb.doc(`publicAvatars/${ALICE_UID}`).get());
  await assertFails(
    bobDb.doc(`publicAvatars/${ALICE_UID}`).set({
      uid: BOB_UID,
      mimeType: "image/jpeg",
      data: "AQIDBA==",
      updatedAt: ts,
    })
  );
});

test("View-Once photo consumption and ChatPreference locked state", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowDate();
  const chatId = "chat_view_once_lock";

  await assertSucceeds(
    aliceDb.doc(`chats/${chatId}`).set({
      memberUids: [ALICE_UID, BOB_UID],
      lastMessageText: "View once photo",
      lastMessageType: "image",
      lastMessageSenderId: ALICE_UID,
      lastMessageAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`chats/${chatId}/messages/vo_1`).set({
      chatId,
      senderId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      type: "image",
      text: "Secret sunset",
      mediaUrl: "rtdb://chats/chat_view_once_lock/media/m_1",
      deliveryStatus: "sent",
      isViewOnce: true,
      viewOnceViewed: false,
      deletedForEveryone: false,
      createdAt: ts,
    })
  );

  // Sender cannot mark recipient's viewOnceViewed
  await assertFails(
    aliceDb.doc(`chats/${chatId}/messages/vo_1`).update({
      viewOnceViewed: true,
    })
  );

  // Recipient Bob can mark viewOnceViewed = true once
  await assertSucceeds(
    bobDb.doc(`chats/${chatId}/messages/vo_1`).update({
      viewOnceViewed: true,
    })
  );

  // Cannot un-expire viewOnceViewed
  await assertFails(
    bobDb.doc(`chats/${chatId}/messages/vo_1`).update({
      viewOnceViewed: false,
    })
  );

  // Owner can set locked preference on their chatPreferences
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/chatPreferences/${chatId}`).set({
      chatId,
      muted: false,
      archived: false,
      pinned: false,
      locked: true,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/chatPreferences/${chatId}`).get());
});
