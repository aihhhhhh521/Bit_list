package com.example.software_project

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Query
import retrofit2.http.*

interface ApiService {
    // User-related endpoints
    @POST("/auth/getCaptcha")
    suspend fun sendCode(@Body request: VerifyCodeRequest): Response<Boolean>

    @POST("auth/verifyCode")
    suspend fun verifyCode(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    //---------新增接口-----------
    @PUT("user/changePassword")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Boolean>
    //---------------------------

    @GET("user/profile")
    suspend fun getProfile(): Response<User>

    @GET("users/{userId}") // <-- 新增接口：假设这是后端提供的新端点
    suspend fun getUserProfileById(@Path("userId") userId: Int): Response<User>

    @PUT("user/update")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<Boolean>

    // --- 修改此接口 ---
    @GET("user/stats")
    suspend fun getUserStats(
        @Query("userId") userId: Int, // <-- 修改: String -> Int
        @Query("trendLength") trendLength: Int,   // 新增：用于完成率趋势图的长度，例如 12 代表最近12周/月
        @Query("taskDateRange") taskDateRange: Int, // 新增：用于任务分布图的时间范围，例如 7 代表最近7天
        @Query("timeAllocationPeriod") timeAllocationPeriod: String // 新增参数，如 "daily", "weekly", "tags"
    ): Response<UserStatsResponse>

    // Task-related endpoints
    @POST("tasks")
    suspend fun createTask(@Body task: Task): Response<Task>

    @GET("tasks")
    suspend fun getTasks(@Query("userId") userId: Int): Response<List<Task>>

    @PUT("tasks/{taskId}")
    suspend fun updateTask(@Path("taskId") taskId: Int, @Body task: Task): Response<Boolean>

    @DELETE("tasks/{taskId}")
    suspend fun deleteTask(@Path("taskId") taskId: Int): Response<Boolean>

    @PUT("tasks/{taskId}/complete")
    suspend fun markTaskAsCompleted(@Path("taskId") taskId: Int): Response<Boolean>

    @Multipart
    @POST("tasks/{taskId}/attachments/upload")
    suspend fun uploadAttachment(
        @Path("taskId") taskId: Int,
        @Part file: MultipartBody.Part
    ): Response<Attachment>

    @Streaming
    @GET("tasks/{taskId}/attachments/{attachmentId}/download")
    suspend fun downloadAttachment(@Path("taskId") taskId: Int, @Path("attachmentId") attachmentId: Int): Response<ResponseBody>

    @DELETE("tasks/{taskId}/attachments/{attachmentId}")
    suspend fun deleteAttachment(@Path("taskId") taskId: Int, @Path("attachmentId") attachmentId: Int): Response<Boolean>

    // --- 新增接口：用于管理附件权限 ---
    @PUT("tasks/{taskId}/attachments/{attachmentId}/permissions")
    suspend fun updateAttachmentPermissions(
        @Path("taskId") taskId: Int,
        @Path("attachmentId") attachmentId: Int,
        @Body request: UpdateAttachmentPermissionsRequest
    ): Response<Boolean>
    // --- 新增接口结束 ---

    @POST("tasks/{taskId}/attachments/{attachmentId}/restore")
    suspend fun restoreAttachment(@Path("taskId") taskId: Int, @Path("attachmentId") attachmentId: Int): Response<Boolean>

    @PUT("tasks/reorder")
    suspend fun reorderTasks(@Body tasks: List<Task>): Response<Boolean>

    // Team-related endpoints
    @POST("teams")
    suspend fun createTeam(@Body team: Team): Response<Team>

    @GET("teams")
    suspend fun getTeams(@Query("userId") userId: Int): Response<List<Team>>

    @PUT("teams/{teamId}")
    suspend fun updateTeam(@Path("teamId") teamId: Int, @Body team: Team): Response<Boolean>

    @DELETE("teams/{teamId}")
    suspend fun deleteTeam(@Path("teamId") teamId: Int): Response<Boolean>

    @POST("teams/{teamId}/join")
    suspend fun joinTeam(@Path("teamId") teamId: Int, @Query("userId") userId: Int): Response<Boolean>

    @POST("teams/{teamId}/tasks/{taskId}/assign")
    suspend fun assignTask(@Path("teamId") teamId: Int, @Path("taskId") taskId: Int, @Query("userId") userId: Int): Response<Boolean>

    // --- 新增接口：用于管理团队成员 ---
    @DELETE("teams/{teamId}/members/{userId}")
    suspend fun removeMemberFromTeam(
        @Path("teamId") teamId: Int,
        @Path("userId") userId: Int
    ): Response<Boolean>

    @PUT("teams/{teamId}/members/{userId}/role")
    suspend fun updateMemberRole(
        @Path("teamId") teamId: Int,
        @Path("userId") userId: Int,
        @Body request: UpdateMemberRoleRequest
    ): Response<Boolean>
    // --- 新增接口结束 ---

    // 番茄钟相关接口（新增）
    @POST("user/focus")
    suspend fun submitFocusData(@Body focusData: TomatoFocusData): Response<Boolean>
}