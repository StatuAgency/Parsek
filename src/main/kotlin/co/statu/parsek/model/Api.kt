package co.statu.parsek.model

import co.statu.parsek.error.BadRequest
import co.statu.parsek.error.InternalServerError
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.*
import io.vertx.ext.web.validation.ValidationHandler.REQUEST_CONTEXT_KEY
import io.vertx.json.schema.SchemaParser
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

abstract class Api : Route() {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Api::class.java)
    }

    override fun getHandler() = Handler<RoutingContext> { context ->
        CoroutineScope(context.vertx().dispatcher()).launch(getExceptionHandler(context)) {
            onBeforeHandle(context)

            val result = handle(context)

            result?.let { sendResult(it, context) }
        }
    }

    override fun getFailureHandler() = Handler<RoutingContext> { context ->
        CoroutineScope(context.vertx().dispatcher()).launch {
            var failure = context.failure()

            try {
                getFailureHandler(context)
            } catch (e: Error) {
                failure = e
            } catch (e: Exception) {
                failure = e
            }

            if (
                failure is BadRequestException ||
                failure is ParameterProcessorException ||
                failure is BodyProcessorException ||
                failure is RequestPredicateException
            ) {
                sendResult(BadRequest(), context, mapOf("bodyValidationError" to failure.message))

                return@launch
            }

            if (failure is IOException) {
                sendResult(BadRequest(), context, mapOf("inputError" to failure.message))

                return@launch
            }

            if (failure !is Result) {
                logger.error("Error on endpoint URL: {} {}", context.request().method(), context.request().path())
                sendResult(InternalServerError(), context)

                throw failure
            }

            sendResult(failure, context)
        }
    }

    private fun sendResult(
        result: Result,
        context: RoutingContext,
        extras: Map<String, Any?> = mapOf()
    ) {
        val response = context.response()

        if (response.ended()) {
            return
        }

        response
            .putHeader("content-type", "application/json; charset=utf-8")

        response.statusCode = result.getStatusCode()
        response.statusMessage = result.getStatusMessage()

        response.end(result.encode(extras))
    }

    private fun getExceptionHandler(context: RoutingContext) = CoroutineExceptionHandler { _, exception ->
        context.fail(exception)
    }

    fun getParameters(context: RoutingContext): RequestParameters = context.get(REQUEST_CONTEXT_KEY)

    abstract override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler?

    abstract suspend fun handle(context: RoutingContext): Result?

    open suspend fun getFailureHandler(context: RoutingContext) = Unit

    open suspend fun onBeforeHandle(context: RoutingContext) = Unit
}