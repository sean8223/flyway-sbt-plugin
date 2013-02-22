// Copyright 2013 Sean Wellington
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package sean8223

import sbt._
import sbt.Keys._
import java.io.File
import java.io.FileWriter
import java.util.Properties
import scala.collection.JavaConversions._

object FlywayPlugin extends Plugin {

  // our configuration

  val Flyway = config("flyway")

  // task keys

  val clean = TaskKey[Unit]("clean", "Drops all objects in the schema without dropping the schema itself.")

  val init = TaskKey[Unit]("init", "Creates and initializes the metadata table in the schema.")

  val migrate = TaskKey[Unit]("migrate", "Migrates the schema to the latest version. Flyway will create the metadata table automatically if it doesn't exist.")

  val validate = TaskKey[Unit]("validate", "Validates the applied migrations against the ones available on the classpath. The build fails if differences in migration names, types or checksums are found.")

  val info = TaskKey[Unit]("info", "Prints the details and status information about all the migrations.")

  val repair = TaskKey[Unit]("repair", "Repairs the Flyway metadata table after a failed migration. User objects left behind must still be cleaned up manually.")

  // setting keys

  val flywayMigrationDirectories = SettingKey[Seq[File]]("flyway-migration-directories", "Directory roots containing migrations (both SQL- and Java-based).")

  val flywayOptions = SettingKey[Map[String, Any]]("flyway-options", "Flyway options.")

  val flywayVersion = SettingKey[String]("flyway-version", "Flyway version.")

  // exported keys

  val flywaySettings = inConfig(Flyway)(Seq(

    managedClasspath <<= (classpathTypes, update) map { 
      (ct, u) => {
	// java migrations require all reachable classes to 
	// be on flyway's classpath, or else the scanner
	// will fail with ClassNotFoundExceptions
	Classpaths.managedJars(Compile, ct, u) ++ 
	Classpaths.managedJars(Runtime, ct, u) ++ 
	Classpaths.managedJars(Provided, ct, u) ++
	Classpaths.managedJars(Flyway, ct, u) 
      }
    },

    clean <<= (streams,
	       baseDirectory,
	       managedClasspath in Flyway,
	       flywayMigrationDirectories,
	       flywayOptions) map { 
      (s, bd, mcp, md, o) => {
	executeFlyway(s.log, bd, mcp, md, o, "clean")
      } 
    },

    init <<= (streams,
	      baseDirectory,
	      managedClasspath in Flyway,
	      flywayMigrationDirectories,
	      flywayOptions) map { 
      (s, bd, mcp, md, o) => {
	executeFlyway(s.log, bd, mcp, md, o, "init")
      } 
    },

    migrate <<= (streams,
		 baseDirectory,
		 managedClasspath in Flyway,
		 flywayMigrationDirectories,
		 flywayOptions) map { 
      (s, bd, mcp, md, o) => {
	executeFlyway(s.log, bd, mcp, md, o, "migrate")
      } 
    },

    validate <<= (streams,
		  baseDirectory,
		  managedClasspath in Flyway,
		  flywayMigrationDirectories,
		  flywayOptions) map { 
      (s, bd, mcp, md, o) => {
	executeFlyway(s.log, bd, mcp, md, o, "validate")
      }
    },

    info <<= (streams,
	      baseDirectory,
	      managedClasspath in Flyway,
	      flywayMigrationDirectories,
	      flywayOptions) map { 
      (s, bd, mcp, md, o) => {
	executeFlyway(s.log, bd, mcp, md, o, "info")
      } 
    },

    repair <<= (streams,
		baseDirectory,
		managedClasspath in Flyway,
		flywayMigrationDirectories,
		flywayOptions) map { 
      (s, bd, mcp, md, o) => {
	executeFlyway(s.log, bd, mcp, md, o, "repair")
      }
    }
    

  )) ++ Seq(

    flywayMigrationDirectories <<= (resourceDirectory in Compile, classDirectory in Compile) apply {
      (rd, cd) => {
	Seq(rd, cd)
      }
    },

    flywayVersion := "2.0.3",

    flywayOptions := Map(),

    libraryDependencies <++= (scalaVersion, flywayVersion) apply {
      (sv, fv) => { 
	Seq("org.scala-lang" % "scala-library" % sv % Flyway.name,
	    "com.googlecode.flyway" % "flyway-commandline" % fv % Flyway.name,
	    "com.googlecode.flyway" % "flyway-core" % fv % Flyway.name) 
      }
    },

    ivyConfigurations += Flyway

  )

  def generateClasspathArgument(log:Logger, classpath:Seq[Attributed[File]], otherLocations:Seq[File]) = {
    val cp = (classpath.map { _.data.getAbsolutePath } ++ otherLocations).mkString(System.getProperty("path.separator"))
    log.debug("Classpath is " + cp)
    cp
  }

  def generateConfiguration(log:Logger, options:Map[String, Any]):File = {
    val tmp = File.createTempFile("flyway", ".properties")
    tmp.deleteOnExit
    val props = new Properties 
    props ++= options.collect {
      case (key, Some(x:String))     => ("flyway." + key, x);
      case (key, x @ Seq(first, _*)) => ("flyway." + key, x.mkString(","));
      case (key, x)                  => ("flyway." + key, x.toString)
    }
    val fw = new FileWriter(tmp)
    try {
      props.store(fw, "Automatically generated by flyway-plugin")
    }
    finally {
      fw.close
    }
    log.debug("Wrote flyway configuration to " + tmp.getAbsolutePath)
    tmp
  }

  def executeFlyway(log:Logger, baseDirectory:File, managedClasspath:Seq[Attributed[File]], migrationDirectories:Seq[File], options:Map[String, Any], command:String) {
    val flywayConfigFile = generateConfiguration(log, options)
    val classpathArgument = generateClasspathArgument(log, managedClasspath, migrationDirectories)
    val cmdLine = Seq("java", "-classpath", classpathArgument, "com.googlecode.flyway.commandline.Main", "-configFile=" + flywayConfigFile.getAbsolutePath(), command)
    val rc = Process(cmdLine, baseDirectory) ! log
    rc match {
      case 0 => ;
      case x => error("Failed with return code: " + x)
    }
  }

}
