package nl.sijpesteijn.testing.fitnesse.plugins.managers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import nl.sijpesteijn.testing.fitnesse.plugins.pluginconfigs.StarterPluginConfig;
import nl.sijpesteijn.testing.fitnesse.plugins.utils.CommandRunner;
import nl.sijpesteijn.testing.fitnesse.plugins.utils.DependencyResolver;
import nl.sijpesteijn.testing.fitnesse.plugins.utils.FirstTimeWriter;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Plugin manager responsible for starting FitNesse.
 * 
 */
public class StarterPluginManager implements PluginManager {

	private final StarterPluginConfig starterPluginConfig;
	private final DependencyResolver resolver = new DependencyResolver();

	/**
	 * 
	 * @param starterPluginConfig
	 *            {@link nl.sijpesteijn.testing.fitnesse.plugins.pluginconfigs.StarterPluginConfig}
	 */
	public StarterPluginManager(final StarterPluginConfig starterPluginConfig) {
		this.starterPluginConfig = starterPluginConfig;
	}

	/**
	 * Start FitNesse
	 * 
	 * @throws MojoFailureException
	 *             , MojoExecutionException
	 */
	@Override
	public void run() throws MojoExecutionException, MojoFailureException {
		String jarLocation;
		jarLocation = resolver.getJarLocation(starterPluginConfig.getDependencies(), "org/fitnesse",
				starterPluginConfig.getBaseDir());
		final String jvmArgumentsString = getJVMArguments(starterPluginConfig.getJvmArguments());
		final String dependencyList = getDependencyList();
		final String command = "java"
				+ jvmArgumentsString
				+ " -cp "
				+ jarLocation
				+ File.pathSeparatorChar
				+ (dependencyList + " fitnesseMain.FitNesseMain -p " + starterPluginConfig.getFitNessePort() + " -d "
						+ starterPluginConfig.getWikiRoot() + " -r " + starterPluginConfig.getNameRootPage()
						+ getLogArgument() + " -e " + starterPluginConfig.getRetainDays());

		final CommandRunner runner = new CommandRunner(starterPluginConfig.getLog());
		try {
			runner.start(command, true, " days.\n");
			if (runner.errorBufferContains("patient.")) {
				new FirstTimeWriter(starterPluginConfig.getWikiRoot() + File.separatorChar
						+ starterPluginConfig.getNameRootPage());
			}
		} catch (final IOException e) {
			throw new MojoExecutionException("Could not start fitnesse.", e);
		} catch (final InterruptedException e) {
			throw new MojoExecutionException("Could not start fitnesse.", e);
		}
		if (runner.getExitValue() != 0 && runner.errorBufferHasContent()) {
			throw new MojoFailureException("Could not start FitNesse: " + runner.getErrorBufferMessage());
		}

	}

	private String getLogArgument() {
		if (starterPluginConfig.getLogPath() != null && !starterPluginConfig.getLogPath().equals("")) {
			return " -l " + starterPluginConfig.getLogPath();
		}
		return "";
	}

	/**
	 * Return the list with jvm dependencies.
	 * 
	 * @return {@link java.lang.String}
	 */
	private String getDependencyList() {
		if (starterPluginConfig.getJvmDependencies() == null) {
			return "";
		}
		String list = "";
		for (final Dependency dependency : starterPluginConfig.getJvmDependencies()) {
			final String dependencyPath = resolver.resolveDependencyPath(dependency, starterPluginConfig.getBaseDir());
			if (!dependencyPath.trim().equals("")) {
				list += dependencyPath;
			}
		}
		return list;
	}

	/**
	 * Return the list of jvm arguments.
	 * 
	 * @param arguments
	 *            {@link java.util.List}
	 * @return {@link java.lang.String}
	 */
	private String getJVMArguments(final List<String> arguments) {
		if (arguments == null) {
			return "";
		}
		String list = "";
		for (final String argument : arguments) {
			list += "-D" + argument + " ";
		}
		return list;
	}
}
