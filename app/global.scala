import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
    Logger.info("Genes loaded: "+models.GeneData.genes.size)
    Logger.info("Predefined genesets loaded: "+models.GeneData.predefinedGeneSets.size)
  }  
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }  
    
}