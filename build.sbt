name := """play-zio"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"
val silencerVersion = "1.6.0"

libraryDependencies ++= Seq(
  "dev.zio"                  %% "zio"    % "1.0.0-RC18-2",
  "com.softwaremill.macwire" %% "macros" % "2.3.3" % Provided,
  compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
)

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8", // yes, this is 2 args
  "-target:jvm-1.8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Ywarn-numeric-widen"
//  "-Xfatal-warnings"
)

scalacOptions.in(Compile) ++= (routes.in(Compile).value ++ TwirlKeys.compileTemplates.in(Compile).value)
  .map(f => s"-P:silencer:pathFilters=$f")

scalafmtOnCompile := true

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
