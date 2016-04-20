import groovy.io.FileType
import groovy.xml.XmlUtil

/* ############# Helpers ############# */

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

def cli = new CliBuilder(usage: 'groovy replace-mvn-deps <maven project> <file with maven dependencies>')
def options = cli.parse(args)

File mavenProject = new File(options.arguments().get(0))
URL mavenDeps = new URL(options.arguments().get(1))

def poms = []
mavenProject.eachFileRecurse(FileType.FILES, {
    if ("pom.xml".equals(it.getName())) {
        Pom pom = new Pom(file: it)
        pom.extractDeps()
        poms.add(pom)
    }
})

BufferedReader readerMvnDeps = new BufferedReader(new InputStreamReader(mavenDeps.openStream()))

String line = null
while ((line = readerMvnDeps.readLine()) != null) {
    def res = line.split(':')

    for (Pom pom : poms) {
        pom.replaceDep(res[0], res[1], res[2])
    }
}

readerMvnDeps.close()

for (Pom pom : poms) {
    pom.serialize()
}

