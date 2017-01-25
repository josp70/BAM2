import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._

name := "BAM2"

version := "1.0"

lazy val `bam2` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test,
                             "org.ddahl" %% "rscala" % "1.0.14", "org.rosuda.REngine" % "Rserve" % "1.8.1")

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

mappings in Universal ++= directory(baseDirectory.value / "scripts")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
