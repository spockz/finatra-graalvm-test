import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.finagle.http.Request
import com.twitter.finagle.Http
import com.twitter.finagle.http.HttpMuxer
import com.twitter.util.Await
import java.util.concurrent.atomic.AtomicReference
import com.twitter.finagle.ListeningServer

object Foo extends App {
    // Simple pong service
    val pingResponse = Response(Status.Ok)
    pingResponse.contentString = "pong"
    val pingService = Service.const(Future.value(pingResponse))

    // Client to talk to ourselves so we can hit the client code paths as well
    val client = Http.client.newService("localhost:8888")

    // Call ourselves on the /ping endpoint for all /proxy requests
    val proxyService = Service.mk((req: Request) => client(Request("/ping")))

    // Create a placholder for the service and a shutdown service so we can automatically and manually shutdown the server
    val atomicServer = new AtomicReference[ListeningServer]()
    val shutdownService = Service.mk { (req: Request) => 
        Option(atomicServer.get).map(_.close()).getOrElse(Future.Unit).map(_ => Response())
    }

    // Combine the three services into a single one with path matching.
    val muxer = new HttpMuxer()
        .withHandler("/ping", pingService)
        .withHandler("/proxy", proxyService)
        .withHandler("/shutdown",  shutdownService)

    // Start the local server and store in the placeholder
    val server = 
        Http.server.serve("localhost:8888", muxer)
    atomicServer.set(server)

    // Warmup on run to hit the JIT and more code paths, and perhaps also native-image generation
    (1 to 100).foreach { _ =>
        Await.ready(Future.collect((1 to 1000).map{_ => proxyService(Request())}))
        Thread.sleep(10)
    }

    // Automatically shutdown when requested
    if (sys.props.contains("autoShutdown")) {
        Await.ready(client(Request("/shutdown")))
    }

    // Wait until the server stops
    Await.ready(server)
}
