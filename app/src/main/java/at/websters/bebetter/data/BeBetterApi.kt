package at.websters.bebetter.data

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import retrofit2.http.*

interface BeBetterApi {
    // ---- Auth ----
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): AuthResponse
    @POST("auth/demo") suspend fun demo(): AuthResponse
    @GET("auth/me") suspend fun me(): MeResponse
    @PUT("auth/me") suspend fun updateMe(@Body body: Map<String, @JvmSuppressWildcards Any?>): MeResponse
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body body: Map<String, String>): Map<String, String>
    @POST("auth/reset-password") suspend fun resetPassword(@Body body: Map<String, String>): Map<String, String>
    @POST("auth/change-password") suspend fun changePassword(@Body body: Map<String, String>): Map<String, String>
    @POST("auth/logout") suspend fun logout(): Map<String, String>
    @HTTP(method = "DELETE", path = "auth/account", hasBody = true) suspend fun deleteAccount(@Body body: Map<String, String>): Map<String, String>

    // ---- Habits ----
    @GET("habits") suspend fun habits(): HabitsResponse
    @GET("habits/scheduled") suspend fun scheduledHabits(@Query("date") date: String? = null): HabitsResponse
    @GET("habits/{id}") suspend fun habitDetail(@Path("id") id: String): HabitDetailWrapper
    @POST("habits") suspend fun createHabit(@Body body: Map<String, @JvmSuppressWildcards Any?>): HabitWrapper
    @PUT("habits/{id}") suspend fun updateHabit(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): HabitWrapper
    @DELETE("habits/{id}") suspend fun deleteHabit(@Path("id") id: String): Map<String, String>
    @POST("habits/{id}/break/start") suspend fun breakStart(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): Map<String, String>
    @POST("habits/{id}/break/end") suspend fun breakEnd(@Path("id") id: String): Map<String, String>
    @POST("habits/{id}/finish") suspend fun finishHabit(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): Map<String, String>
    @POST("habits/{id}/buddy") suspend fun addBuddy(@Path("id") id: String, @Body body: Map<String, String>): Map<String, String>

    // ---- Logs ----
    @POST("logs") suspend fun completeHabit(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, JsonElement>
    @GET("logs/today") suspend fun logsToday(): LogsTodayResponse
    @GET("logs/with-scheduled") suspend fun logsWithScheduled(@Query("date") date: String? = null): Map<String, JsonElement>
    @DELETE("logs/{id}") suspend fun deleteLog(@Path("id") id: String): Map<String, String>
    @DELETE("logs/habit/{habitId}") suspend fun undoHabit(@Path("habitId") habitId: String, @Query("scheduledTime") scheduledTime: String? = null, @Query("date") date: String? = null): Map<String, String>

    // ---- Grid ----
    @GET("grid") suspend fun grid(@Query("from") from: String? = null, @Query("to") to: String? = null): GridResponse
    @GET("grid/years") suspend fun gridYears(): GridYearsResponse
    @GET("grid/day") suspend fun gridDay(@Query("date") date: String): GridDayDetailResponse

    // ---- Stats ----
    @GET("stats/overview") suspend fun statsOverview(): StatsOverview
    @GET("stats/streak") suspend fun statsStreak(): Map<String, JsonElement>
    @GET("stats/consistency") suspend fun statsConsistency(): Map<String, JsonElement>
    @GET("stats/weekly") suspend fun statsWeekly(): Map<String, JsonElement>

    // ---- Tasks ----
    @GET("tasks") suspend fun tasks(@Query("date") date: String? = null): TasksResponse
    @POST("tasks") suspend fun createTask(@Body body: Map<String, @JvmSuppressWildcards Any?>): TaskWrapper
    @PUT("tasks/{id}") suspend fun updateTask(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): TaskWrapper
    @DELETE("tasks/{id}") suspend fun deleteTask(@Path("id") id: String): Map<String, String>
    @POST("tasks/reorder") suspend fun reorderTasks(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, String>
    @POST("tasks/{id}/complete") suspend fun completeTask(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): Map<String, String>
    @GET("tasks/completed") suspend fun completedTasks(@Query("date") date: String? = null): Map<String, JsonElement>
    @DELETE("tasks/{id}/uncomplete") suspend fun uncompleteTask(@Path("id") id: String, @Query("date") date: String? = null): Map<String, String>

    // ---- Friends ----
    @GET("friends/list") suspend fun friendList(): FriendListResponse
    @GET("friends") suspend fun friendsAlt(): FriendListResponse
    @GET("friends/search") suspend fun friendSearch(@Query("q") q: String): FriendSearchResponse
    @GET("friends/lookup") suspend fun friendLookup(@Query("q") q: String): Map<String, JsonElement>
    @POST("friends/request") suspend fun friendRequest(@Body body: Map<String, String>): Map<String, String>
    @GET("friends/requests") suspend fun friendRequests(): FriendRequestsResponse
    @POST("friends/request/{id}/accept") suspend fun acceptRequest(@Path("id") id: String): Map<String, String>
    @POST("friends/request/{id}/decline") suspend fun declineRequest(@Path("id") id: String): Map<String, String>
    @DELETE("friends/{id}") suspend fun removeFriend(@Path("id") id: String): Map<String, String>
    @GET("friends/feed") suspend fun feed(): Map<String, List<FeedItem>>
    @GET("friends/profile/{id}") suspend fun friendProfile(@Path("id") id: String): FriendProfileResponse
    @POST("friends/link") suspend fun createFriendLink(): Map<String, String>
    @POST("friends/link/accept") suspend fun acceptFriendLink(@Body body: Map<String, String>): Map<String, String>

    // ---- Challenges ----
    @GET("challenges") suspend fun challenges(): ChallengesResponse
    @POST("challenges") suspend fun createChallenge(@Body body: Map<String, @JvmSuppressWildcards Any?>): ChallengeWrapper
    @GET("challenges/{id}") suspend fun challengeDetail(@Path("id") id: String): ChallengeWrapper
    @POST("challenges/{id}/accept") suspend fun acceptChallenge(@Path("id") id: String): Map<String, String>
    @POST("challenges/{id}/decline") suspend fun declineChallenge(@Path("id") id: String): Map<String, String>
    @GET("challenges/{id}/grid") suspend fun challengeGrid(@Path("id") id: String): Map<String, JsonElement>
    @POST("challenges/{id}/resolve") suspend fun resolveChallenge(@Path("id") id: String, @Body body: Map<String, String?>): Map<String, String>
    @GET("challenges/leaderboard/friends") suspend fun friendsLeaderboard(): Map<String, JsonElement>
    @POST("challenges/invite-link") suspend fun challengeInviteLink(@Body body: Map<String, String>): Map<String, String>
    @GET("challenges/invite/{token}") suspend fun challengeInvite(@Path("token") token: String): Map<String, JsonElement>
    @POST("challenges/invite/{token}/accept") suspend fun acceptChallengeInvite(@Path("token") token: String): Map<String, String>

    // ---- Leaderboard ----
    @GET("leaderboard/global") suspend fun globalLeaderboard(): LeaderboardResponse

    // ---- Presets ----
    @GET("presets") suspend fun presets(@Query("q") q: String? = null, @Query("category") category: String? = null): PresetsResponse
    @GET("presets/{id}") suspend fun presetDetail(@Path("id") id: String): PresetDetailWrapper
    @POST("presets") suspend fun createPreset(@Body body: Map<String, @JvmSuppressWildcards Any?>): PresetWrapper
    @POST("presets/{id}/like") suspend fun likePreset(@Path("id") id: String): Map<String, String>
    @POST("presets/{id}/fork") suspend fun forkPreset(@Path("id") id: String): Map<String, String>
    @POST("presets/{id}/use") suspend fun usePreset(@Path("id") id: String): Map<String, String>
    @POST("presets/{id}/stop-using") suspend fun stopUsingPreset(@Path("id") id: String): Map<String, String>
    @POST("presets/{id}/report") suspend fun reportPreset(@Path("id") id: String, @Body body: Map<String, String>): Map<String, String>

    // ---- Notifications ----
    @GET("notifications") suspend fun notifications(): NotificationsResponse
    @POST("notifications/read") suspend fun markRead(@Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): Map<String, String>
    @GET("notifications/preferences") suspend fun notifPrefs(): NotifPrefs
    @PUT("notifications/preferences") suspend fun updateNotifPrefs(@Body body: NotifPrefs): NotifPrefs

    // ---- Vacation ----
    @GET("vacation/status") suspend fun vacationStatus(): VacationStatus
    @POST("vacation/start") suspend fun vacationStart(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, String>
    @POST("vacation/end") suspend fun vacationEnd(): Map<String, String>

    // ---- Upload ----
    @Multipart @POST("upload") suspend fun upload(@Part file: MultipartBody.Part): UploadResponse

    // ---- Assistant ----
    @GET("assistant/settings") suspend fun assistantSettings(): AssistantSettings
    @PUT("assistant/settings") suspend fun updateAssistantSettings(@Body body: Map<String, @JvmSuppressWildcards Any?>): AssistantSettings
    @POST("assistant/chat") suspend fun assistantChat(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, JsonElement>
    @GET("assistant/sessions") suspend fun assistantSessions(): Map<String, JsonElement>

    // ---- Admin ----
    @GET("admin/stats") suspend fun adminStats(): Map<String, JsonElement>
    @GET("admin/users") suspend fun adminUsers(@Query("q") q: String? = null): Map<String, JsonElement>
    @POST("admin/users/{id}/role") suspend fun adminSetRole(@Path("id") id: String, @Body body: Map<String, String>): Map<String, String>
    @POST("admin/users/{id}/ban") suspend fun adminBan(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): Map<String, String>
    @POST("admin/users/{id}/unban") suspend fun adminUnban(@Path("id") id: String): Map<String, String>
    @GET("admin/reports") suspend fun adminReports(): Map<String, JsonElement>
}
