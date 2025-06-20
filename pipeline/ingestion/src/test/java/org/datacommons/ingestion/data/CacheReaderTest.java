package org.datacommons.ingestion.data;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.datacommons.proto.Storage.Observations;
import org.junit.Test;

public class CacheReaderTest {
  @Test
  public void testParseArcRow_outArcwithNode() {
    CacheReader reader = newCacheReader();
    String row =
        "d/m/Percent_WorkRelatedPhysicalActivity_ModerateActivityOrHeavyActivity_In_Count_Person^measuredProperty^Property^0,H4sIAAAAAAAAAOPS5WJNzi/NK5GCUEqyKcn6SYnFqfoepbmJeUGpiSmJSTmpwSWJJWGJRcWCDGDwwR4AejAnwDgAAAA=";

    NodesEdges expected =
        new NodesEdges()
            .addNode(
                Node.builder().subjectId("count").name("count").types(List.of("Property")).build())
            .addEdge(
                Edge.builder()
                    .subjectId(
                        "Percent_WorkRelatedPhysicalActivity_ModerateActivityOrHeavyActivity_In_Count_Person")
                    .predicate("measuredProperty")
                    .objectId("count")
                    .objectValue("")
                    .provenance("dc/base/HumanReadableStatVars")
                    .build());

    NodesEdges actual = reader.parseArcRow(row);

    assertEquals(expected, actual);
  }

  @Test
  public void testParseArcRow_outArcwithoutNode() {
    CacheReader reader = newCacheReader();
    String row =
        "d/m/Percent_WorkRelatedPhysicalActivity_ModerateActivityOrHeavyActivity_In_Count_Person^name^^0,H4sIAAAAAAAAAONqYFSSTUnWT0osTtX3KM1NzAtKTUxJTMpJDS5JLAlLLCrWig9ILUpOzStJTE9VCM8vylYISs1JLElNUQjIqCzOTE7MUXBMLsksyyyp1FHwzU9JLQJKwoUU/IsUPFITyyoRIo65+XnpCgH5BaVAYzLz8wQZwOCDPQA1JajOjAAAAA==";

    NodesEdges expected =
        new NodesEdges()
            .addEdge(
                Edge.builder()
                    .subjectId(
                        "Percent_WorkRelatedPhysicalActivity_ModerateActivityOrHeavyActivity_In_Count_Person")
                    .predicate("name")
                    .objectId(
                        "Percent_WorkRelatedPhysicalActivity_ModerateActivityOrHeavyActivity_In_Count_Person")
                    .objectValue(
                        "Percentage Work Related Physical Activity, Moderate Activity Or Heavy Activity Among Population")
                    .provenance("dc/base/HumanReadableStatVars")
                    .build());

    NodesEdges actual = reader.parseArcRow(row);

    assertEquals(expected, actual);
  }

  @Test
  public void testParseArcRow_inArc() {
    CacheReader reader = newCacheReader();
    String row =
        "d/l/dc/d/UnitedNationsUn_SdgIndicatorsDatabase^isPartOf^Provenance^0,H4sIAAAAAAAAAOPS4GIL9YsPdnGX4ktJ1k9KLE7Vh/CV0PiCDGDwwR4AhMbiaDMAAAA=";

    NodesEdges expected =
        new NodesEdges()
            .addNode(
                Node.builder()
                    .subjectId("dc/base/UN_SDG")
                    .name("UN_SDG")
                    .types(List.of("Provenance"))
                    .build())
            .addEdge(
                Edge.builder()
                    .subjectId("dc/base/UN_SDG")
                    .predicate("isPartOf")
                    .objectId("dc/d/UnitedNationsUn_SdgIndicatorsDatabase")
                    .objectValue("")
                    .provenance("dc/base/UN_SDG")
                    .build());

    NodesEdges actual = reader.parseArcRow(row);

    assertEquals(expected, actual);
  }

  @Test
  public void testParseTimeSeriesRow() {
    CacheReader reader = newCacheReader();
    String row =
        "d/3/geoId/sch2915390^Mean_PrecipitableWater_Atmosphere,H4sIAAAAAAAAAOPqZpHi8igtykmtVAjS9TT6w8wlysVlZGBkqmtgpGtkIMgQvilZeYOoA4qwkeBcve7Os9OVUIWNBQOzZ5XPaVJBEjbWNTAU3OHQ9Oj4DD1U1aaCIvbH7mx9ookibGguqN6xi98hSwZV2EJQ0Mxmb5CYFqohJoJMszik59WpogobCq49MOHqWjk5VJcYCRac67h074AqqrCxoOOVWoag1QqohlgIpnUkx96ZZoQqbC7IeI+Js2s+mkvMBKub9X2WrdJENdtE0N9BOPHwZkVU71gKGoOBioMUh5+/o2O8u1uwEnOAoYuWBIwbH56aWJKRWuSWX5SanFhcYsSRl5+YqJeeX+bE5ZuZk5OZm1qSWuTBGOSZUVJSUGylr19eXq6Xl5yaqQdTqF9QlJ9SmlxSrF8OMUs3GagtsSRVNzc/JTWnWD89Jz8pMUc3DWoFAAwgwZ8OAgAA";

    Observations series =
        Observations.newBuilder()
            .putValues("2025-02-20", "5.42201")
            .putValues("2025-02-22", "9.29649")
            .putValues("2025-02-23", "10.2551")
            .putValues("2025-03-01", "15.2984")
            .putValues("2025-02-25", "12.9467")
            .putValues("2025-02-17", "7.10376")
            .putValues("2025-02-18", "13.0436")
            .putValues("2025-02-24", "10.7473")
            .putValues("2025-02-21", "7.52996")
            .putValues("2025-03-02", "10.8767")
            .putValues("2025-03-03", "8.33461")
            .putValues("2025-02-28", "18.5893")
            .putValues("2025-02-27", "13.3116")
            .putValues("2025-02-26", "12.8333")
            .putValues("2025-03-04", "8.8511")
            .putValues("2025-02-19", "10.1")
            .build();

    List<Observation> expected =
        List.of(
            Observation.builder()
                .variableMeasured("Mean_PrecipitableWater_Atmosphere")
                .observationAbout("geoId/sch2915390")
                .observations(series)
                .observationPeriod("P1D")
                .measurementMethod("NOAA_GFS")
                .scalingFactor("")
                .unit("Millimeter")
                .provenanceUrl(
                    "https://www.ncei.noaa.gov/products/weather-climate-models/global-forecast")
                .importName("NOAA_GFS_WeatherForecast")
                .build());

    NodesEdges expectedGraph =
        new NodesEdges()
            .addNode(
                Node.builder()
                    .subjectId("dc/os/Mean_PrecipitableWater_Atmosphere_geoId_sch2915390_870755143")
                    .name("Mean Precipitable Water Atmosphere | geoId/sch2915390 | 870755143")
                    .types(List.of("StatVarObsSeries"))
                    .build())
            .addEdge(
                Edge.builder()
                    .subjectId("dc/os/Mean_PrecipitableWater_Atmosphere_geoId_sch2915390_870755143")
                    .predicate("variableMeasured")
                    .objectId("Mean_PrecipitableWater_Atmosphere")
                    .objectValue("")
                    .provenance("dc/base/NOAA_GFS_WeatherForecast")
                    .build())
            .addEdge(
                Edge.builder()
                    .subjectId("dc/os/Mean_PrecipitableWater_Atmosphere_geoId_sch2915390_870755143")
                    .predicate("observationAbout")
                    .objectId("geoId/sch2915390")
                    .objectValue("")
                    .provenance("dc/base/NOAA_GFS_WeatherForecast")
                    .build())
            .addEdge(
                Edge.builder()
                    .subjectId("dc/os/Mean_PrecipitableWater_Atmosphere_geoId_sch2915390_870755143")
                    .predicate("name")
                    .objectId("dc/os/Mean_PrecipitableWater_Atmosphere_geoId_sch2915390_870755143")
                    .objectValue("Mean_PrecipitableWater_Atmosphere | geoId/sch2915390 | 870755143")
                    .provenance("dc/base/NOAA_GFS_WeatherForecast")
                    .build())
            .addEdge(
                Edge.builder()
                    .subjectId("dc/os/Mean_PrecipitableWater_Atmosphere_geoId_sch2915390_870755143")
                    .predicate("typeOf")
                    .objectId("StatVarObsSeries")
                    .objectValue("")
                    .provenance("dc/base/NOAA_GFS_WeatherForecast")
                    .build());

    List<Observation> actual = reader.parseTimeSeriesRow(row);

    assertEquals(expected, actual);

    assertEquals(expectedGraph, actual.get(0).getObsGraph());
  }

  private static CacheReader newCacheReader() {
    return new CacheReader("datcom-store");
  }
}
