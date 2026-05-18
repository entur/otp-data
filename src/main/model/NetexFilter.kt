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
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
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
import org.xml.sax.Attributes
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

        val spijpToSj = mutableMapOf<String, MutableSet<String>>()
        val filterConfig = FilterConfigBuilder()
            .withPlugins(listOf(SpijpToSjPlugin(spijpToSj)))
            .withEntitySelectors(listOf(ServiceJourneyEntitySelector(quayIds, spijpToSj)))
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

        // Pass 1: build entity model (plugins also collect data here)
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
 * Traversal:
 *   quayId
 *     → PassengerStopAssignment   (QuayRef back-ref in EntityModel)
 *     → ScheduledStopPoint        (ScheduledStopPointRef forward ref on PSA)
 *     → StopPointInJourneyPattern (ScheduledStopPointRef back-ref in EntityModel)
 *     → ServiceJourney            (via spijpToSj — TimetabledPassingTime inside SJ holds
 *                                   StopPointInJourneyPatternRef, but TPT has its own entity id
 *                                   so EntityModel cannot resolve SJ from it; SpijpToSjPlugin
 *                                   builds the SPIJP→SJ map during Pass 1 instead)
 *
 * Keeps ALL entities except ServiceJourneys not reachable from the quay set.
 * Combined with unreferencedEntitiesToPrune, unused Lines/Routes/JourneyPatterns are
 * removed in the subsequent pruning step.
 */
private class ServiceJourneyEntitySelector(
    private val quayIds: Set<String>,
    private val spijpToSj: Map<String, Set<String>>
) : EntitySelector {

    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        val model = context.entityModel
        val targetIds = findServiceJourneys(model)
        println("  ServiceJourneyEntitySelector: ${targetIds.size} service journeys selected")

        val kept = mutableMapOf<String, MutableMap<String, Entity>>()
        for (entity in model.listAllEntities()) {
            if (entity.type == "ServiceJourney" && entity.id.baseId() !in targetIds) continue
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
                        spijpToSj[spinJP.id.baseId()]?.let { result.addAll(it) }
                    }
                }
            }
        }
        return result
    }
}

/**
 * Collects a SPIJP-ID → ServiceJourney-ID mapping during Pass 1 (BuildEntityModelSaxHandler).
 * TimetabledPassingTime→ServiceJourney is a containment relationship not tracked by the
 * EntityModel, so a plugin is the right hook to capture it during the existing XML scan.
 */
private class SpijpToSjPlugin(
    private val spijpToSj: MutableMap<String, MutableSet<String>>
) : AbstractNetexPlugin() {
    private var currentSjId: String? = null

    override fun getName() = "SpijpToSjPlugin"
    override fun getDescription() = "Maps StopPointInJourneyPattern IDs to parent ServiceJourney IDs"
    override fun getSupportedElementTypes() = setOf("ServiceJourney", "StopPointInJourneyPatternRef")

    override fun startElement(elementName: String, attributes: Attributes?, currentEntity: Entity?) {
        when (elementName) {
            "ServiceJourney" -> currentSjId = attributes?.getValue("id")
            // currentEntity is TimetabledPassingTime when the ref is inside passingTimes,
            // and StopPointInJourneyPattern when inside pointsInSequence — only the former maps to an SJ
            "StopPointInJourneyPatternRef" -> if (currentEntity?.type == "TimetabledPassingTime") {
                val spijpId = attributes?.getValue("ref") ?: return
                val sjId = currentSjId ?: return
                spijpToSj.getOrPut(spijpId) { mutableSetOf() }.add(sjId)
            }
        }
    }

    override fun endElement(elementName: String, currentEntity: Entity?) {
        if (elementName == "ServiceJourney") currentSjId = null
    }
}

private fun String.baseId() = substringBefore('|')
