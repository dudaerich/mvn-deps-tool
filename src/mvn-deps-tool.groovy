import groovy.io.FileType
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

/* ############# Helpers ############# */

class Command {
    final static int ERR = -1
    final static int REPLACE = 0
    final static int BOM = 1

    public static int parse(String command) {
        switch (command.toUpperCase()) {
            case "REPLACE": return REPLACE
            case "BOM": return BOM
            default: return ERR
        }
    }
}

class Pom {
    File file = null
    def depsManagement = []
    def deps = []

    void extractDeps() {
        def pom = new XmlSlurper().parse(file)
        depsManagement.addAll pom.dependencyManagement.dependencies.'*'.findAll { it.type.text() != 'test-jar' }
        deps.addAll pom.dependencies.'*'.findAll { it.version.text() != "" && it.type.text() != 'test-jar' }
    }

    void replaceDep(String groupId, String artifactId, String version) {
        def depManagementRes = depsManagement.findAll { it.groupId == groupId && it.artifactId == artifactId }
        for (def depManagement : depManagementRes) {
            depManagement.version = version
        }

        def depRes = deps.findAll { it.groupId == groupId && it.artifactId == artifactId }
        for (def dep : depRes) {
            dep.version = version
        }
    }

    void serialize() {
        def pom = new XmlSlurper().parse(file)

        for (def depManagement : depsManagement) {
            def depNodeRes = pom.dependencyManagement.dependencies.'*'.findAll { it.groupId == depManagement.groupId && it.artifactId == depManagement.artifactId && it.type.text() != 'test-jar' }
            for (def depNode : depNodeRes) {
                depNode.version = depManagement.version.text()
            }
        }

        for (def dep : deps) {
            def depNodeRes = pom.dependencies.'*'.findAll { it.groupId == dep.groupId && it.artifactId == dep.artifactId  && it.type.text() != 'test-jar'}
            for (def depNode : depNodeRes) {
                depNode.version = dep.version.text()
            }
        }

        String xml = XmlUtil.serialize(pom).replaceAll('tag0:', '').replaceAll(':tag0', '')
        PrintWriter writer = new PrintWriter(new FileOutputStream(file, false))
        writer.print(xml)
        writer.close()
    }

    boolean contains(String groupId, String artifactId) {
        return depsManagement.find( { it.groupId == groupId && it.artifactId == artifactId } ) != null || deps.find( { it.groupId == groupId && it.artifactId == artifactId } ) != null
    }

    String toString() {
        StringBuilder resp = new StringBuilder(file.getAbsolutePath())
        resp.append('\n')

        for (def dep : deps) {
            resp.append("${dep.groupId}:${dep.artifactId}:${dep.version}")
            resp.append('\n')
        }
        return resp.toString()
    }
}

/* ############# Main program ############# */

def cli = new CliBuilder(usage: 'groovy mvn-deps-tool.groovy <command> <maven project> <file with maven dependencies>')
cli.h(longOpt: 'help', 'Print this help.')
def options = cli.parse(args)

if (options.h || options.arguments().size() != 3) {
    cli.usage()
    System.exit(1)
}

int command = Command.parse(options.arguments().get(0))

if (command == Command.ERR) {
    cli.usage()
    System.exit(1)
}

File mavenProject = new File(options.arguments().get(1))
File mavenDeps = new File(options.arguments().get(2))

def poms = []
mavenProject.eachFileRecurse(FileType.FILES, {
    if ("pom.xml".equals(it.getName())) {
        Pom pom = new Pom(file: it)
        pom.extractDeps()
        poms.add(pom)
    }
})

if (command == Command.REPLACE) {
    mavenDeps.eachLine { line ->
        def gav = line.split(':')

        if (gav.size() >= 3) {
            for (Pom pom : poms) {
                pom.replaceDep(gav[0], gav[1], gav[2])
            }
        }
    }

    for (Pom pom : poms) {
        pom.serialize()
    }
} else if (command == Command.BOM) {
    def deps = []
    mavenDeps.eachLine { line ->
        def gav = line.split(':')

        if (gav.size() >= 3) {
            for (Pom pom : poms) {
                if (pom.contains(gav[0], gav[1])) {
                    deps.add([ groupId: gav[0], artifactId: gav[1], version: gav[2] ])
                    break
                }
            }
        }
    }
    def writer = new StringWriter()
    def xmlBuilder = new MarkupBuilder(writer)
    xmlBuilder.project {
        modelVersion(System.getProperty('modelVersion', '4.0.0'))
        groupId(System.getProperty('groupId', 'org.default'))
        artifactId(System.getProperty('artifactId', 'default'))
        version(System.getProperty('version', '1.0.0'))
        dependencyManagement {
            dependencies {
                deps.each { dep ->
                    dependency {
                        groupId(dep.groupId)
                        artifactId(dep.artifactId)
                        version(dep.version)
                    }
                }
            }
        }
    }
    println writer
}

