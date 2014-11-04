import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction
import edu.berkeley.compbio.jlibsvm.util.*
import org.openrdf.model.vocabulary.*
import org.openrdf.model.URI
import slib.graph.model.impl.graph.elements.Edge
import slib.graph.io.conf.GDataConf
import slib.graph.io.loader.*
import slib.graph.io.util.GFormat
import slib.graph.model.graph.G
import slib.graph.model.impl.repo.URIFactoryMemory
import slib.graph.model.repo.URIFactory
import slib.sml.sm.core.engine.SM_Engine
import slib.sml.sm.core.metrics.ic.utils.*
import slib.sml.sm.core.utils.*
import slib.graph.algo.extraction.rvf.instances.InstancesAccessor
import slib.graph.algo.graph.utils.*
import slib.graph.algo.extraction.utils.*
import slib.utils.impl.Timer
import slib.graph.algo.extraction.rvf.instances.impl.InstanceAccessor_RDF_TYPE
import slib.graph.model.impl.graph.memory.GraphMemory


public class SemanticKernelFunction implements KernelFunction<Set<String>> {
  def URI = "http://phenomebrowser.net/smltest/"
  URIFactory factory = URIFactoryMemory.getSingleton()
  URI graph_uri = factory.getURI(URI)
  G graph = null
  GDataConf graphconf = null 
  SM_Engine engine = null
  Map<String, Integer> class2index = [:]
  Map<Integer, String> index2class = [:]

  // hard-coded configuration; add to constructor at some point; simGIC (weighted Jaccard) should define a kernel function

  ICconf icConf = new IC_Conf_Corpus("Resnik", SMConstants.FLAG_IC_ANNOT_RESNIK_1995)
  SMconf smConfPairwise = new SMconf("JiangConrath", SMConstants.FLAG_DIST_PAIRWISE_DAG_NODE_JIANG_CONRATH_1997)
  SMconf smConfGroupwise = new SMconf("BMA", SMConstants.FLAG_SIM_GROUPWISE_AVERAGE)

  //  SMconf smConfGroupwise = new SMconf("SimGIC", SMConstants.FLAG_SIM_GROUPWISE_DAG_GIC)



  // ontologyFile: RDF/XML file containing the ontology (classified)
  // dataFile: TSV file containing the actual data; structure: <id> [tab] <Class URI>
  public SemanticKernelFunction(String ontologyFile, String dataFile) {

    smConfGroupwise.setICconf(icConf)
    smConfPairwise.setICconf(icConf)


    graph = new GraphMemory(graph_uri)
    this.graphconf = new GDataConf(GFormat.RDF_XML, ontologyFile)
    GraphLoaderGeneric.populate(graphconf, this.graph)

    URI virtualRoot = factory.getURI("http://phenomebrowser.net/smltest/virtualRoot");
    graph.addV(virtualRoot);
    // We root the graphs using the virtual root as root
    GAction rooting = new GAction(GActionType.REROOTING)
    rooting.addParameter("root_uri", virtualRoot.stringValue())
    GraphActionExecutor.applyAction(factory, rooting, graph)

    // remove all instances from the ontology
    Set removeE = new LinkedHashSet()
    graph.getE().each { it ->
      String es = it.toString();
      if ( es.indexOf("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")>-1 ) {
	removeE.add( it );
      }
    }               
    removeE.each { graph.removeE(it) }
    
    def instanceCounter = 0
    new File(dataFile).splitEachLine("\t") { line ->
      if (line[0].startsWith("map")) {
	def toks = line
	def counter = 0
	toks[1..-1].each { tok ->
	  index2class[counter] = tok
	  class2index[tok] = counter
	  counter += 1
	}
	this.index2class = index2class
	this.class2index = class2index
      } else {
	def iduri = factory.getURI(URI+instanceCounter)
	line[1..-1].each { oc ->
	  def onturi = factory.getURI(oc)
	  try {
	    Edge e = new Edge(iduri, RDF.TYPE, onturi);
	    graph.addE(e)
	  } catch (Exception E) {
	    E.printStackTrace()
	  }
	}
	instanceCounter += 1
      }
    }

    engine = new SM_Engine(graph)

  }

  // s1 and s2 are sets of URI-Strings
  double evaluate(Set<String> s1, Set<String> s2) {
    /*
    Set s1 = new LinkedHashSet()
    Set s2 = new LinkedHashSet()
    a.indexes.each { ia ->
      if (index2class[ia]) {
	s1.add(factory.getURI(index2class[ia]))
      }
    }
    b.indexes.each { ib ->
      if (index2class[ib]) {
	s2.add(factory.getURI(index2class[ib]))
      }
    }
    */
    Set<URI> a = new LinkedHashSet()
    Set<URI> b = new LinkedHashSet()
    s1.each { a.add(factory.getURI(it)) }
    s2.each { b.add(factory.getURI(it)) }

    //    double sim = engine.computeGroupwiseStandaloneSim(smConfGroupwise, a, b)
    double sim = engine.compare(smConfGroupwise, smConfPairwise, a, b)
    //    println "s1: $s1\ts2: $s2\t$sim"

    return sim
  }

  String toString() {
    def s = ""
    s += "kernel_type SemanticKernelFunction\n"
    return s
  }

}