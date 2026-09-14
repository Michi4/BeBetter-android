package at.websters.bebetter.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ---------- Auth ----------
data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val avatar: String? = null,
    val bio: String? = null,
    val role: String = "user",
    val isPublic: Boolean = false,
    val isDemo: Boolean = false,
    val createdAt: String? = null
)

data class AuthResponse(
    val token: String = "",
    val user: User? = null
)

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val agreeToTerms: Boolean = true,
    val friendToken: String? = null
)
data class MeResponse(val user: User)

// ---------- Habits ----------
data class HabitTag(val id: String = "", val name: String = "", val color: String? = null)

data class HabitBreak(
    val id: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
    val reason: String? = null
)

data class ScheduleSlot(
    val time: String? = null,
    val days: List<Int>? = null
)

data class Habit(
    val id: String = "",
    val userId: String? = null,
    val title: String = "",
    val description: String? = null,
    val emoji: String = "",
    val frequencyType: String = "daily",
    val daysPerWeek: JsonElement? = null,
    val schedules: JsonElement? = null,
    val intervalDays: Int? = null,
    val reminderMinutes: JsonElement? = null,
    val verificationType: String = "honor",
    val active: Boolean = true,
    val bestStreak: Int = 0,
    val isPublic: Boolean = false,
    val createdAt: String? = null,
    val tags: List<HabitTag>? = null,
    val breaks: List<HabitBreak>? = null,
    // enriched by backend
    val scheduledDays: List<Int>? = null,
    val scheduledTime: String? = null,
    val completedToday: Boolean? = null,
    val isActive: Boolean? = null,
    val challengeId: String? = null,
    val user: User? = null
) {
    fun parsedDays(): List<Int> = try {
        when {
            daysPerWeek == null -> listOf(0,1,2,3,4,5,6)
            daysPerWeek.isJsonArray -> daysPerWeek.asJsonArray.mapNotNull { runCatching { it.asInt }.getOrNull() }
            daysPerWeek.isJsonPrimitive -> emptyList()
            else -> scheduledDays ?: listOf(0,1,2,3,4,5,6)
        }
    } catch (_: Exception) { scheduledDays ?: listOf(0,1,2,3,4,5,6) }

    fun parsedSchedules(): List<ScheduleSlot> {
        return try {
            if (schedules == null || !schedules.isJsonArray) return emptyList()
            schedules.asJsonArray.mapNotNull { el ->
                runCatching {
                    val o = el.asJsonObject
                    val t = if (o.has("time") && !o.get("time").isJsonNull) o.get("time").asString else null
                    val d = if (o.has("days") && o.get("days").isJsonArray) o.getAsJsonArray("days").map { it.asInt } else null
                    ScheduleSlot(t, d)
                }.getOrNull()
            }
        } catch (_: Exception) { emptyList() }
    }
}

data class HabitsResponse(val habits: List<Habit> = emptyList())
data class HabitWrapper(val habit: Habit = Habit())
data class HabitDetailWrapper(val habit: Habit = Habit())

// ---------- Logs ----------
data class HabitLog(
    val id: String = "",
    val habitId: String = "",
    val userId: String? = null,
    val completedAt: String = "",
    val status: String = "completed",
    val proofUrl: String? = null,
    val scheduledTime: String? = null,
    val habit: Habit? = null
)

data class LogsTodayResponse(val logs: List<HabitLog> = emptyList())
data class LogCreateResponse(val log: HabitLog? = null)

// ---------- Tasks ----------
data class Task(
    val id: String = "",
    val userId: String? = null,
    val title: String = "",
    val description: String? = null,
    val emoji: String = "",
    val dueDate: String? = null,
    val isScheduled: Boolean = true,
    val isEveryday: Boolean = false,
    val isActive: Boolean = true,
    val scheduledTime: String? = null,
    val scheduledDays: JsonElement? = null,
    val reminderMinutes: JsonElement? = null,
    val position: Int = 0,
    val createdAt: String? = null,
    val isCompletedToday: Boolean = false,
    val isDueToday: Boolean = true
)

data class TasksResponse(val tasks: List<Task> = emptyList(), val isOnVacation: Boolean = false)
data class TaskWrapper(val task: Task = Task())
data class TaskLog(val id: String = "", val taskId: String = "", val completedAt: String = "", val note: String? = null)

// ---------- Grid ----------
data class GridItem(val type: String = "", val title: String = "", val emoji: String? = null)
data class GridDay(
    val scheduled: Int = 0,
    val completed: Int = 0,
    val habits: Int = 0,
    val tasks: Int = 0,
    val items: List<GridItem> = emptyList()
)
data class GridResponse(val grid: Map<String, GridDay> = emptyMap(), val vacationDays: List<String> = emptyList())
data class GridYearsResponse(val years: List<Int> = emptyList())
data class GridDayDetailResponse(
    val date: String = "",
    val habits: List<HabitLog> = emptyList(),
    val tasks: List<TaskLogDetail> = emptyList(),
    val scheduledHabits: List<ScheduledHabitEntry> = emptyList(),
    val isOnVacation: Boolean = false
)
data class TaskLogDetail(val id: String = "", val taskId: String = "", val completedAt: String = "", val task: Task? = null)
data class ScheduledHabitEntry(val id: String = "", val title: String? = null, val emoji: String? = null, val completed: Boolean = false, val proofUrl: String? = null)

// ---------- Stats ----------
data class StatsOverview(
    val activeHabits: Int = 0,
    val todayLogs: Int = 0,
    val totalLogs: Int = 0,
    val bestStreak: Int = 0,
    val activeStreak: Int = 0,
    val consistency: Int = 0,
    val isOnVacation: Boolean = false
)

// ---------- Friends ----------
data class Friend(val id: String = "", val username: String = "", val avatar: String? = null, val bio: String? = null, val isPublic: Boolean = false)
data class FriendListResponse(val friends: List<Friend> = emptyList())
data class FriendSearchResponse(val users: List<Friend> = emptyList(), val results: List<Friend> = emptyList())
data class FriendRequestItem(val id: String = "", val status: String = "pending", val requester: Friend? = null, val receiver: Friend? = null, val createdAt: String? = null)
data class FriendRequestsResponse(val requests: List<FriendRequestItem> = emptyList(), val incoming: List<FriendRequestItem> = emptyList(), val outgoing: List<FriendRequestItem> = emptyList())
data class FriendProfileResponse(val user: Friend? = null, val habits: List<Habit>? = null, val isFriend: Boolean = false)
data class FeedItem(val id: String = "", val userId: String = "", val type: String = "", val createdAt: String = "", val user: Friend? = null, val payload: JsonElement? = null)

// ---------- Challenges ----------
data class Challenge(
    val id: String = "",
    val creatorId: String = "",
    val opponentId: String = "",
    val habitId: String = "",
    val title: String = "",
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val stake: String? = null,
    val status: String = "active",
    val winnerId: String? = null,
    val creator: Friend? = null,
    val opponent: Friend? = null,
    val habit: Habit? = null
)
data class ChallengesResponse(val challenges: List<Challenge> = emptyList())
data class ChallengeWrapper(val challenge: Challenge = Challenge())

// ---------- Presets ----------
data class Preset(
    val id: String = "",
    val authorId: String = "",
    val authorName: String? = null,
    val title: String = "",
    val description: String? = null,
    val emoji: String? = null,
    val category: String = "general",
    val frequencyType: String = "daily",
    val verificationType: String = "honor",
    val isPublished: Boolean = false,
    val likesCount: Int = 0,
    val forksCount: Int = 0,
    val usagesCount: Int = 0,
    val createdAt: String? = null
)
data class PresetsResponse(val presets: List<Preset> = emptyList())
data class PresetWrapper(val preset: Preset = Preset())
data class PresetDetailWrapper(val preset: Preset = Preset())

// ---------- Notifications ----------
data class AppNotification(
    val id: String = "",
    val type: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: String = "",
    val data: JsonElement? = null
)
data class NotificationsResponse(val notifications: List<AppNotification> = emptyList())
data class NotifPrefs(
    val morningEnabled: Boolean = true,
    val morningTime: String = "08:00",
    val habitRemindersEnabled: Boolean = true,
    val eveningEnabled: Boolean = true,
    val eveningTime: String = "21:00",
    val announcementsEnabled: Boolean = true
)

// ---------- Leaderboard ----------
data class LeaderboardEntry(val userId: String = "", val username: String = "", val avatar: String? = null, val score: Int = 0, val streak: Int = 0, val completions: Int = 0)
data class LeaderboardResponse(val leaderboard: List<LeaderboardEntry> = emptyList(), val entries: List<LeaderboardEntry> = emptyList())

// ---------- Vacation ----------
data class VacationStatus(val onVacation: Boolean = false, val vacation: JsonElement? = null)

// ---------- Assistant ----------
data class AssistantSettings(
    val enabled: Boolean = false,
    val confirmBeforeExecute: Boolean = true,
    val tasksLevel: Int = 2,
    val habitsLevel: Int = 2,
    val logsLevel: Int = 2,
    val statsLevel: Int = 1
)
data class ChatMessage(val id: String = "", val role: String = "", val content: String = "", val createdAt: String? = null)
data class ChatSession(val id: String = "", val title: String = "", val updatedAt: String? = null, val messages: List<ChatMessage>? = null)

// ---------- Admin ----------
data class AdminStats(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalHabits: Int = 0,
    val totalLogs: Int = 0
)

// ---------- Upload ----------
data class UploadResponse(val url: String = "", val fileUrl: String = "")

// ---------- Generic ----------
data class MessageResponse(val message: String? = null, val error: String? = null)
data class IdResponse(val id: String? = null)
