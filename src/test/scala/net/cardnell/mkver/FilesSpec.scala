package net.cardnell.mkver

import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, assertM, suite, testM}

object FilesSpec extends DefaultRunnableSpec {
  val fs = java.io.File.separator

  def spec = suite("Files")(
    testM("format should replace variables") {
      assertM(Path("../").map(_.path.toFile))(equalTo(java.nio.file.Paths.get("../").toFile))
    },
    testM("glob specific file") {
      val result = for {
        p <- Path.currentWorkingDirectory
        files <- Files.glob(p, "build.sbt")
        list = files.map(_.path.toString)
      } yield list
      assertM(result)(equalTo(List("build.sbt")))
    },
    testM("glob files in sub directories") {
      val result = for {
        p <- Path.currentWorkingDirectory
        files <- Files.glob(p, "*.sbt")
        list = files.map(_.path.toString)
      } yield list
      assertM(result)(equalTo(List("build.sbt")))
    },
    testM("glob wildcard in directory") {
      val result = for {
        p <- Path.currentWorkingDirectory
        files <- Files.glob(p, "**/*.sbt")
        list = files.map(_.path.toString)
      } yield list
      assertM(result)(hasSameElements(List(s"project${fs}plugins.sbt")))
    },
    testM("glob wildcard in all directories") {
      val result = for {
        p <- Path.currentWorkingDirectory
        files <- Files.glob(p, "***.sbt")
        list = files.map(_.path.toString)
      } yield list
      assertM(result)(hasSameElements(List(s"project${fs}plugins.sbt", "build.sbt")))
    },
  )
}
