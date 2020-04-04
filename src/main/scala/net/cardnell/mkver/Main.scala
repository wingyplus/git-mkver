package net.cardnell.mkver

import better.files._
import net.cardnell.mkver.MkVer._
import net.cardnell.mkver.CommandLineArgs.{CommandLineOpts, NextOpts, PatchOpts, TagOpts}

case class ProcessResult(stdout: String, stderr: String, exitCode: Int)

case class MkVerError(message: String)

object Main {
  def main(args: Array[String]): Unit = {
    new Main().mainImpl(args) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(message) =>
        println(message)
    }
  }
}

class Main(git: Git.Service = Git.Live.git()) {
  def mainImpl(args: Array[String]): Either[MkVerError, String] = {
    CommandLineArgs.mkverCommand.parse(args, sys.env)
      .fold( help => Left(MkVerError(help.toString())), opts => run(opts))
  }

  def run(opts: CommandLineOpts): Either[MkVerError, String] = {
    git.checkGitRepo().flatMap { _ =>
      val currentBranch = git.currentBranch()
      val config = AppConfig.getBranchConfig(opts.configFile, currentBranch)
      opts.p match {
        case nextOps@NextOpts(_) =>
          runNext(nextOps, config, currentBranch)
        case TagOpts(_) =>
          runTag(config, currentBranch).map(_ => "")
        case PatchOpts(_) =>
          val patchConfigs = AppConfig.getPatchConfigs(opts.configFile, config)
          runPatch(config, currentBranch, patchConfigs).map(_ => "")
      }
    }
  }

  def runNext(nextOpts: NextOpts, config: BranchConfig, currentBranch: String): Either[MkVerError, String] = {
    getNextVersion(git, config, currentBranch).flatMap { nextVersion =>
      nextOpts.format.map { format =>
        Right(Formatter(nextVersion, config).format(format))
      }.getOrElse {
        formatTag(config, nextVersion)
      }
    }
  }

  def runTag(config: BranchConfig, currentBranch: String): Either[MkVerError, Unit] = {
    getNextVersion(git, config, currentBranch).map { nextVersion =>
      formatTag(config, nextVersion).map { tag =>
        val tagMessage = Formatter(nextVersion, config).format(config.tagMessageFormat)
        if (config.tag && nextVersion.commitCount > 0) {
          git.tag(tag, tagMessage)
        }
      }
    }
  }

  def runPatch(config: BranchConfig, currentBranch: String, patchConfigs: List[PatchConfig]): Either[MkVerError, Unit] = {
    getNextVersion(git, config, currentBranch).map { nextVersion =>
      patchConfigs.foreach { patch =>
        val regex = patch.find.r
        val replacement = Formatter(nextVersion, config).format(patch.replace)
        patch.filePatterns.foreach { filePattern =>
          File.currentWorkingDirectory.glob(filePattern, includePath = false).foreach { file =>
            println(s"patching: $file, replacement: $replacement")
            val newContent = regex.replaceAllIn(file.contentAsString, replacement)
            file.overwrite(newContent)
          }
        }
      }
    }
  }
}
