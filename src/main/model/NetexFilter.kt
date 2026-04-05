package org.entur.otp.setup.model

import org.entur.netex.tools.lib.config.CliConfig
import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.netex.tools.lib.io.XMLFiles
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.model.EntityModel
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.NetexFileWriter
import org.entur.netex.tools.lib.output.XmlContext
import org.entur.netex.tools.lib.plugin.NetexFileWriterContext
import org.entur.netex.tools.lib.report.FileIndex
import org.entur.netex.tools.lib.sax.BuildEntityModelSaxHandler
import org.entur.netex.tools.lib.sax.OutputNetexSaxHandler
import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selections.InclusionPolicy
import org.entur.netex.tools.lib.selectors.entities.CompositeEntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.netex.tools.lib.selectors.refs.CompositeRefSelector
import java.io.File

object NetexFilter {

    /**
     * Filters all NeTEx files in [netexDir] to only keep service journeys serving
     * stops whose quay IDs are in [quayIds], then replaces the directory in-place.
     */
    fun filter(quayIds: Set<String>, netexDir: File) {
        if (quayIds.isEmpty()) {
            println("  No quays found — skipping NeTEx filter")
            return
        }
        val filteredDir = File(netexDir.parentFile, "netex-filtered")

        println("\nFilter NeTEx files using ${quayIds.size} quays...")

        val filterConfig = FilterConfigBuilder()
            .withEntitySelectors(listOf(ServiceJourneyEntitySelector(quayIds)))
            .withUnreferencedEntitiesToPrune(
                setOf("JourneyPattern", "Route", "Line", "Network", "Operator", "DestinationDisplay")
            )
            .build()

        runFilter(filterConfig, netexDir, filteredDir)

        netexDir.deleteRecursively()
        filteredDir.renameTo(netexDir)
        println("NeTEx filter complete.")
    }

    private fun runFilter(filterConfig: FilterConfig, input: File, target: File) {
        val cliConfig = CliConfig()
        val model = EntityModel(cliConfig.alias())
        val fileIndex = FileIndex()

        // Pass 1: build entity model
        XMLFiles.parseXmlDocuments(input) { file ->
            BuildEntityModelSaxHandler(
                entityModel = model,
                plugins = filterConfig.plugins,
                file = file,
                inclusionPolicy = InclusionPolicy(
                    entitySelection = null,
                    refSelection = null,
                    skipElements = filterConfig.skipElements
                )
            )
        }
        println("  Entity model: ${model.listAllEntities().size} entities, ${model.listAllRefs().size} refs")

        // Pass 2: select entities and refs to keep
        val entitiesToKeep = CompositeEntitySelector(filterConfig).selectEntities(
            EntitySelectorContext(entityModel = model)
        )
        val refsToKeep = CompositeRefSelector(filterConfig, entitiesToKeep).selectRefs(model)

        // Pass 3: write filtered output
        target.mkdirs()
        val writerContext = NetexFileWriterContext(
            useSelfClosingTagsWhereApplicable = filterConfig.useSelfClosingTagsWhereApplicable,
            removeEmptyCollections = true,
            preserveComments = filterConfig.preserveComments
        )
        XMLFiles.parseXmlDocuments(input) { file ->
            val outFile = File(target, file.name)
            val xmlContext = XmlContext(xmlFile = outFile)
            OutputNetexSaxHandler(
                entityModel = model,
                fileIndex = fileIndex,
                inclusionPolicy = InclusionPolicy(
                    entitySelection = entitiesToKeep,
                    refSelection = refsToKeep,
                    skipElements = filterConfig.skipElements
                ),
                fileWriter = NetexFileWriter(
                    netexFileWriterContext = writerContext,
                    xmlContext = xmlContext
                ),
                outputFile = outFile,
                elementWriter = DelegatingXMLElementWriter(
                    handlers = filterConfig.customElementHandlers,
                    xmlContext = xmlContext
                ),
                elementsRequiredChildren = filterConfig.elementsRequiredChildren
            )
        }
    }
}

/**
 * Selects ServiceJourney entities that (transitively) serve any of the given quay IDs.
 *
 * Traversal (all via back-references in the EntityModel):
 *   quayId
 *     → PassengerStopAssignment  (QuayRef back-ref)
 *     → ScheduledStopPoint       (ScheduledStopPointRef forward ref on PSA)
 *     → StopPointInJourneyPattern (ScheduledStopPointRef back-ref)
 *     → ServiceJourney           (StopPointInJourneyPatternRef back-ref — the ref is on
 *                                  TimetabledPassingTime, but its nearest entity ancestor
 *                                  in the SAX stack is ServiceJourney)
 *
 * The selector keeps ALL entities except ServiceJourneys not reachable from the quay set.
 * Combined with unreferencedEntitiesToPrune, unused Lines/Routes/JourneyPatterns are
 * removed in the subsequent pruning step.
 */
private class ServiceJourneyEntitySelector(private val quayIds: Set<String>) : EntitySelector {

    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        val model = context.entityModel
        val targetIds = findServiceJourneys(model)
        println("  ServiceJourneyEntitySelector: ${targetIds.size} service journeys selected")

        val kept = mutableMapOf<String, MutableMap<String, Entity>>()
        for (entity in model.listAllEntities()) {
            if (entity.type == "ServiceJourney" && entity.id !in targetIds) continue
            kept.getOrPut(entity.type) { mutableMapOf() }[entity.id] = entity
        }
        return EntitySelection(kept, model)
    }

    private fun findServiceJourneys(model: EntityModel): Set<String> {
        val result = mutableSetOf<String>()
        for (quayId in quayIds) {
            for (psa in model.getEntitiesReferringTo(quayId)) {
                if (psa.type != "PassengerStopAssignment") continue
                for (sspRef in model.getRefsOfTypeFrom(psa.id, "ScheduledStopPointRef")) {
                    for (spinJP in model.getEntitiesReferringTo(sspRef.ref)) {
                        if (spinJP.type != "StopPointInJourneyPattern") continue
                        for (sj in model.getEntitiesReferringTo(spinJP.id)) {
                            if (sj.type == "ServiceJourney") result.add(sj.id)
                        }
                    }
                }
            }
        }
        return result
    }
}
