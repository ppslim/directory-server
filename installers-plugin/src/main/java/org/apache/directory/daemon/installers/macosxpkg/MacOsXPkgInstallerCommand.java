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
package org.apache.directory.daemon.installers.macosxpkg;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.directory.daemon.installers.AbstractMojoCommand;
import org.apache.directory.daemon.installers.GenerateMojo;
import org.apache.directory.daemon.installers.MojoHelperUtils;
import org.apache.directory.daemon.installers.Target;
import org.apache.directory.server.InstallationLayout;
import org.apache.directory.server.InstanceLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.taskdefs.Execute;


/**
 * PKG Installer command for creating Mac OS X packages.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MacOsXPkgInstallerCommand extends AbstractMojoCommand<MacOsXPkgTarget>
{
    /** The hdiutil utility executable */
    private File hdiutilUtility = new File( "/usr/bin/hdiutil" );


    /**
     * Creates a new instance of MacOsXPkgInstallerCommand.
     *
     * @param mojo
     *      the Server Installers Mojo
     * @param target
     *      the PKG target
     */
    public MacOsXPkgInstallerCommand( GenerateMojo mojo, MacOsXPkgTarget target )
    {
        super( mojo, target );
        initializeFilterProperties();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for macosx or the PackageMaker or hdiutil utilities can't be found.</li>
     *   <li>Creates the Mac OS X PKG Installer for Apache DS</li>
     *   <li>Package it in a Mac OS X DMG (Disk iMaGe)</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target
        if ( !verifyTarget() )
        {
            return;
        }

        log.info( "  Creating Mac OS X PKG installer..." );

        // Creating the target directory
        File targetDirectory = getTargetDirectory();
        targetDirectory.mkdirs();

        log.info( "    Copying PKG installer files" );

        // Creating the root directories hierarchy
        File pkgRootDirectory = new File( targetDirectory, "root" );
        pkgRootDirectory.mkdirs();
        File pkgRootUsrBinDirectory = new File( pkgRootDirectory, "usr/bin" );
        pkgRootUsrBinDirectory.mkdirs();
        File pkgRootUsrLocalApachedsDirectory = new File( pkgRootDirectory, "usr/local/apacheds-"
            + mojo.getProject().getVersion() );
        pkgRootUsrLocalApachedsDirectory.mkdirs();
        File pkgRootInstancesDirectory = new File( pkgRootUsrLocalApachedsDirectory, "instances" );
        pkgRootInstancesDirectory.mkdirs();
        File pkgRootInstancesDefaultDirectory = new File( pkgRootInstancesDirectory, "default" );
        pkgRootInstancesDefaultDirectory.mkdirs();
        File pkgRootInstancesDefaultConfDirectory = new File( pkgRootInstancesDefaultDirectory, "conf" );
        pkgRootInstancesDefaultConfDirectory.mkdirs();
        new File( pkgRootInstancesDefaultDirectory, "log" ).mkdirs();
        new File( pkgRootInstancesDefaultDirectory, "partitions" ).mkdirs();
        new File( pkgRootInstancesDefaultDirectory, "run" ).mkdirs();
        File pkgRootLibraryLaunchDaemons = new File( pkgRootDirectory, "Library/LaunchDaemons" );
        pkgRootLibraryLaunchDaemons.mkdirs();

        // Copying the apacheds files in the root directory
        try
        {
            // Creating the installation layout and copying files to it
            copyCommonFiles( mojo );

            // Copying the apacheds command to /usr/bin
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream(
                "apacheds-usr-bin.sh" ), new File( pkgRootUsrBinDirectory, "apacheds" ), true );

            // Copying the org.apache.directory.server.plist file to /Library/LaunchDaemons/
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream(
                "org.apache.directory.server.plist" ), new File( pkgRootLibraryLaunchDaemons,
                "org.apache.directory.server.plist" ), true );

            // Create Resources folder and sub-folder
            // Copying the resources files and Info.plist file needed for the 
            // generation of the PKG
            File pkgResourcesEnglishDirectory = new File( targetDirectory, "Resources/en.lproj" );
            pkgResourcesEnglishDirectory.mkdirs();
            File pkgScriptsDirectory = new File( targetDirectory, "scripts" );
            pkgScriptsDirectory.mkdirs();

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "pkg-background.tiff" ), new File(
                pkgResourcesEnglishDirectory, "background.tiff" ) );

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "License.rtf" ), new File(
                pkgResourcesEnglishDirectory, "License.rtf" ) );

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "Info.plist" ), new File( targetDirectory,
                "Info.plist" ) );

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "postflight" ), new File(
                pkgScriptsDirectory, "postflight" ) );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy PKG resources files." );
        }

        // Generating the PKG
        log.info( "    Generating Mac OS X PKG Installer" );
        Execute createPkgTask = new Execute();
        String[] cmd = new String[]
            { target.getPackageMakerUtility().getAbsolutePath(), "--root", "root/", "--resources", "Resources/",
                "--info", "Info.plist", "--title", "Apache Directory Server " + mojo.getProject().getVersion(),
                "--version", mojo.getProject().getVersion(), "--scripts", "scripts", "--out",
                "Apache Directory Server Installer.pkg" };
        createPkgTask.setCommandline( cmd );
        createPkgTask.setWorkingDirectory( targetDirectory );
        try
        {
            createPkgTask.execute();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the PKG: " + e.getMessage() );
        }

        log.info( "  Creating Mac OS X DMG..." );

        // Creating the disc image directory
        File dmgDirectory = new File( mojo.getOutputDirectory(), target.getId() + "-dmg" );
        dmgDirectory.mkdirs();

        log.info( "    Copying DMG files" );

        // Create dmg directory and its sub-directory
        File dmgDmgBackgroundDirectory = new File( dmgDirectory, "dmg/.background" );
        dmgDmgBackgroundDirectory.mkdirs();

        // Copying the files
        try
        {
            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "dmg-background.png" ), new File(
                dmgDirectory, "dmg/.background/background.png" ) );

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "DS_Store" ), new File( dmgDirectory,
                "dmg/.DS_Store" ) );

            MojoHelperUtils.copyFiles( new File( targetDirectory, "Apache Directory Server Installer.pkg" ), new File(
                dmgDirectory, "dmg/Apache Directory Server Installer.pkg" ) );

        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy DMG resources files." );
        }

        // Setting execution permission to the postflight script
        // (unfortunately, the execution permission has been lost after the 
        // copy of the PKG to the dmg folder)
        MojoHelperUtils.exec( new String[]
            { "chmod", "755",
                new File( dmgDirectory, "dmg/Apache Directory Server Installer.pkg/Contents/Resources/postflight" )
                    .toString() }, dmgDirectory, false );

        // Generating the DMG
        log.info( "    Generating Mac OS X DMG Installer" );
        String finalName = target.getFinalName();
        if ( !finalName.endsWith( ".dmg" ) )
        {
            finalName = finalName + ".dmg";
        }
        try
        {
            Execute createDmgTask = new Execute();
            createDmgTask.setCommandline( new String[]
                { hdiutilUtility.getAbsolutePath(), "makehybrid", "-quiet", "-hfs", "-hfs-volume-name",
                    "Apache Directory Server Installer", "-hfs-openfolder", "dmg/", "dmg/", "-o", "TMP.dmg" } );
            createDmgTask.setWorkingDirectory( dmgDirectory );
            createDmgTask.execute();

            createDmgTask.setCommandline( new String[]
                {
                    hdiutilUtility.getAbsolutePath(),
                    "convert",
                    "-quiet",
                    "-format",
                    "UDZO",
                    "TMP.dmg",
                    "-o",
                    "../" + finalName } );
            createDmgTask.execute();

        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the DMG: " + e.getMessage() );
        }

        log.info( "=> Mac OS X DMG generated at " + new File( mojo.getOutputDirectory(), finalName ) );
    }


    /**
     * Verifies the target.
     *
     * @return
     *      <code>true</code> if the target is correct, 
     *      <code>false</code> if not.
     */
    private boolean verifyTarget()
    {
        // Verifying the target is Mac OS X
        if ( !target.getOsName().equalsIgnoreCase( Target.OS_NAME_MAC_OS_X ) )
        {
            log.warn( "Mac OS X PKG installer can only be targeted for Mac OS X platform!" );
            log.warn( "The build will continue, but please check the the platform of this installer target." );
            return false;
        }

        // Verifying the PackageMaker utility exists
        if ( !target.getPackageMakerUtility().exists() )
        {
            log.warn( "Cannot find 'PackageMaker' utility at this location: " + target.getPackageMakerUtility() );
            log.warn( "The build will continue, but please check the location of your 'Package Maker' utility." );
            return false;
        }

        // Verifying the hdiutil utility exists
        if ( !hdiutilUtility.exists() )
        {
            log.warn( "Cannot find 'hdiutil' utility at this location: " + hdiutilUtility );
            log.warn( "The build will continue, but please check the location of your 'hdiutil' utility." );
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    protected void initializeFilterProperties()
    {
        super.initializeFilterProperties();

        filterProperties.put( "installation.directory", "/usr/local/apacheds-"
            + mojo.getProject().getVersion() );
        filterProperties.put( "instances.directory", "/usr/local/apacheds-"
            + mojo.getProject().getVersion() + "/instances" );
        filterProperties.put( "user", "root" );
        filterProperties.put( "wrapper.java.command", "# wrapper.java.command=<path-to-java-executable>" );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstallationDirectory()
    {
        return new File( getTargetDirectory(), "root/usr/local/apacheds-"
            + mojo.getProject().getVersion() );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstanceDirectory()
    {
        return new File( getInstallationDirectory(), "instances/default" );
    }

}