def cost = 0
def par = 0
def acc = 0
new File(args[0]).eachLine { line ->
    def toks = line.split("-")
    def mp = toks[0]
    def lpar = toks[1]
    def lcost = toks[2].substring(0, toks[2].indexOf(" ")).trim()
    toks = line.split(" ")
    def lacc = new Double(toks[2].replaceAll(";",""))
    if (lacc > acc) {
       cost = lcost
       par = lpar
       acc = lacc
    }   
}
println "$cost $par $acc"
