package net.cardnell.mkver

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppConfigSpec extends AnyFlatSpec with Matchers {

  "getBranchConfig for master" should "return master config" in {
    val branchConfig = AppConfig.getBranchConfig(None, "master")
    branchConfig.name should be("master")
  }

  "getBranchConfig for feat" should "return .* config" in {
    val branchConfig = AppConfig.getBranchConfig(None, "feat/f1")
    branchConfig.name should be(".*")
  }

  "mergeFormats" should "merge formats" in {
    val f1 = Format("f1", "v1")
    val f2 = Format("f2", "v2")
    val f3 = Format("f3", "v3")
    val f1b = Format("f1", "v4")
    val result = AppConfig.mergeFormats(List(f1b, f2), List(f1, f3))
    result should be((Formatter.builtInFormats ++ List(f1b, f2, f3)).sortBy(_.name))
  }

  "getPatchConfigs" should "return nothing in reference config" in {
    val branchConfig = BranchConfig(".*", "v", true, "Version", "release {Version}", "RC", Nil, Nil)
    val patchConfigs = AppConfig.getPatchConfigs(None, branchConfig)
    patchConfigs should be(Nil)
  }
}
