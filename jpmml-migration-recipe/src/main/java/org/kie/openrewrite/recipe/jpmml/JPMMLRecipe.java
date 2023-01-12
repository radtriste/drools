package org.kie.openrewrite.recipe.jpmml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.RemoveUnusedImports;

public class JPMMLRecipe extends Recipe {



    private static Properties CHANGED_TYPES;

    static {
        try (InputStream input = JPMMLRecipe.class.getResourceAsStream ("/changed_types.properties")) {
            CHANGED_TYPES = new Properties();
            // load a properties file
            CHANGED_TYPES.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

   @JsonCreator
    public JPMMLRecipe() {
        CHANGED_TYPES.forEach((from, to) -> doNext(new ChangeType(from.toString(), to.toString(), true)));
        //doNext(new JPMMLCodeRecipe());
        doNext(new RemoveUnusedImports());
    }



    @Override
    public String getDisplayName() {
        return "JPMML Update recipe";
    }

    @Override
    public String getDescription() {
        return "Migrate JPMML version from 1.5.1 to 1.6.4.";
    }

}