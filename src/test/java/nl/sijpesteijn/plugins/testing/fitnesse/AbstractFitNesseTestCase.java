package nl.sijpesteijn.plugins.testing.fitnesse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import nl.sijpesteijn.testing.fitnesse.plugins.utils.DependencyResolver;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class AbstractFitNesseTestCase extends AbstractMojoTestCase {

    protected static final String LOG4J_JAR = "log4j/log4j/1.2.15/log4j-1.2.15.jar";
    protected static final String JUNIT_JAR = "junit/junit/4.8.2/junit-4.8.2.jar";
    protected static final String FITNESSE_JAR = "org/fitnesse/fitnesse/20111025/fitnesse-20111025.jar";
    protected static final String TARGET_CLASSES = "target/classes";
    protected static final String FITNESSE_ROOT = "FitNesseRoot";
    protected static final String TARGET = "/target";

    private String testDirectory;
    protected String REPO;
    protected Model model;

    @Override
    protected void setUp() throws Exception {
        testDirectory = new File("").getAbsolutePath().replace("\\", "/");
        REPO = getRepositoryDirectory();
        final File pom = new File(getBasedir(), "pom.xml");
        final MavenXpp3Reader reader = new MavenXpp3Reader();
        model = reader.read(new InputStreamReader(new FileInputStream(pom)), true);
        final Properties properties = model.getProperties();
        properties.put("basedir", getBasedir());
        super.setUp();
    }

    public String getTestDirectory() {
        return testDirectory;
    }

    public String getRepositoryDirectory() {
        String repoDir = System.getenv("MAVEN_REPO");
        if (repoDir != null) {
            return repoDir.replace('\\', '/');
        }

        repoDir = System.getProperty("basedir");

        if (repoDir == null) {
            repoDir = new File("").getAbsolutePath();
        }

        return repoDir.replace('\\', '/');

    }

    public Xpp3Dom getPluginConfiguration(final String artifactId, final String goal) throws MojoExecutionException {
        final List<Plugin> plugins = model.getBuild().getPlugins();
        for (final Plugin plugin : plugins) {
            if (plugin.getArtifactId().equals(artifactId)) {
                final Xpp3Dom configuration = getConfiguration(plugin, goal);
                if (configuration != null) {
                    return configuration;
                }
            }
        }
        throw new MojoExecutionException("Could not find plugin configuration for artifactId: " + artifactId
                + ", goal: " + goal);
    }

    private Xpp3Dom getConfiguration(final Plugin plugin, final String goal) {
        final List<PluginExecution> executions = plugin.getExecutions();
        if (executions != null) {
            for (final PluginExecution execution : executions) {
                if (hasGoal(execution.getGoals(), goal)) {
                    return (Xpp3Dom) plugin.getConfiguration();
                }
            }
        }
        return null;
    }

    private boolean hasGoal(final List<String> goals, final String goalToFind) {
        if (goals != null) {
            for (final String goal : goals) {
                if (goal.equals(goalToFind)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String[] getStringArrayFromConfiguration(final Xpp3Dom configuration, final String name) {
        final Xpp3Dom child = configuration.getChild(name);
        final String[] statics = new String[child.getChildCount()];
        for (int i = 0; i < child.getChildCount(); i++) {
            statics[i] = resolvePlaceHolder(child.getChild(i).getValue());
        }
        return statics;
    }

    private String resolvePlaceHolder(final String value) {
        final int start = value.indexOf("${");
        final int stop = value.indexOf("}");
        if (start > -1 && stop > -1) {
            final String propertyName = getPropertyValue(value.substring(start + 2, stop));
            if (propertyName != null) {
                return value.substring(0, start) + propertyName + value.substring(stop + 1, value.length());
            }
        }
        return value;
    }

    private String getPropertyValue(final String propertyName) {
        final Properties properties = model.getProperties();
        return properties.getProperty(propertyName);
    }

    protected List<String> getClasspathElements(final List<Dependency> dependencies) {
        final DependencyResolver resolver = new DependencyResolver();
        final List<String> classpathElements = new ArrayList<String>();
        for (final Dependency dependency : dependencies) {
            classpathElements.add(resolver.resolveDependencyPath(dependency, getRepositoryDirectory()));
        }
        return classpathElements;
    }

    protected Dependency[] getDependencyArrayFromConfiguration(final Xpp3Dom configuration, final String name) {
        final Xpp3Dom child = configuration.getChild(name);
        final Dependency[] dependencies = new Dependency[child.getChildCount()];
        for (int i = 0; i < child.getChildCount(); i++) {
            final Xpp3Dom excludeDependency = child.getChild(i);
            final Dependency dependency = new Dependency();
            for (int j = 0; j < excludeDependency.getChildCount(); j++) {
                final Xpp3Dom xpp3Dom = excludeDependency.getChild(j);
                if (xpp3Dom.getName().equals("groupId")) {
                    dependency.setGroupId(xpp3Dom.getValue());
                }
                if (xpp3Dom.getName().equals("artifactId")) {
                    dependency.setArtifactId(xpp3Dom.getValue());
                }
                if (xpp3Dom.getName().equals("version")) {
                    dependency.setVersion(xpp3Dom.getValue());
                }
                if (xpp3Dom.getName().equals("classifier")) {
                    dependency.setClassifier(xpp3Dom.getValue());
                }
                if (xpp3Dom.getName().equals("type")) {
                    dependency.setType(xpp3Dom.getValue());
                }
            }
            dependencies[i] = dependency;
        }
        return dependencies;
    }

    protected String getStringValueFromConfiguration(final Xpp3Dom configuration, final String name,
                                                     final String defaultValue)
    {
        final Xpp3Dom child = configuration.getChild(name);
        if (child != null) {
            return resolvePlaceHolder(child.getValue());
        }
        return resolvePlaceHolder(defaultValue);
    }
}
