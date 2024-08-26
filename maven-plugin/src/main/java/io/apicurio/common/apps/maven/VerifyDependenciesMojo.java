package io.apicurio.common.apps.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Mojo(name = "verify-dependencies")
public class VerifyDependenciesMojo extends AbstractMojo {

    @Parameter(required = false, defaultValue = "compile,runtime")
    String scopes;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArtifactFilter filter = new ArtifactFilter() {
            @Override
            public boolean include(org.apache.maven.artifact.Artifact artifact) {
                return true;
            }
        };

        try {
            Set<String> matchingScopes = parseScopes(scopes);

            ProjectBuildingRequest buildingRequest =
                    new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            buildingRequest.setProject(project);
            DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, filter);

            Set<Artifact> invalidArtifacts = new TreeSet<>(new ArtifactComparator());

            DependencyNodeVisitor visitor = new DependencyNodeVisitor() {

                @Override
                public boolean visit(DependencyNode dependencyNode) {
                    Artifact artifact = dependencyNode.getArtifact();
                    if (matchingScopes.contains(artifact.getScope())) {
                        if (!isValid(artifact)) {
                            invalidArtifacts.add(artifact);
                        }
                    }
                    return true;
                }

                @Override
                public boolean endVisit(DependencyNode dependencyNode) {
                    return true;
                }
            };
            rootNode.accept(visitor);

            if (!invalidArtifacts.isEmpty()) {
                String serializedInvalidArtifacts = serialize(invalidArtifacts);
                throw new MojoFailureException("Invalid dependencies found: \n" + serializedInvalidArtifacts);
            }
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot build project dependency graph", e);
        }
    }

    private boolean isValid(Artifact artifact) {
        return artifact.getVersion().contains("-redhat-");
    }

    private static String serialize(Set<Artifact> invalidArtifacts) {
        StringBuilder sb = new StringBuilder();
        for (Artifact artifact : invalidArtifacts) {
            sb.append("    ");
            sb.append(artifact.getGroupId()).append(":").append(artifact.getArtifactId()).append(":").append(artifact.getVersion());
            sb.append(" [").append(artifact.getScope()).append("]");
            sb.append("\n");
        }
        return sb.toString();
    }

    private static Set<String> parseScopes(String scopes) {
        Set<String> rval = new HashSet<>();
        String[] splits = scopes.split(",");
        for (String split : splits) {
            split = split.trim();
            if (!split.isEmpty()) {
                rval.add(split);
            }
        }
        return rval;
    }

    private static class ArtifactComparator implements Comparator<Artifact> {

        @Override
        public int compare(Artifact a1, Artifact a2) {
            String a1s = a1.getGroupId() + ":" + a1.getArtifactId() + ":" + a1.getVersion();
            String a2s = a2.getGroupId() + ":" + a2.getArtifactId() + ":" + a2.getVersion();
            return a1s.compareTo(a2s);
        }
    }

}
