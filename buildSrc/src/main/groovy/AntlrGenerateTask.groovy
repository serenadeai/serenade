package ai.serenade

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class AntlrGenerateTask extends DefaultTask {
    @Input
    String jar;

    @Input
    String language;

    @Input
    @Optional
    String grammar;

    @Input
    String packageName;

    @Input
    @Optional
    String lexer;

    @Input
    @Optional
    String parser;

    @Optional
    @Input
    List<String> args = ["-visitor"]

    def buildPath() {
        def name = packageName.replaceAll("\\.", "/")
        return "${project.buildDir}/generated/antlr/main/${name}/"
    }

    def fileName(String f) {
        return f.substring(f.lastIndexOf("/") + 1).split("\\.")[0]
    }

    def runAntlr(String f) {
        def working = workingPath(f)
        def file = fileName(f)
        def build = buildPath()
        def antlrArgs = args + ["-package", packageName]
        def argsString = antlrArgs.join(" ")

        Process command = """
            java -Xmx500M -cp ${jar} org.antlr.v4.Tool ${argsString} -o ${build} ${file}.g4
        """.execute(null, project.file(working))
        command.waitForProcessOutput(System.out, System.err)

        if (command.exitValue() != 0) {
            throw new GradleException('Antlr failed to compile')
        }
    }

    def workingPath(String f) {
        return f.substring(0, f.lastIndexOf("/") + 1)
    }

    AntlrGenerateTask() {
        project.afterEvaluate {
            def build = buildPath()
            outputs.dir project.file(build)

            if (grammar != null) {
                def file = fileName(grammar)
                inputs.file project.file(grammar)
                outputs.file project.file("${build}${file}Lexer.${language}")
                outputs.file project.file("${build}${file}Lexer.tokens")
                outputs.file project.file("${build}${file}BaseVisitor.${language}")
                outputs.file project.file("${build}${file}Visitor.${language}")
                outputs.file project.file("${build}${file}Parser.${language}")
                outputs.file project.file("${build}${file}.tokens")
            }

            else {
                if (lexer != null) {
                    def file = fileName(lexer)
                    inputs.file project.file(lexer)
                    outputs.file project.file("${build}${file}.${language}")
                    outputs.file project.file("${build}${file}.tokens")
                }

                if (parser != null) {
                    def file = fileName(lexer)
                    inputs.file project.file(parser)
                    outputs.file project.file("${build}${file}.${language}")
                    outputs.file project.file("${build}${file}BaseVisitor.${language}")
                    outputs.file project.file("${build}${file}Visitor.${language}")
                    outputs.file project.file("${build}${file}.tokens")
                }
            }
        }
    }

    @TaskAction
    def start() {
        if (grammar != null) {
            runAntlr(grammar)
        }

        else {
            if (lexer != null) {
                runAntlr(lexer)
            }

            if (parser != null) {
                runAntlr(parser)
            }
        }
    }
}
