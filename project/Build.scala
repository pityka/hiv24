import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "hiv24-web"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "pityu" %% "commons" % "0.1"      
    )

  val scalatest = "org.scalatest" %% "scalatest_2.9.0" % "1.6" % "test" from "http://www.scala-tools.org/repo-snapshots/org/scalatest/scalatest_2.9.0/1.6-SNAPSHOT/scalatest_2.9.0-1.6-SNAPSHOT.jar"

   



    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here    
  
    ) dependsOn (genesClusters)


    lazy val genesClusters = Project(
    id = "genes_clusters",
    base = file("genes_clusters/"),
    settings = Defaults.defaultSettings ++ Seq (libraryDependencies ++= appDependencies ++ Seq(scalatest))
    )

}
