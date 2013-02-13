This is a SBT plugin that provides an interface to the Flyway database
migration tool (<http://flywaydb.org>). The plugin is entirely self-contained
and does not require previous installation of Flyway to operate. It is
compatible with SBT 0.11.3 and Scala 2.9.1+

The current version of the plugin is *1.0*.


Quick Start
===========

1. Add flyway-sbt-plugin to your `project/plugins.sbt`:

    resolvers += "sean8223 Releases" at "https://github.com/sean8223/repository/raw/master/releases"

    addSbtPlugin("sean8223" %% "flyway-sbt-plugin" % PLUGIN_VERSION) // see above

2. In your `build.sbt`, do the following:
 
   * Inject the plugin settings into your build definition:

     `seq(flywaySettings:_*)`

   * Add your database driver to your list of libraryDependencies with "flyway" scope:

     `libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.22" % "flyway"`

   * Configure options for your environment:

         conf in Flyway := Map("driver" -> "com.mysql.jdbc.Driver",
                               "url" -> "jdbc:mysql://localhost:3306/foo",
                               "user" -> "root"
                               ...) 
						 
3. Test your configuration by running `flyway:info` from the SBT prompt.
	

Settings
========

The plugin exposes three settings:

* *flyway:conf* (`conf in Flyway` in build.sbt): a `Map[String, String]` 
  containg options to pass into Flyway. Refer to 
  <http://flywaydb.org/documentation/commandline/> for a full description
  of the options that can be passed into the conf object. The keys of this
  Map correspond to the the option name without the "flyway." prefix,
  e.g. "user" or "cleanOnValidationError".

* *flyway:migration-directories* (`migrationDirectories in Flyway` in 
  build.sbt): a `Seq[File]` containing roots on the classpath that contain
  migrations SQL and/or Java. By default, these are set to the value of
  `compile:resource-directory` (usually src/main/resources) and 
  `compile:class-directory` (usually target/_scala-version_/_sbt-version_/classes),
  but can be changed to suit your project layout as needed.

* *flyway:version* (`version in Flyway` in build.sbt): a `String`
  indicating the version of Flyway to use. The default value is "2.0.3".
  This plugin has not been tested extensively with other versions, YMMV.

If you need to make additional libraries available to Flyway, simply add
them to `libraryDependencies` under the "flyway" scope.


Tasks
=====

All tasks from the command tool are supported:

* *flyway:init*: Creates and initializes the metadata table in the schema.

* *flyway:clean*: Drops all objects in the schema without dropping the schema itself.

* *flyway:migrate*: Migrates the schema to the latest version. Flyway will create the metadata table automatically if it doesn't exist.

* *flyway:validate*: Validates the applied migrations against the ones available on the classpath. The build fails if differences in migration names, types or checksums are found.

* *flyway:info*: Prints the details and status information about all the migrations.

* *flyway:repair*: Repairs the Flyway metadata table after a failed migration. User objects left behind must still be cleaned up manually.
