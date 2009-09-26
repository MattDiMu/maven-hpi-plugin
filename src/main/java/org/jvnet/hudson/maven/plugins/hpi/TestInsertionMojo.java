package org.jvnet.hudson.maven.plugins.hpi;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;

/**
 * Insert default test suite.
 *
 * @author Kohsuke Kawaguchi
 * @goal insert-test
 * @phase generate-test-sources
 */
public class TestInsertionMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     */
    protected MavenProject project;

    /**
     * If true, the automatic test injection will be skipped.
     *
     * @parameter
     */
    private boolean disabledTestInjection;

    private static String quote(String s) {
        return '"'+s+'"';
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        String target = HpiUtil.findHudsonVersion(project);
        if (new VersionNumber(target).compareTo(new VersionNumber("1.326"))<0) {
            getLog().info("Skipping auto-test generation because we are targeting Hudson "+target);
            return;
        }
        if (disabledTestInjection) {
            getLog().info("Skipping auto-test generation");
            return;
        }

        try {
            File f = new File(project.getBasedir(), "target/inject-tests");
            f.mkdirs();
            File javaFile = new File(f, "InjectedTest.java");
            PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(javaFile)));
            w.println("import java.util.*;");
            w.println("/**");
            w.println(" * Entry point to auto-generated tests (generated by maven-hpi-plugin).");
            w.println(" * If this fails to compile, you are probably using Hudson &lt; 1.326. If so, disable");
            w.println(" * this code generation by configuring maven-hpi-plugin to &lt;disabledTestInjection>false&lt;/disabledTestInjection>.");
            w.println(" */");
            w.println("public class InjectedTest extends junit.framework.TestCase {");
            w.println("  public static junit.framework.Test suite() throws Exception {");
            w.println("    Map parameters = new HashMap();");
            w.println("    parameters.put(\"basedir\","+quote(project.getBasedir().getAbsolutePath())+")");
            w.println("    parameters.put(\"artifactId\","+quote(project.getArtifactId())+")");
            w.println("    parameters.put(\"outputDirectory\","+quote(project.getBuild().getOutputDirectory())+")");
            w.println("    parameters.put(\"testOutputDirectory\","+quote(project.getBuild().getTestOutputDirectory())+")");
            w.println("    return new org.jvnet.hudson.test.PluginAutomaticTestBuilder.build(parameters)");
            w.println("  }");
            w.println("}");
            w.close();
            project.addTestCompileSourceRoot(f.getAbsolutePath());

            // always set the same time stamp on this file, so that Maven will not re-compile this
            // every time we run this mojo.
            javaFile.setLastModified(0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
