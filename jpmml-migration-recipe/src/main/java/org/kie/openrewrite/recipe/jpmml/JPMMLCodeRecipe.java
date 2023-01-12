package org.kie.openrewrite.recipe.jpmml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;

public class JPMMLCodeRecipe extends Recipe {

    @Option(displayName = "Old fully-qualified type name",
            description = "Fully-qualified class name of the original instantiated type.",
            example = "org.dmg.pmml.ScoreDistribution")
    @NotNull
    String oldInstantiatedFullyQualifiedTypeName;

    @Option(displayName = "New fully-qualified type name",
            description = "Fully-qualified class name of the replacement type. The `OuterClassName$NestedClassName` naming convention should be used for nested classes.",
            example = "org.dmg.pmml.ComplexScoreDistributions")
    @NotNull
    String newInstantiatedFullyQualifiedTypeName;

    @JsonCreator
    public JPMMLCodeRecipe(@NotNull @JsonProperty("oldInstantiatedFullyQualifiedTypeName") String oldInstantiatedFullyQualifiedTypeName,
                           @NotNull @JsonProperty("newInstantiatedFullyQualifiedTypeName") String newInstantiatedFullyQualifiedTypeName) {
        this.oldInstantiatedFullyQualifiedTypeName = oldInstantiatedFullyQualifiedTypeName;
        this.newInstantiatedFullyQualifiedTypeName = newInstantiatedFullyQualifiedTypeName;
    }



    @Override
    public String getDisplayName() {
        return "JPMML Update Code recipe";
    }

    @Override
    public String getDescription() {
        return "Migrate JPMML Code version from 1.5.1 to 1.6.4.";
    }



    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new JPMMLVisitor(oldInstantiatedFullyQualifiedTypeName, newInstantiatedFullyQualifiedTypeName);
    }



}