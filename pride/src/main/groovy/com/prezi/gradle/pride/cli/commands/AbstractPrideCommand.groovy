package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.cli.CliConfiguration
import com.prezi.gradle.pride.vcs.Vcs
import com.prezi.gradle.pride.vcs.VcsManager
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
abstract class AbstractPrideCommand extends AbstractCommand {
	@Option(name = ["-p", "--pride-directory"], title = "directory",
			description = "Initializes the pride in the given directory instead of the current directory")
	private File explicitPrideDirectory

	private VcsManager vcsManager

	protected File getPrideDirectory() {
		explicitPrideDirectory ?: new File(System.getProperty("user.dir"))
	}

	protected VcsManager getVcsManager() {
		if (vcsManager == null) {
			vcsManager = new VcsManager()
		}
		return vcsManager
	}

	protected Vcs getVcs() {
		return getVcs(configuration.getString(CliConfiguration.REPO_TYPE_DEFAULT))
	}

	protected Vcs getVcs(String repoType) {
		return getVcsManager().getVcs(repoType, configuration)
	}
}
