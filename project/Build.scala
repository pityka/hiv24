import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "hiv24"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "pityu" %% "hiv24" % "1.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here    
      resolvers += Resolver.file("Local ivy ~/.ivy2/local", file("/Users/pityka/.ivy2/local"))(Resolver.ivyStylePatterns)
  
    )

}
