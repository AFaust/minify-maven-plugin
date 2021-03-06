/*
 * $Id$
 *
 * Minify Maven Plugin
 * https://github.com/samaxes/minify-maven-plugin
 *
 * Copyright (c) 2009 samaxes.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samaxes.maven.minify.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;
import com.samaxes.maven.minify.common.JavaScriptErrorReporter;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * Task for merging and compressing JavaScript files.
 */
public class ProcessJSFilesTask extends ProcessFilesTask {

    private final String jsEngine;

    private final boolean munge;

    private final boolean verbose;

    private final boolean preserveAllSemiColons;

    private final boolean disableOptimizations;

    /**
     * Task constructor.
     *
     * @param log Maven plugin log
     * @param bufferSize size of the buffer used to read source files
     * @param debug show source file paths in log output
     * @param skipMerge whether to skip the merge step or not
     * @param skipMinify whether to skip the minify step or not
     * @param jsEngine minify processor engine selected
     * @param webappSourceDir web resources source directory
     * @param webappTargetDir web resources target directory
     * @param inputDir directory containing source files
     * @param sourceFiles list of source files to include
     * @param sourceIncludes list of source files to include
     * @param sourceExcludes list of source files to exclude
     * @param outputDir directory to write the final file
     * @param outputFilename the output file name
     * @param suffix final filename suffix
     * @param nosuffix whether to use a suffix for the minified filename or not
     * @param charset if a character set is specified, a byte-to-char variant allows the encoding to be selected.
     *        Otherwise, only byte-to-byte operations are used
     * @param linebreak split long lines after a specific column
     * @param munge minify only
     * @param verbose display informational messages and warnings
     * @param preserveAllSemiColons preserve unnecessary semicolons
     * @param disableOptimizations disable all the built-in micro optimizations
     */
    public ProcessJSFilesTask(final Log log, final Integer bufferSize, final boolean debug, final boolean skipMerge,
            final boolean skipMinify, final String webappSourceDir, final String webappTargetDir, final String inputDir,
            final List<String> sourceFiles, final List<String> sourceIncludes, final List<String> sourceExcludes, final String outputDir,
            final String outputFilename, final String suffix, final boolean nosuffix, final boolean keepMerged, final String charset,
            final int linebreak, final String jsEngine, final boolean munge, final boolean verbose, final boolean preserveAllSemiColons,
            final boolean disableOptimizations)
    {
        super(log, bufferSize, debug, skipMerge, skipMinify, webappSourceDir, webappTargetDir, inputDir, sourceFiles, sourceIncludes,
                sourceExcludes, outputDir, outputFilename, suffix, nosuffix, keepMerged, charset, linebreak);

        this.jsEngine = jsEngine;
        this.munge = munge;
        this.verbose = verbose;
        this.preserveAllSemiColons = preserveAllSemiColons;
        this.disableOptimizations = disableOptimizations;
    }

    /**
     * Minifies a JavaScript file.
     *
     * @param mergedFile input file resulting from the merged step
     * @param minifiedFile output file resulting from the minify step
     * @throws IOException when the minify step fails
     */
    @Override
    protected void minify(final File mergedFile, final File minifiedFile) throws IOException {
        try
        {
            final InputStream in = new FileInputStream(mergedFile);
            try
            {
                final OutputStream out = new FileOutputStream(minifiedFile);
                try
                {
                    final InputStreamReader reader = new InputStreamReader(in, this.charset);
                    try
                    {
                    final OutputStreamWriter writer = new OutputStreamWriter(out, this.charset);
                        try
                        {

                            this.log.info("Creating the minified file [" + ((this.debug) ? minifiedFile.getPath() : minifiedFile.getName())
                                    + "].");

                            if ("closure".equals(this.jsEngine))
                            {
                                this.log.debug("Using Google Closure Compiler engine.");

                                final CompilerOptions options = new CompilerOptions();
                                CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
                                options.setOutputCharset(this.charset);

                                final SourceFile input = SourceFile.fromInputStream(mergedFile.getName(), in);
                                final List<SourceFile> externs = Collections.emptyList();

                                final Compiler compiler = new Compiler();
                                compiler.compile(externs, Arrays.asList(new SourceFile[] { input }), options);

                                writer.append(compiler.toSource());
                            }
                            else
                            {
                                this.log.debug("Using YUI Compressor engine.");

                                final JavaScriptCompressor compressor = new JavaScriptCompressor(reader, new JavaScriptErrorReporter(
                                        this.log, mergedFile.getName()));
                                compressor.compress(writer, this.linebreak, this.munge, this.verbose, this.preserveAllSemiColons,
                                        this.disableOptimizations);
                            }
                        }
                        finally
                        {
                            writer.close();
                        }
                    }
                    finally
                    {
                        reader.close();
                    }
                }
                finally
                {
                    // may already be closed but make sure
                    out.close();
                }
            }
            finally
            {
                // may already be closed but make sure
                in.close();
            }
        } catch (final IOException e) {
            this.log.error("Failed to compress the JavaScript file [" + mergedFile.getName() + "].", e);
            throw e;
        }

        this.logCompressionGains(mergedFile, minifiedFile);
    }
}
