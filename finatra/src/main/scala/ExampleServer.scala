import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import javax.inject.Inject
import com.twitter.finagle.Http


object ExampleServerMain extends ExampleServer

class ExampleServer extends HttpServer {

  
//   override val modules: Seq[Module] = Seq(
    // ExampleModule)

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .add[ExampleController]
  }

}

object ExampleController {
  val client =
    Http.client.newService("localhost:8888", "proxy-client")
}

class ExampleController extends Controller {

  get("/ping") { request: Request =>
    "pong"
  }

  get("/pingproxy") { request: Request =>
    ExampleController.client.apply(Request("/ping"))
  }

}