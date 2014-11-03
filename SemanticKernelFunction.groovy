
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction
import edu.berkeley.compbio.jlibsvm.util.*
import org.openrdf.model.vocabulary.*
import org.openrdf.model.URI
import slib.sglib.model.impl.graph.elements.Edge
import slib.sglib.io.conf.GDataConf
import slib.sglib.io.loader.*
import slib.sglib.io.util.GFormat
import slib.sglib.model.graph.G
import slib.sglib.model.impl.repo.URIFactoryMemory
import slib.sglib.model.repo.URIFactory
import slib.sml.sm.core.engine.SM_Engine
import slib.sml.sm.core.metrics.ic.utils.*
import slib.sml.sm.core.utils.*
import slib.sglib.algo.graph.extraction.rvf.instances.InstancesAccessor
import slib.sglib.algo.graph.utils.*
import slib.utils.impl.Timer
import slib.sglib.algo.graph.extraction.rvf.instances.impl.InstanceAccessor_RDF_TYPE
import slib.sglib.model.impl.graph.memory.GraphMemory
import slib.sglib.algo.graph.utils.*


public class SemanticKernelFunction implements KernelFunction<SparseVector> {
  def URI = "http://phenomebrowser.net/smltest/"
  URIFactory factory = URIFactoryMemory.getSingleton()
  URI graph_uri = factory.createURI(URI)
  G graph = null
  GDataConf graphconf = null 
  SM_Engine engine = null
  Map<String, Integer> class2index = [:]
  Map<Integer, String> index2class = [:]

  // hard-coded configuration; add to constructor at some point; simGIC (weighted Jaccard) should define a kernel function
  ICconf icConf = new IC_Conf_Corpus("Resnik", SMConstants.FLAG_IC_ANNOT_RESNIK_1995)
  SMconf smConfGroupwise = new SMconf("SimGIC", SMConstants.FLAG_SIM_GROUPWISE_DAG_GIC)



  // ontologyFile: RDF/XML file containing the ontology (classified)
  // dataFile: TSV file containing the actual data; structure: <id> [tab] <Class URI>
  public SemanticKernelFunction(String ontologyFile, String dataFile) {

    smConfGroupwise.setICconf(icConf)


    graph = new GraphMemory(graph_uri)
    this.graphconf = new GDataConf(GFormat.RDF_XML, ontologyFile)
    GraphLoaderGeneric.populate(graphconf, this.graph)

    URI virtualRoot = factory.createURI("http://phenomebrowser.net/smltest/virtualRoot");
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
	def iduri = factory.createURI(URI+instanceCounter)
	line[1..-1].each { oc ->
	  def onturi = factory.createURI(oc)
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

    def counter = 0
    /*
    graph.getV().each { v ->
      class2index[v.toString()] = counter
      index2class[counter] = v.toString()
      counter += 1
    }
    */
  }

  double evaluate(SparseVector a, SparseVector b) {
    Set s1 = new LinkedHashSet()
    Set s2 = new LinkedHashSet()
    a.indexes.each { ia ->
      if (index2class[ia]) {
	s1.add(factory.createURI(index2class[ia]))
      }
    }
    b.indexes.each { ib ->
      if (index2class[ib]) {
	s2.add(factory.createURI(index2class[ib]))
      }
    }
    double sim = engine.computeGroupwiseStandaloneSim(smConfGroupwise, s1, s2)
    //    println "s1: $s1\ts2: $s2\t$sim"

    return sim
  }
}