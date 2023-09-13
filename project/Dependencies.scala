import sbt.*

object Dependencies {

  object Versions {
    val Akka                  = "2.8.4"
    val AkkaHttp              = "10.5.2"
    val Enumeratum            = "1.7.2"
    val Logback               = "1.4.7"
    val Prometheus            = "0.16.0"
    val ScalaCollectionCompat = "2.9.0"
    val ScalaLogging          = "3.9.5"
    val ScalaMock             = "5.2.0"
    val ScalaTest             = "3.2.15"
  }

  val AkkaHttp         = "com.typesafe.akka"          %% "akka-http"           % Versions.AkkaHttp
  val Enumeratum       = "com.beachape"               %% "enumeratum"          % Versions.Enumeratum
  val PrometheusCommon = "io.prometheus"               % "simpleclient_common" % Versions.Prometheus
  val ScalaLogging     = "com.typesafe.scala-logging" %% "scala-logging"       % Versions.ScalaLogging

  object Provided {
    val AkkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.Akka % "provided"
  }

  object Test {
    val AkkaHttpJson      = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.AkkaHttp   % "it,test"
    val AkkaHttpTestkit   = "com.typesafe.akka" %% "akka-http-testkit"    % Versions.AkkaHttp   % "it,test"
    val AkkaSlf4j         = "com.typesafe.akka" %% "akka-slf4j"           % Versions.Akka       % "it,test"
    val AkkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit"  % Versions.Akka       % "it,test"
    val AkkaTestkit       = "com.typesafe.akka" %% "akka-testkit"         % Versions.Akka       % "it,test"
    val Logback           = "ch.qos.logback"     % "logback-classic"      % Versions.Logback    % "it,test"
    val PrometheusHotspot = "io.prometheus"      % "simpleclient_hotspot" % Versions.Prometheus % "it,test"

    val ScalaCollectionCompat =
      "org.scala-lang.modules" %% "scala-collection-compat" % Versions.ScalaCollectionCompat % "it,test"
    val ScalaMock          = "eu.monniot"    %% "scala3mock"           % "0.3.0"            % "it,test"
    val ScalaMockScalaTest = "eu.monniot"    %% "scala3mock-scalatest" % "0.3.0"            % "it,test"
    val ScalaTest          = "org.scalatest" %% "scalatest"            % Versions.ScalaTest % "it,test"
  }

}
