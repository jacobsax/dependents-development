package com.jwu.javaparser.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import com.jwu.javaparser.dependencygraph.DependencyGraph;
import com.jwu.javaparser.parser.visitors.ClassOrInterfaceDeclarationVisitor;
import com.jwu.javaparser.parser.visitors.MethodCallVisitor;
import com.jwu.javaparser.parser.visitors.MethodOverloadOverrideVisitor;
import com.jwu.javaparser.parser.visitors.PackageDeclarationVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * DirectoryParser finds all .Java files in a directory and all its children, and
 * then parses the AST trees of all those files to construct a single dependency
 * call graph.
 *
 * DirectoryParser also searches for jar files in specified locations, and uses those
 * jars for type inference.
 *
 * If running this on Mac OSX, you can very quickly run into a max open files limit. To get around this problem,
 * increase the max files allowed: https://medium.com/mindful-technology/too-many-open-files-limit-ulimit-on-mac-os-x-add0f1bfddde
 * Unfortunately, to carry out type inference, you have to load every Jar file and all relevant files inside it - when
 * parsing a project with a lot of dependencies this can become a big number fast...
 */
public class DirectoryParser {

    String projectName;
    String projectID;
    boolean parseAll;

    /**
     * DirectoryParser instantiates a directory parser
     * @param projectName name of the project being parsed
     * @param projectID a unique identifier for the project being parsed
     */
    DirectoryParser(String projectName, String projectID, boolean parseAll) {
        this.projectName = projectName;
        this.projectID = projectID;
        this.parseAll = parseAll;
    }

    /**
     * Lists all jars at a specific path
     * @param dirPath
     * @return
     */
    private Collection<Path> listFilesAtPath(Path dirPath, String fileEnding) {
        try {
            return Files.list(dirPath)
                    .filter(filePath -> filePath.toString().endsWith(fileEnding))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * List all directories at a specific path
     * @param dirPath
     * @return
     */
    private Collection<Path> listDirsAtPath(Path dirPath) {
        try {
            return Files.list(dirPath)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * parseDirectoriesForJars finds the paths of all jars in a directory
     * @param dirPath
     * @return
     */
    private Collection<Path> parseDirectoriesForFiles(Path dirPath, String fileEnding) {
        // find all files at the current directory
        Collection<Path> foundFiles = this.listFilesAtPath(dirPath, fileEnding);

        // find all sub directories in the current directory
        Collection<Path> foundDirs = this.listDirsAtPath(dirPath);

        // find all files in all sub directories
        for (Path subDir: foundDirs) {
            foundFiles.addAll(this.parseDirectoriesForFiles(subDir, fileEnding));
        }

        // return all found files
        return foundFiles;
    }

    /**
     * parseMethods builds a dependency call graph from a set of source files
     * @param graph the graph to add to
     * @param dirPath the dir path of the project
     * @param jarDirPaths  a list of paths to search for Jar files to use for type resolution
     * @throws IOException
     */
    public void parseMethods(DependencyGraph graph, Path dirPath, Collection<Path> jarDirPaths) throws IOException {

        // build a new type solver for type resolution
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver()); // ensure the type solver attempts to solve types via reflection (allows for solving of java std libs)

        // search for jars and add them to the type solver
        for (Path jarDirPath: jarDirPaths) {
            for (Path jarPath: this.parseDirectoriesForFiles(jarDirPath, ".jar")) {
                try {
                    System.out.println("Found and added jar at " + jarPath.toString());
                    combinedTypeSolver.add(new JarTypeSolver(jarPath));
                } catch (Exception e) {

                }
            }
        }

        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy()
                .collect(dirPath);

        for (SourceRoot sourceRoot: projectRoot.getSourceRoots()) {
            combinedTypeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
        }

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);

        // create the AST tree visitors
        VoidVisitor<?> classVisitor = new ClassOrInterfaceDeclarationVisitor(graph);
        VoidVisitor<?> packageVisitor = new PackageDeclarationVisitor(graph, this.projectName, this.projectID);

        VoidVisitor<?> overloadVisitor = null;
        VoidVisitor<?> methodVisitor = null;
        if (this.parseAll) {
            overloadVisitor = new MethodOverloadOverrideVisitor(graph);
            methodVisitor = new MethodCallVisitor(graph);
        }

        for (SourceRoot sourceRoot: projectRoot.getSourceRoots()) {
            sourceRoot.tryToParse();
            // A compilation unit is an AST tree. For each compilation unit, visit the relevant nodes
            for (CompilationUnit compilationUnit: sourceRoot.getCompilationUnits()) {
                packageVisitor.visit(compilationUnit, null);
                classVisitor.visit(compilationUnit, null);

                if (overloadVisitor != null && methodVisitor != null) {
                    overloadVisitor.visit(compilationUnit, null);
                    methodVisitor.visit(compilationUnit, null);
                }
            }
        }
    }
}
