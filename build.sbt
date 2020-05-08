
val finagleVersion = "20.4.1"

lazy val exampleServerSettings = Seq(
  fork in run := true,
  javaOptions in Test ++= Seq(
    // we are unable to guarantee that Logback will not get picked up b/c of coursier caching
    // so we set the Logback System properties in addition to the slf4j-simple and the
    // the Framework test logging disabled property.
    "-Dlog.service.output=/dev/stdout",
    "-Dlog.access.output=/dev/stdout",
    "-Dlog_level=OFF",
    "-Dorg.slf4j.simpleLogger.defaultLogLevel=off",
    "-Dcom.twitter.inject.test.logging.disabled"
  ),
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-http" % finagleVersion exclude ("org.checkerframework", "checker-compat-qual") exclude ("io.netty", "netty-tcnative-boringssl-static"),
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  ),
  excludeDependencies ++= Seq(
    // commons-logging is replaced by jcl-over-slf4j
    ExclusionRule(organization = "commons-logging", name = "commons-logging")
  ),
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case "module-info.class" => MergeStrategy.last
    case "META-INF/io.netty.versions.properties" => MergeStrategy.last
    case other => MergeStrategy.defaultMergeStrategy(other)
  }

)


// libraryDependencies += "com.twitter" %% "finatra-httpclient" % finagleVersion exclude ("org.checkerframework", "checker-compat-qual") exclude ("io.netty", "netty-tcnative-boringssl-static")
// graalVMNativeImageGraalVersion := Some("20.0.0")

name := "test-finatra"

lazy val root = (project in file ("."))
  .aggregate(finagle, finatra)
    

lazy val finagle = (project in file ("finagle"))
  .enablePlugins(GraalVMNativeImagePlugin)
  .settings(exampleServerSettings)
  .settings(
    graalVMNativeImageOptions ++= Seq(
      // "-H:Optimize=0", //Faster compilation iteration
      "--no-server",
      "--verbose",
      "-Dio.netty.noUnsafe=true",
      "-H:+TraceClassInitialization",
      "-H:+ReportExceptionStackTraces",
      // Initialize slf4j and logback at build time, I would expect these libs to be made graal compatible already
      "--initialize-at-build-time=org.slf4j",
      "--initialize-at-build-time=ch.qos.logback",
      "--initialize-at-build-time=com.fasterxml.jackson.annotation.JsonProperty$Access",
      // These two don't seme to have an effect
      "--initialize-at-run-time=io.netty.handler.codec.http2.Http2ServerUpgradeCodec",    
      // Could not find cause of CallConstruct being initialized so instead of delaying to to runtime, initialize whole of scala at build time
      "--initialize-at-build-time=scala",
      // Make sure that we get the reason for failing to generate the native image
      "--report-unsupported-elements-at-runtime",
      "--allow-incomplete-classpath",
      // Apparently these four classes cause an UnsatisfiedLinkError which can be solved by delaying init to runtime
      "--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop,io.netty.channel.unix.Errors,io.netty.channel.unix.IovArray,io.netty.channel.unix.Socket",
      "--no-fallback",
      "--enable-http"
      // s"-H:ReflectionConfigurationFiles=${baseDirectory.value}/src/main/resources/reflect-config.json",
    )
  )

lazy val finatra = (project in file ("finatra"))
  .enablePlugins(GraalVMNativeImagePlugin)
  .settings(exampleServerSettings)
  .settings(libraryDependencies ++= 
      Seq(
        "com.twitter" %% "twitter-server-logback-classic" % finagleVersion exclude ("io.netty", "netty-tcnative-boringssl-static"),
        "com.twitter" %% "finatra-http" % finagleVersion exclude ("org.checkerframework", "checker-compat-qual") exclude ("io.netty", "netty-tcnative-boringssl-static"),
        "com.twitter" %% "finatra-httpclient" % finagleVersion exclude ("org.checkerframework", "checker-compat-qual") exclude ("io.netty", "netty-tcnative-boringssl-static"),
    )
  )
  .settings(
      graalVMNativeImageOptions ++= Seq(
        // "--initialize-at-build-time",
        // "--initialize-at-run-time=io.netty.buffer.UnpooledByteBufAllocator$InstrumentedUnpooledDirectByteBuf,io.netty.buffer.UnpooledDirectByteBuf,io.netty.buffer.UnpooledByteBufAllocator$InstrumentedUnpooledDirectByteBuf,io.netty.handler.codec.http2.Http2CodecUtil,io.netty.buffer.AbstractReferenceCountedByteBuf",    
        
        // "--initialize-at-build-time=io.netty.handler",
        // "--initialize-at-build-time=io.netty.buffer",
        // "--initialize-at-build-time=io.netty.buffer.UnpooledUnsafeDirectByteBuf",  
        // "-H:CLibraryPath=/Users/alessandro/Downloads/netty-tcnative-boringssl-static-2.0.30.Final-osx-x86_64/META-INF/native",
        "-H:IncludeResources=.*/.*\\.properties",
        "-H:IncludeResources=META-INF/services",
        // "-Djava.library.path=/Users/alessandro/Downloads/netty-tcnative-boringssl-static-2.0.30.Final-osx-x86_64/META-INF/native",
        "--verbose",
        "-Dio.netty.noUnsafe=true",
        // "-H:-UseServiceLoaderFeature",
        "-H:+TraceClassInitialization",
        "-H:+ReportExceptionStackTraces",
        // Initialize slf4j and logback at build time, I would expect these libs to be made graal compatible already
        "--initialize-at-build-time=org.slf4j",
        "--initialize-at-build-time=ch.qos.logback",
        // These two don't seme to have an effect
        "--initialize-at-run-time=com.twitter.server.AdminHttpServer.reflMethod$Method1",    
        "--initialize-at-run-time=io.netty.handler.codec.http2.Http2ServerUpgradeCodec",    
        // Could not find cause of CallConstruct being initialized so instead of delaying to to runtime, initialize whole of scala at build time
        "--initialize-at-build-time=scala",
        // Make sure that we get the reason for failing to generate the native image
        "--report-unsupported-elements-at-runtime",
        "--allow-incomplete-classpath",
        // Apparently these four classes cause an UnsatisfiedLinkError which can be solved by delaying init to runtime
        "--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop,io.netty.channel.unix.Errors,io.netty.channel.unix.IovArray,io.netty.channel.unix.Socket",
        "--no-fallback",
        // "--initialize-at-build-time=scala.runtime.EmptyMethodCache",
        // "--initialize-at-build-time=ch.qos.logback,com.fasterxml.jackson,fresh.graal,io.netty,io.reactivex,org.reactivestreams,org.slf4j"
        s"-H:ReflectionConfigurationFiles=${baseDirectory.value}/src/native/reflectconfig.json",
    )
  )
  



