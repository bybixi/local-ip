package com.localip.app.core.util

import kotlinx.coroutines.CancellationException

/**
 * suspend 版 [runCatching]：取消异常原样上抛，其余异常收进 [Result]。
 * 在协程里直接用 runCatching 会把 CancellationException 一起吞掉，
 * 导致 Job.cancel() 后协程"卡"在看似失败的 Result 上而不是真正退出。
 */
suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        Result.failure(t)
    }
