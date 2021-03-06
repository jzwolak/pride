package com.prezi.gradle.pride.cli.commands

import com.prezi.gradle.pride.Pride
import com.prezi.gradle.pride.PrideException
import com.prezi.gradle.pride.cli.CliConfiguration
import com.prezi.gradle.pride.cli.PrideInitializer
import io.airlift.command.Command
import io.airlift.command.Option
import org.apache.commons.configuration.Configuration

/**
 * Created by lptr on 31/03/14.
 */
@Command(name = "init", description = "Initialize pride")
class InitCommand extends AbstractPrideCommand {

	@Option(name = ["-f", "--force"],
			description = "Force initialization of a pride, even if one already exists")
	private boolean overwrite

	@Option(name = ["-T", "--repo-type"],
			title = "type",
			description = "Repository type (used to identify the type of any existing repos)")
	private String explicitRepoType

	@Option(name = "--no-add-existing",
			description = "Do not add existing modules in the pride directory to the pride")
	boolean explicitNoAddExisting

	@Override
	public void run() {
		if (!overwrite && Pride.containsPride(prideDirectory)) {
			throw new PrideException("A pride already exists in ${prideDirectory}")
		}
		def pride = PrideInitializer.create(prideDirectory, configuration, vcsManager)
		def vcs = getVcs()

		if (!explicitNoAddExisting) {
			log.debug "Adding existing modules"
			def addedAny = false
			prideDirectory.eachDir { File dir ->
				if (Pride.isValidModuleDirectory(dir)) {
					log.info "Addign existing ${vcs.type} module in ${dir}"
					pride.addModule(dir.name, vcs)
					addedAny = true
				}
			}
			if (addedAny) {
				pride.save()
				PrideInitializer.reinitialize(pride)
			}
		}
	}

	@Override
	protected void overrideConfiguration(Configuration configuration) {
		super.overrideConfiguration(configuration)
		if (explicitRepoType) {
			configuration.setProperty(CliConfiguration.REPO_TYPE_DEFAULT, explicitRepoType)
		}
	}
}
