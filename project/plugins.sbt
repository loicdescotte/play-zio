// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.1")

val neoScalafmtVersion = "1.15"
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % neoScalafmtVersion)
addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % neoScalafmtVersion)
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "2.0.0-RC6-1")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.5")
