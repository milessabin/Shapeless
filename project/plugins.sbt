scalacOptions += "-deprecation"
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.26"

addSbtPlugin("com.typesafe"                      % "sbt-mima-plugin"       % "0.3.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-osgi"              % "0.9.5")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"         % "0.9.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-git"               % "1.0.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"           % "0.6.27")
addSbtPlugin("com.github.gseitz"                 % "sbt-release"           % "1.0.11")
addSbtPlugin("com.jsuereth"                      % "sbt-pgp"               % "1.1.2")
addSbtPlugin("org.xerial.sbt"                    % "sbt-sonatype"          % "2.3")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"      % "2.1.0")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"         % "1.5.1")
addSbtPlugin("org.scala-native"                  % "sbt-scala-native"      % "0.3.8")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject" % "0.6.0")
addSbtPlugin("org.portable-scala"                % "sbt-scala-native-crossproject" % "0.6.0")
