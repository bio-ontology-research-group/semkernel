import org.semanticweb.owlapi.io.* 
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.elk.owlapi.*

def cli = new CliBuilder()
cli.with {
usage: 'Self'
  h longOpt:'help', 'this information'
  s longOpt:'search-class', 'URI of phenotype class to build a classifier for', args:1, required:true
  o longOpt:'output', 'output file', args:1, required:true
  r longOpt:'ratio', 'ratio of negative to positive (default: use all)', args:1, required:false
  //  "1" longOpt:'pmi', 'min PMI', args:1, required:true
  //  "2" longOpt:'lmi', 'min LMI', args:1, required:true
}

def opt = cli.parse(args)
if( !opt ) {
  //  cli.usage()
  return
}
if( opt.h ) {
    cli.usage()
    return
}

def searchClass = opt.s
def fout = new PrintWriter(new BufferedWriter(new FileWriter(opt.o)))
Double ratio = -1
if (opt.r) { ratio = new Double(opt.r) }

def classifierFor = new TreeSet()
classifierFor.add(searchClass)

OWLOntologyManager manager = OWLManager.createOWLOntologyManager()

OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File("mp.obo"))

OWLDataFactory fac = manager.getOWLDataFactory()

OWLReasonerFactory reasonerFactory = null

ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
OWLReasonerFactory fac1 = new ElkReasonerFactory()
OWLReasoner reasoner = fac1.createReasoner(ont)

reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)

def cl = fac.getOWLClass(IRI.create(searchClass))
reasoner.getSubClasses(cl, false).getFlattened().each { sc ->
  classifierFor.add(sc.toString().replaceAll("<","").replaceAll(">",""))
}

def map = [:].withDefault { new TreeSet() }

//def classes = ont.getClassesInSignature(true)

new File("gene_association.mgi").splitEachLine("\t") { line ->
  if (! line[0].startsWith("!")) {
    def gid = line[1]
    def got = line[4].replaceAll(":","_")
    def evidence = line[6]
    if (evidence != "ND") {
      got = "http://purl.obolibrary.org/obo/"+got
      map[gid].add(got)
      //      classes.add(got)
    }
  }
}

def pmap = [:].withDefault { new LinkedHashSet() }
new File("mousephenotypes.txt").splitEachLine("\t") { line ->
  def mgiid = line[0]
  if (mgiid in map.keySet()) {
    def pid = line[1]
    if (pid) {
      pid = pid.replaceAll(":","_")
      pid = "http://purl.obolibrary.org/obo/"+pid
      pmap[mgiid].add(pid)
    }
  }
}

// header; OBSOLETE
/*
fout.print("map")
classes.each { fout.print("\t$it") }
fout.println ("")
*/

List negatives = []
List positives = []
pmap.each { k, pset ->
  def s = ""
  if (classifierFor.intersect(pset).size()>0) {
    s = "1"
  } else {
    s = "0"
  }
  def gos = map[k]
  gos.each { s+=("\t$it") }
  if (classifierFor.intersect(pset).size()>0) {
    positives.add(s)
  } else {
    negatives.add(s)
  }
}
positives.each { fout.println(it) }
Collections.shuffle(negatives)
def posSize = positives.size()
def cutoff = Math.round(ratio * posSize)

if (cutoff > negatives.size()) {
  ratio = -1
}
if (ratio > 0) {
  negatives[0..cutoff].each { 
    fout.println(it)
  }
} else {
  negatives.each {
    fout.println(it)
  }
}
fout.flush()
fout.close()
