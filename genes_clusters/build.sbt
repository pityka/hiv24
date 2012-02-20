
name := "hiv24"

organization := "pityu"

version := "1.01"

unmanagedBase <<= baseDirectory { base => base / "../lib" }

resolvers += Resolver.file("Project local repo", file("repository/local/"))(Resolver.ivyStylePatterns)




scalacOptions += "-deprecation"

scalacOptions += "-unchecked"
