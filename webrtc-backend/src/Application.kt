import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.lang.Thread.sleep
import java.time.Duration
import java.util.*


/**
 * Originally written by Artem Bagritsevich.
 *
 * https://github.com/artem-bagritsevich/WebRTCKtorSignalingServerExample
 */
fun main(args: Array<String>): Unit = EngineMain.main(args)
enum class MessageType {
    STATE,
    OFFER,
    ANSWER,
    ICE,
    NAME
}

@Suppress("unused") // Referenced in application.conf
@JvmOverloads
fun Application.module(testing: Boolean = false) {
    var receivedAnswer = false;
    var answerSDP:String? = null;
    val mutex = Mutex()



    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {

        get("/") {
            call.respond("Hello from WebRTC signaling server")
        }

        post("/"){
            val clientName = call.request.headers["Client-Name"]
            val bytes = call.receive<ByteArray>()
            var hopperMessage = String(bytes)
            println("Recieved offer")
            println(hopperMessage)
            hopperMessage = "OFFER $hopperMessage"
            val uuid = clientName?.let { it1 -> SessionManager.findClient(it1) }
            var response: String? = null;
            if(uuid == null){
                response = "{\"sucess\":0,\"message\":\"\"}"
            }
            else {
                //val sessionId = SessionManager.clients[uuid]

                SessionManager.handleOffer(uuid,hopperMessage)
                println("Waiting for answer")
                while (!receivedAnswer) {
                    println("Inside while")
                    withContext(Dispatchers.IO) {
                        sleep(100)
                    }
                }
                response = "{\"suceess\":1,\"message\":$answerSDP,\"}"
                println(" recev answer")
            }
            println("Sending response")
            println(response)
            answerSDP?.let { it1 -> call.respondText(it1) }
    }
        webSocket("/rtc") {
            val sessionID = UUID.randomUUID()
            try {
                SessionManager.onSessionStarted(sessionID, this)
                //TODO: check if client present or not


                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            var message = frame.readText();
                            SessionManager.onMessage(sessionID, frame.readText())
                                if (message.startsWith(MessageType.ANSWER.toString(), true)) {
                                    //println(message)
                                    answerSDP = message.substringAfter(' ')
                                    //println(answerSDP)
                                    receivedAnswer = true
                                }
                        }

                        else -> Unit
                    }
                }
                println("Exiting incoming loop, closing session: $sessionID")
                SessionManager.onSessionClose(sessionID)
            } catch (e: ClosedReceiveChannelException) {
                println("onClose $sessionID")
                SessionManager.onSessionClose(sessionID)
            } catch (e: Throwable) {
                println("onError $sessionID $e")
                SessionManager.onSessionClose(sessionID)
            }
        }
    }

}

