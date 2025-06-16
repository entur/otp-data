package org.entur.otp.setup

import org.entur.otp.setup.csv.SetupCaseParser
import org.entur.otp.setup.model.Config
import org.entur.otp.setup.model.ConfResource


fun config(): Config = Config(
    osm = listOf(
        ConfResource(
            ids = "norway".split(),
            url = "https://download.geofabrik.de/europe",
            file = "norway-latest.osm.pbf"
        )
    ),
    // NeTEx resource templates, {{file}} and {{environment}} is injected at execution time.
    netex = listOf(
        ConfResource(
            ids = "norway atb akt bra fin flt goa hav hur inn kol mor nbu nor ost rut sjn sky sof tel tro vkt vyg vyx".split(),
            // https://storage.googleapis.com/marduk-production/outbound/netex/rb_akt-aggregated-netex.zip
            url = "https://storage.googleapis.com/marduk-{{en-netex-env}}/outbound/netex",
            file = "rb_{{id}}-aggregated-netex.zip",
        ),
        ConfResource(
            ids = (
                "03_Oslo 11_Rogaland 15_More%20og%20Romsdal 18_Nordland 32_Akershus " +
                    "33_Buskerud 34_Innlandet 39_Vestfold 40_Telemark 42_Agder 46_Vestland " +
                    "50_Trondelag 55_Troms 56_Finnmark RailStations"
                ).split(),
            // https://storage.googleapis.com/marduk-production/tiamat/42_Agder_latest.zip
            url = "https://storage.googleapis.com/marduk-{{en-netex-env}}/tiamat",
            file = "{{id}}_latest.zip",
        )
    ),
    cases = SetupCaseParser.parse(),
    // Entur Netex environment: dev or production
    env = mapOf(Pair("en-netex-env", "production"))
)

fun String.split(): List<String> = this.split(' ')
