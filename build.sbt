ThisBuild / organization := "com.pinkstack"
ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file(".")).settings(
  name := "facetrace",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect"        % "3.5.3",
    "org.typelevel" %% "cats-effect-kernel" % "3.5.3",
    "org.typelevel" %% "cats-effect-std"    % "3.5.3",
    "co.fs2"        %% "fs2-core"           % "3.9.4",
    "co.fs2"        %% "fs2-io"             % "3.9.4",
    "com.monovore"  %% "decline-effect"     % "2.4.1",

    // Logging
    "org.typelevel" %% "log4cats-core"   % "2.6.0",
    "org.typelevel" %% "log4cats-slf4j"  % "2.6.0",
    "ch.qos.logback" % "logback-classic" % "1.4.14",

    // OpenCV
    "org.bytedeco" % "javacv-platform" % "1.5.9"
  )
)
