package com.prezi.gradle.pride.cli

import com.prezi.gradle.pride.PrideException
import io.airlift.command.Option

/**
 * Created by lptr on 31/03/14.
 */
abstract class AbstractPrideCommand extends AbstractCommand {
	@Option(name = ["-p", "--pride-directory"], title = "directory",
			description = "Initializes the pride in the given directory instead of the current directory")
	private File explicitPrideDirectory

	protected File getPrideDirectory() {
		explicitPrideDirectory ?: new File(System.getProperty("user.dir"))
	}

	protected static PrideException invalidOptionException(String message, String option, String configuration) {
		return new PrideException("${message}. Either use ${option}, or set it in the global configuration (~/.prideconfig) as \"${configuration}\". See 'pride help config' for more information.")
	}
}
