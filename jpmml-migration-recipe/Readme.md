JPPML migration recipe
======================

The jpmml-recipe contains code needded to migrate jpmml from version 1.5.1 to 1.6.4.

The main `JPMMLRecipe` features the OpenRewrite recipe chain-ability to re-use some already existing recipes, so that

1. It invokes `ChangeType` for classes that changed name/package, but kept the same method signature
2. It invokes `JPMMLCodeRecipe` for more fine-grained manipulation, e.g. removal of `FieldName` usage and replacement of `ScoreDistribution`
3. It invokes `RemoveUnusedImports` to remove unused imports


The `changed_types.properties` file is used to define the classes that must be replaced, and will be used to configure the `ChangeType` invocation.

The `changed_instantiation.properties` file is used to define cases where the declared variable must kept its original type, but only the instantiated object must be changed (e.g. `org.dmg.pmml.ScoreDistribution` -> `org.dmg.pmml.ComplexScoreDistribution`)

The `CommonTestingUtilities` has been thought to be re-usable by different recipes, even if currently defined in that module



