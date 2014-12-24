import edu.uci.ics.jung.algorithms.shortestpath.*
import edu.uci.ics.jung.graph.*

def querygene = args[0]

def mgi2geneid = [:]
def geneid2mgi = [:]
new File("data/MGI_Gene_Model_Coord.rpt").splitEachLine("\t") { line ->
    def mgiid = line[0]
    def geneid = line[5]
    mgi2geneid[mgiid] = geneid
    geneid2mgi[geneid] = mgiid
}


UndirectedGraph<String, Object> g = new UndirectedSparseGraph<String, Object>()
new File("data/mppi_data_download.txt").splitEachLine("\\s+") { line ->
  def g1 = line[1]
  def g2 = line[2]
  def m1 = geneid2mgi[g1]
  def m2 = geneid2mgi[g2]
  if (m1 && m2) {
    try {
      g.addVertex(m1)
      g.addVertex(m2)
      g.addEdge(new Object(), m1, m2)
    } catch (Exception E) {
      println E.getMessage()
    }
  }
}

def pw2genes = [:].withDefault { new TreeSet() }
new File("data/pathways_summary.txt").splitEachLine("\t") { line ->
  if (line.size()>=4 && line[0]!="MGI_ID") {
    def mid = line[0]
    def pw = line[2]
    pw2genes[pw].add(mid)
  }
}

def mgi2uniprot = [:]
def uniprot2mgi = [:]
new File("data/MRK_Sequence.rpt").splitEachLine("\t") { line ->
  def mgi = line[0]
  def uniprot = line[14]
  if (uniprot) {
    mgi2uniprot[mgi] = uniprot
    uniprot2mgi[uniprot] = mgi
  }
}

//def reactomepw2genes = [:].withDefault { new TreeSet() }
new File("data/UniProt2Reactome.txt").splitEachLine("\t") { line ->
  if (line[0]) {
    def uniprot = uniprot2mgi[line[0]]
    def reactome = line[1]
    if (uniprot) {
      pw2genes[reactome].add(uniprot)
    }
  }
}

def interpro2mgi = [:].withDefault { new LinkedHashSet() }
new File("data/MGI_InterProDomains.rpt").splitEachLine("\t") { line ->
  def dom = line[0]
  def mgi = line[2]
  if (mgi) {
    interpro2mgi[dom].add(mgi)
  }
}

DijkstraDistance<String, Object> dist = new DijkstraDistance<String, Object> (g, true)

//println dist.getDistanceMap("MGI:2446108")

pw2genes.each { pw, genes ->
  def min = 100
  if (g.containsVertex(querygene)) {
    genes.each { gene ->
      if (g.containsVertex(gene)) {
	def d = dist.getDistance(gene, querygene)
	if (d && d < min) {
	  min = d
	}
      }
    }
  }
  println "$pw\t$min"
}
