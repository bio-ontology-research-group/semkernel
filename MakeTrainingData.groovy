def classifierFor = args[0]

def map = [:].withDefault { new TreeSet() }

def classes = new TreeSet()

new File("gene_association.mgi").splitEachLine("\t") { line ->
  if (! line[0].startsWith("!")) {
    def gid = line[1]
    def got = line[4].replaceAll(":","_")
    def evidence = line[6]
    if (evidence != "ND") {
      got = "http://purl.obolibrary.org/obo/"+got
      map[gid].add(got)
      classes.add(got)
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

// header
print "map"
classes.each { print "\t$it" }
println ""

pmap.each { k, pset ->
  if (classifierFor in pset) {
    print "1"
  } else {
    print "0"
  }
  def gos = map[k]
  gos.each { print "\t$it" }
  println ""
}