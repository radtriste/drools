package org.kie.openrewrite.recipe.jpmml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;

public class JPMMLCodeRecipe extends Recipe {

    private static Properties CHANGED_INSTANTIATIONS;

    static {
        try (InputStream input = JPMMLRecipe.class.getResourceAsStream ("/changed_instantiation.properties")) {
            CHANGED_INSTANTIATIONS = new Properties();
            // load a properties file
            CHANGED_INSTANTIATIONS.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @JsonCreator
    public JPMMLCodeRecipe() {
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
        return new JPMMLVisitor(CHANGED_INSTANTIATIONS);
    }
}