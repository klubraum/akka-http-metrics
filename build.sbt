// General info

lazy val filterScalacOptions = { options: Seq[String] =>
  options.filterNot { o =>
    // get rid of value discard
    o == "-Ywarn-value-discard" || o == "-Wvalue-discard"
  }
}

// for sbt-github-actions
ThisBuild / crossScalaVersions := Seq("3.3.0")

lazy val commonSettings = Defaults.itSettings ++
  Seq(
    credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      "klubraum",
      System.getenv("GITHUB_TOKEN")
    ),
    organization := "com.klubraum",
    crossScalaVersions := (ThisBuild / crossScalaVersions).value,
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ~= filterScalacOptions,
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    publishMavenStyle := true,
    Test / publishArtifact := false,
    githubOwner := "klubraum",
    githubRepository := "akka-http-metrics"
  )

lazy val `akka-http-metrics` = (project in file("."))
  .aggregate(
    `akka-http-metrics-core`,
    `akka-http-metrics-prometheus`
  )
  .settings(commonSettings: _*)
  .settings(
    publishArtifact := false
  )

lazy val `akka-http-metrics-core` = (project in file("core"))
  .configs(IntegrationTest)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.AkkaHttp,
      Dependencies.Enumeratum,
      Dependencies.Provided.AkkaStream,
      Dependencies.Test.AkkaHttpTestkit,
      Dependencies.Test.AkkaSlf4j,
      Dependencies.Test.AkkaStreamTestkit,
      Dependencies.Test.Logback,
      Dependencies.Test.ScalaMock,
      Dependencies.Test.ScalaMockScalaTest,
      Dependencies.Test.ScalaTest
    )
  )

lazy val `akka-http-metrics-prometheus` = (project in file("prometheus"))
  .configs(IntegrationTest)
  .dependsOn(`akka-http-metrics-core`)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.PrometheusCommon,
      Dependencies.Provided.AkkaStream,
      Dependencies.Test.AkkaHttpTestkit,
      Dependencies.Test.AkkaSlf4j,
      Dependencies.Test.AkkaStreamTestkit,
      Dependencies.Test.AkkaTestkit,
      Dependencies.Test.Logback,
      Dependencies.Test.PrometheusHotspot,
      Dependencies.Test.ScalaTest
    )
  )
