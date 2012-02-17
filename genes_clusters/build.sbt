
name := "hiv24"

organization := "pityu"

version := "1.01"

unmanagedBase <<= baseDirectory { base => base / "../lib" }





scalacOptions += "-deprecation"

scalacOptions += "-unchecked"
