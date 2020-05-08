import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.finagle.http.Request
import com.twitter.finagle.Http
import com.twitter.finagle.http.HttpMuxer
import com.twitter.util.Await

object Foo extends App {
    val pingResponse = Response(Status.Ok)
    pingResponse.contentString = "pong"
    val pingService = Service.const(Future.value(pingResponse))

    val client = Http.client.newService("localhost:8888")

    val proxyService = Service.mk((req: Request) => client(Request("/ping")))

    val muxer = new HttpMuxer()
        .withHandler("/ping", pingService)
        .withHandler("/proxy", proxyService)

    val server = 
        Http.server.serve("localhost:8888", muxer)

    Await.ready(server)
}
