def id = ""

new File("mp.obo").eachLine { line ->
  if (line.startsWith("id: ")) {
    id = line.substring(4).trim().replaceAll(":","_")
    def uri = "http://purl.obolibrary.org/obo/"+id
    println "groovy MakeTrainingData.groovy -r 1 -s $uri -o mptraining/$id"
  }
}