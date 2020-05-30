//import java.io.File

name := "ScalaFX Ensemble"

version := "1.14.1-SNAPSHOT"

organization := "org.scalafx"

val dottyVersion = "0.24.0-RC1"
val scala213Version = "2.13.1"
scalaVersion := dottyVersion

// To cross compile with Dotty and Scala 2
crossScalaVersions := Seq(dottyVersion, scala213Version)

libraryDependencies ++= Seq(
  "org.scalafx" % "scalafx_2.13" % "14-R19",
  "org.scala-lang.modules" % "scala-xml_2.13" % "1.3.0",
  "org.scalafx" % "scalafx-extras_2.13" % "0.3.4",
  "org.scalatest" % "scalatest_2.13" % "3.1.2"
)

// Add OS specific JavaFX dependencies
val javafxModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux") => "linux"
  case n if n.startsWith("Mac") => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}
libraryDependencies ++= javafxModules.map(m => "org.openjfx" % s"javafx-$m" % "14.0.1" classifier osName)

resolvers += Opts.resolver.sonatypeSnapshots

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

// Sources should also be copied to output, so the sample code, for the viewer,
// can be loaded from the same file that is used to execute the example
unmanagedResourceDirectories in Compile += baseDirectory(_ / "src/main/scala").value

// Set the prompt (for this build) to include the project id.
shellPrompt := { state => System.getProperty("user.name") + ":" + Project.extract(state).currentRef.project + "> " }

// Run in separate VM, so there are no issues with double initialization of JavaFX
fork := true

fork in Test := true

// Create file used to determine available examples at runtime.
resourceGenerators in Compile += Def.task {
  /** Scan source directory for available examples
    * Return pairs 'directory' -> 'collection of examples in that directory'.
    */
  def loadExampleNames(inSourceDir: File): Array[(String, Array[String])] = {
    val examplesDir = "/scalafx/ensemble/example/"
    val examplePath = new File(inSourceDir, examplesDir)
    val exampleRootFiles = examplePath.listFiles()
    for (dir <- exampleRootFiles if dir.isDirectory) yield {
      val leaves = for (f <- dir.listFiles() if f.getName.contains(".scala")) yield {
        f.getName.stripSuffix(".scala").stripPrefix("Ensemble")
      }
      dir.getName.capitalize -> leaves.sorted
    }
  }

  /** Create file representing names and directories for all availabe examples.
    * It will be loaded by the application at runtime and used to popolate example tree.
    */
  def generateExampleTreeFile(inSourceDir: File,
                              outSourceDir: File,
                              templatePath: String): Seq[File] = {
    val exampleDirs = loadExampleNames(inSourceDir)
    val contents = exampleDirs.map { case (dir, leaves) => dir + " -> " + leaves.mkString(", ") }.mkString("\n")
    val outFile = new File(outSourceDir, templatePath)
    IO.write(outFile, contents)

    Seq(outFile)
  }

  generateExampleTreeFile(
    (scalaSource in Compile).value,
    (resourceManaged in Compile).value,
    "/scalafx/ensemble/example/example.tree"
  )
}.taskValue

mainClass in Compile := Some("scalafx.ensemble.Ensemble")
