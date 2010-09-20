/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.daemon.installers;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.directory.daemon.installers.archive.ArchiveInstallerCommand;
import org.apache.directory.daemon.installers.archive.ArchiveTarget;
import org.apache.directory.daemon.installers.bin.BinInstallerCommand;
import org.apache.directory.daemon.installers.bin.BinTarget;
import org.apache.directory.daemon.installers.deb.DebInstallerCommand;
import org.apache.directory.daemon.installers.deb.DebTarget;
import org.apache.directory.daemon.installers.macosxpkg.MacOsXPkgInstallerCommand;
import org.apache.directory.daemon.installers.macosxpkg.MacOsXPkgTarget;
import org.apache.directory.daemon.installers.nsis.NsisInstallerCommand;
import org.apache.directory.daemon.installers.nsis.NsisTarget;
import org.apache.directory.daemon.installers.rpm.RpmInstallerCommand;
import org.apache.directory.daemon.installers.rpm.RpmTarget;
import org.apache.directory.daemon.installers.solarispkg.SolarisPkgInstallerCommand;
import org.apache.directory.daemon.installers.solarispkg.SolarisPkgTarget;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;


/**
 * Maven 2 mojo creating the platform specific installation layout images.
 * 
 * @goal generate
 * @description Creates platform specific installation layout images.
 * @phase package
 * @requiresDependencyResolution runtime
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GenerateMojo extends AbstractMojo
{
    /**
     * The target directory into which the mojo creates os and platform 
     * specific images.
     * 
     * @parameter default-value="${project.build.directory}/installers"
     */
    private File outputDirectory;

    /**
     * The source directory where various configuration files for the installer 
     * are stored.
     * 
     * @parameter default-value="${project.basedir}/src/main/installers"
     */
    private File sourceDirectory;

    /**
     * The associated maven project.
     * 
     * @parameter expression="${project}" default-value="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The RPM installer targets.
     * 
     * @parameter
     */
    private RpmTarget[] rpmTargets;

    /**
     * The Mac OS X installer targets.
     * 
     * @parameter
     */
    private MacOsXPkgTarget[] macOsXPkgTargets;

    /**
     * The Solaris PKG installers targets.
     * 
     * @parameter
     */
    private SolarisPkgTarget[] solarisPkgTargets;

    /**
     * The NSIS installer targets.
     * 
     * @parameter
     */
    private NsisTarget[] nsisTargets;

    /**
     * The Debian installer targets.
     * 
     * @parameter
     */
    private DebTarget[] debTargets;

    /**
     * The Binary installer targets.
     * 
     * @parameter
     */
    private BinTarget[] binTargets;

    /**
     * The Archive installers targets.
     * 
     * @parameter
     */
    private ArchiveTarget[] archiveTargets;

    /**
     * The packages files.
     * 
     * @parameter
     */
    private PackagedFile[] packagedFiles;

    /**
     * The exclusions.
     * 
     * @parameter
     */
    private Set excludes;

    /** daemon bootstrapper */
    private Artifact bootstrapper;
    /** logging API need by bootstraper */
    private Artifact logger;
    /** commons-daemon dependency needed by native daemon */
    private Artifact daemon;

    private List<Target> allTargets;


    public void execute() throws MojoExecutionException, MojoFailureException
    {
        FileUtils.mkdir( outputDirectory.getAbsolutePath() );

        // Collecting all targets 
        initializeAllTargets();

        // Makes sure defaulted values are set to globals
        setDefaults();

        // bail if there is nothing to do 
        if ( allTargets.isEmpty() )
        {
            getLog().info( "-------------------------------------------------------" );
            getLog().info( "[installers:generate]" );
            getLog().info( "No installers to generate." );
            getLog().info( "-------------------------------------------------------" );
            return;
        }

        // report what we have to build 
        reportSetup();

        // search for and find the bootstrapper artifact
        //        setBootstrapArtifacts();

        // generate installers for all targets
        for ( Target target : allTargets )
        {
            // create the installation image first
            CreateImageCommand imgCmd = new CreateImageCommand( this, target );
            imgCmd.execute();

            // ---------------------------------------------------------------
            // Generate all installers
            // ---------------------------------------------------------------

            if ( target instanceof NsisTarget )
            {
                NsisInstallerCommand nsisCmd = null;
                nsisCmd = new NsisInstallerCommand( this, ( NsisTarget ) target );
                nsisCmd.execute();
            }

            if ( target instanceof RpmTarget )
            {
                RpmInstallerCommand rpmCmd = null;
                rpmCmd = new RpmInstallerCommand( this, ( RpmTarget ) target );
                rpmCmd.execute();
            }

            if ( target instanceof MacOsXPkgTarget )
            {
                MacOsXPkgInstallerCommand pkgCmd = null;
                pkgCmd = new MacOsXPkgInstallerCommand( this, ( MacOsXPkgTarget ) target );
                pkgCmd.execute();
            }

            if ( target instanceof SolarisPkgTarget )
            {
                SolarisPkgInstallerCommand pkgCmd = null;
                pkgCmd = new SolarisPkgInstallerCommand( this, ( SolarisPkgTarget ) target );
                pkgCmd.execute();
            }

            if ( target instanceof DebTarget )
            {
                DebInstallerCommand debCmd = null;
                debCmd = new DebInstallerCommand( this, ( DebTarget ) target );
                debCmd.execute();
            }

            if ( target instanceof BinTarget )
            {
                BinInstallerCommand binCmd = null;
                binCmd = new BinInstallerCommand( this, ( BinTarget ) target );
                binCmd.execute();
            }

            if ( target instanceof ArchiveTarget )
            {
                ArchiveInstallerCommand archiveCmd = null;
                archiveCmd = new ArchiveInstallerCommand( this, ( ArchiveTarget ) target );
                archiveCmd.execute();
            }
        }
    }


    /**
     * Initializes all targets.
     */
    private void initializeAllTargets()
    {
        allTargets = new ArrayList<Target>();

        addAllTargets( allTargets, nsisTargets );
        addAllTargets( allTargets, rpmTargets );
        addAllTargets( allTargets, debTargets );
        addAllTargets( allTargets, macOsXPkgTargets );
        addAllTargets( allTargets, solarisPkgTargets );
        addAllTargets( allTargets, binTargets );
        addAllTargets( allTargets, archiveTargets );
    }


    /**
     * Adds an array of targets to the given list.
     *
     * @param list
     *      the list of targets
     * @param array
     *      an array of targets
     */
    private void addAllTargets( List<Target> list, Target[] array )
    {
        if ( ( list != null ) && ( array != null ) )
        {
            list.addAll( Arrays.asList( array ) );
        }
    }


    private void setDefaults() throws MojoFailureException
    {
        if ( allTargets == null )
        {
            return;
        }

        // TODO FIXME
        //        if ( application.getName() == null )
        //        {
        //            throw new MojoFailureException( "Installed application name cannot be null." );
        //        }
        //
        //        if ( application.getCompany() == null )
        //        {
        //            if ( project.getOrganization() != null )
        //            {
        //                application.setCompany( project.getOrganization().getName() );
        //            }
        //            else
        //            {
        //                application.setCompany( "Apache Software Foundation" );
        //            }
        //        }
        //
        //        if ( application.getDescription() == null )
        //        {
        //            if ( project.getDescription() != null )
        //            {
        //                application.setDescription( project.getDescription() );
        //            }
        //            else
        //            {
        //                application.setDescription( "No description of this application is available." );
        //            }
        //        }
        //
        //        if ( project.getInceptionYear() != null )
        //        {
        //            application.setCopyrightYear( project.getInceptionYear() );
        //        }
        //
        //        if ( application.getUrl() == null )
        //        {
        //            if ( project.getUrl() != null )
        //            {
        //                application.setUrl( project.getUrl() );
        //            }
        //            else if ( project.getOrganization() != null )
        //            {
        //                application.setUrl( project.getOrganization().getUrl() );
        //            }
        //            else
        //            {
        //                application.setUrl( "http://www.apache.org" );
        //            }
        //        }
        //
        //        if ( application.getVersion() == null )
        //        {
        //            application.setVersion( project.getVersion() );
        //        }
        //
        //        if ( application.getMinimumJavaVersion() == null )
        //        {
        //            application.setMinimumJavaVersion( JavaEnvUtils.getJavaVersion() );
        //        }
        //
        //        if ( application.getAuthors() == null )
        //        {
        //            List<String> authors = new ArrayList<String>();
        //            @SuppressWarnings(value =
        //                { "unchecked" })
        //            List<Developer> developers = project.getDevelopers();
        //
        //            for ( Developer developer : developers )
        //            {
        //                if ( developer.getEmail() != null )
        //                {
        //                    authors.add( developer.getEmail() );
        //                }
        //                else
        //                {
        //                    authors.add( developer.getName() );
        //                }
        //            }
        //
        //            application.setAuthors( authors );
        //        }
        //
        //        if ( application.getEmail() == null )
        //        {
        //            if ( !project.getMailingLists().isEmpty() )
        //            {
        //                application.setEmail( ( ( MailingList ) project.getMailingLists().get( 0 ) ).getPost() );
        //            }
        //
        //            application.setEmail( "general@apache.org" );
        //        }
        //
        //        if ( application.getIcon() == null )
        //        {
        //            application.setIcon( new File( "src/main/installers/logo.ico" ) );
        //        }
        //
        //        if ( application.getReadme() == null )
        //        {
        //            application.setReadme( new File( "README" ) );
        //        }
        //
        //        if ( application.getLicense() == null )
        //        {
        //            application.setLicense( new File( "LICENSE" ) );
        //        }

        for ( Target target : allTargets )
        {
            // TODO FIXME
            //            if ( target.getApplication() == null )
            //            {
            //                target.setApplication( this.application );
            //            }

            if ( target.getLoggerConfigurationFile() == null )
            {
                target.setLoggerConfigurationFile( new File( sourceDirectory, "log4j.properties" ) );
            }

            if ( target.getBootstrapperConfigurationFile() == null )
            {
                target.setBootstrapperConfigurationFile( new File( sourceDirectory, "bootstrapper.properties" ) );
            }

            if ( target.getServerConfigurationFile() == null )
            {
                target.setServerConfigurationFile( new File( sourceDirectory, "server.xml" ) );
            }

            if ( target.getOsVersion() == null )
            {
                target.setOsVersion( "*" );
            }
        }
    }


    //    private void setBootstrapArtifacts() throws MojoFailureException
    //    {
    //        Artifact artifact = null;
    //        Iterator artifacts = project.getDependencyArtifacts().iterator();
    //
    //        while ( artifacts.hasNext() )
    //        {
    //            artifact = ( Artifact ) artifacts.next();
    //            if ( artifact.getArtifactId().equals( BOOTSTRAPPER_ARTIFACT_ID )
    //                && artifact.getGroupId().equals( BOOTSTRAPPER_GROUP_ID ) )
    //            {
    //                getLog().info( "Found bootstrapper dependency with version: " + artifact.getVersion() );
    //                bootstrapper = artifact;
    //            }
    //            else if ( artifact.getArtifactId().equals( LOGGER_ARTIFACT_ID )
    //                && artifact.getGroupId().equals( LOGGER_GROUP_ID ) )
    //            {
    //                getLog().info( "Found logger dependency with version: " + artifact.getVersion() );
    //                logger = artifact;
    //            }
    //            else if ( artifact.getArtifactId().equals( DAEMON_ARTIFACT_ID )
    //                && artifact.getGroupId().equals( DAEMON_GROUP_ID ) )
    //            {
    //                getLog().info( "Found daemon dependency with version: " + artifact.getVersion() );
    //                daemon = artifact;
    //            }
    //        }
    //
    //        if ( bootstrapper == null )
    //        {
    //            throw new MojoFailureException( "Bootstrapper dependency artifact required: " + BOOTSTRAPPER_GROUP_ID + ":"
    //                + BOOTSTRAPPER_ARTIFACT_ID );
    //        }
    //        if ( logger == null )
    //        {
    //            throw new MojoFailureException( "Logger dependency artifact required: " + LOGGER_GROUP_ID + ":"
    //                + LOGGER_ARTIFACT_ID );
    //        }
    //        if ( daemon == null )
    //        {
    //            throw new MojoFailureException( "Daemon dependency artifact required: " + DAEMON_GROUP_ID + ":"
    //                + DAEMON_ARTIFACT_ID );
    //        }
    //    }

    public void reportSetup()
    {
        getLog().info( "-------------------------------------------------------" );
        getLog().info( "[installers:generate]" );
        getLog().info( "sourceDirectory = " + sourceDirectory );
        getLog().info( "outputDirectory = " + outputDirectory );
        getLog().info( "---------------------- allTargets ---------------------" );

        if ( allTargets != null )
        {
            boolean isFirst = true;

            for ( Target target : allTargets )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    getLog().info( "" );
                }

                getLog().info( "id: " + target.getId() );
                getLog().info( "osName: " + target.getOsName() );
                getLog().info( "osArch: " + target.getOsArch() );
                getLog().info( "osVersion: " + target.getOsVersion() );
                getLog().info( "daemonFramework: " + target.getDaemonFramework() );
                getLog().info( "loggerConfigurationFile: " + target.getLoggerConfigurationFile() );
                getLog().info( "bootstrapperConfigurationFiles: " + target.getBootstrapperConfigurationFile() );
                getLog().info( "serverConfigurationFile: " + target.getServerConfigurationFile() );
            }
        }

        getLog().info( "-------------------------------------------------------" );
    }


    public File getOutputDirectory()
    {
        return outputDirectory;
    }


    public Artifact getBootstrapper()
    {
        return bootstrapper;
    }


    public Artifact getDaemon()
    {
        return daemon;
    }


    public Artifact getLogger()
    {
        return logger;
    }


    public MavenProject getProject()
    {
        return project;
    }


    public Set getExcludes()
    {
        return this.excludes;
    }


    public File getSourceDirectory()
    {
        return this.sourceDirectory;
    }


    public void setPackagedFiles( PackagedFile[] packagedFiles )
    {
        this.packagedFiles = packagedFiles;
    }


    public PackagedFile[] getPackagedFiles()
    {
        return packagedFiles;
    }
}