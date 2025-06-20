package org.datacommons.ingestion.pipeline;

import static org.datacommons.ingestion.pipeline.SkipProcessing.SKIP_GRAPH;
import static org.datacommons.ingestion.pipeline.SkipProcessing.SKIP_OBS;

import com.google.cloud.spanner.Mutation;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.*;
import org.datacommons.ingestion.data.CacheReader;
import org.datacommons.ingestion.data.ImportGroupVersions;
import org.datacommons.ingestion.data.NodesEdges;
import org.datacommons.ingestion.data.Observation;
import org.datacommons.ingestion.spanner.SpannerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Transforms and DoFns for the ingestion pipeline. */
public class Transforms {
  private static final Logger LOGGER = LoggerFactory.getLogger(Transforms.class);

  static class CacheRowKVMutationsDoFn extends DoFn<String, KV<String, Mutation>> {
    private static final Counter DUPLICATE_OBS_COUNTER =
        Metrics.counter(CacheRowKVMutationsDoFn.class, "dc_duplicate_obs_creation");
    private static final Counter DUPLICATE_NODES_COUNTER =
        Metrics.counter(CacheRowKVMutationsDoFn.class, "dc_duplicate_nodes_creation");
    private static final Counter DUPLICATE_EDGES_COUNTER =
        Metrics.counter(CacheRowKVMutationsDoFn.class, "dc_duplicate_edges_creation");

    private final CacheReader cacheReader;
    private final SpannerClient spannerClient;
    private final SkipProcessing skipProcessing;
    private final boolean writeObsGraph;
    private final TupleTag<KV<String, Mutation>> graphTag;
    private final TupleTag<KV<String, Mutation>> observationTag;
    // Using a bounded cache to prevent excessive memory consumption.
    // As these are bundle-level caches, their size depends on the bundle's data.
    // Large caches can block memory for the bundle's duration, risking OOM errors,
    // GC thrashing, or worker failures.
    // Note: Increasing capacity can enhance filtering performance, but might require more worker
    // memory.
    private final LRUCache<String, Boolean> seenNodes = new LRUCache<>(5_000_000);
    private final LRUCache<String, Boolean> seenEdges = new LRUCache<>(5_000_000);
    private final LRUCache<String, Boolean> seenObs = new LRUCache<>(5_000_000);

    private CacheRowKVMutationsDoFn(
        CacheReader cacheReader,
        SpannerClient spannerClient,
        SkipProcessing skipProcessing,
        boolean writeObsGraph,
        TupleTag<KV<String, Mutation>> graphTag,
        TupleTag<KV<String, Mutation>> observationTag) {
      this.cacheReader = cacheReader;
      this.spannerClient = spannerClient;
      this.skipProcessing = skipProcessing;
      this.writeObsGraph = writeObsGraph;
      this.graphTag = graphTag;
      this.observationTag = observationTag;
    }

    @StartBundle
    public void startBundle() {
      seenNodes.clear();
      seenObs.clear();
      seenEdges.clear();
    }

    @FinishBundle
    public void finishBundle() {
      seenNodes.clear();
      seenObs.clear();
      seenEdges.clear();
    }

    @ProcessElement
    public void processElement(@Element String row, MultiOutputReceiver out) {
      if (CacheReader.isArcCacheRow(row) && skipProcessing != SKIP_GRAPH) {
        NodesEdges nodesEdges = cacheReader.parseArcRow(row);
        outputGraphMutations(nodesEdges, out);
      } else if (CacheReader.isObsTimeSeriesCacheRow(row) && skipProcessing != SKIP_OBS) {
        var obs = cacheReader.parseTimeSeriesRow(row);
        var kvs = spannerClient.toObservationKVMutations(obs);
        var filtered = spannerClient.filterObservationKVMutations(kvs, seenObs);
        filtered.forEach(out.get(observationTag)::output);

        var dups = kvs.size() - filtered.size();
        if (dups > 0) {
          DUPLICATE_OBS_COUNTER.inc(dups);
        }

        if (writeObsGraph) {
          obs.stream()
              .map(Observation::getObsGraph)
              .forEach(obsGraph -> outputGraphMutations(obsGraph, out));
        }
      }
    }

    private void outputGraphMutations(NodesEdges nodesEdges, MultiOutputReceiver out) {
      var kvs = spannerClient.toGraphKVMutations(nodesEdges.getNodes(), nodesEdges.getEdges());
      var filtered =
          spannerClient.filterGraphKVMutations(
              kvs, seenNodes, seenEdges, DUPLICATE_NODES_COUNTER, DUPLICATE_EDGES_COUNTER);
      filtered.forEach(out.get(graphTag)::output);
    }
  }

  static class ExtractKVMutationsDoFn extends DoFn<KV<String, Iterable<Mutation>>, Mutation> {
    private static final Counter DUPLICATE_OBS_COUNTER =
        Metrics.counter(ExtractKVMutationsDoFn.class, "dc_duplicate_obs_extraction");
    private static final Counter DUPLICATE_NODES_COUNTER =
        Metrics.counter(ExtractKVMutationsDoFn.class, "dc_duplicate_nodes_extraction");
    private static final Counter DUPLICATE_EDGES_COUNTER =
        Metrics.counter(ExtractKVMutationsDoFn.class, "dc_duplicate_edges_extraction");

    private final SpannerClient spannerClient;

    public ExtractKVMutationsDoFn(SpannerClient spannerClient) {
      this.spannerClient = spannerClient;
    }

    @ProcessElement
    public void processElement(
        @Element KV<String, Iterable<Mutation>> kv, OutputReceiver<Mutation> out) {
      // Storing caches at the bundle level can create long-lived objects, potentially leading to
      // performance issues such as frequent garbage collection (GC) and thrashing. Hence,
      // maintaining at key level.
      // Following de-duplication approach only removes duplicates among values for a
      // single key. For effective de-duplication, the grouping key should be a subset of the
      // primary keys from the relevant tables (e.g. edges, nodes, observations).
      final Set<String> seenNodes = new HashSet<>();
      final Set<String> seenObs = new HashSet<>();
      final Set<String> seenEdges = new HashSet<>();
      for (var mutation : kv.getValue()) {
        if (mutation.getTable().equals(spannerClient.getNodeTableName())) {
          var subjectId = SpannerClient.getSubjectId(mutation);
          if (seenNodes.contains(subjectId)) {
            DUPLICATE_NODES_COUNTER.inc();
            continue;
          }
          seenNodes.add(subjectId);
        }
        if (mutation.getTable().equals(spannerClient.getEdgeTableName())) {
          var edgeKey = SpannerClient.getEdgeKey(mutation);
          if (seenEdges.contains(edgeKey)) {
            DUPLICATE_EDGES_COUNTER.inc();
            continue;
          }
          seenEdges.add(edgeKey);
        } else if (mutation.getTable().equals(spannerClient.getObservationTableName())) {
          var key = SpannerClient.getFullObservationKey(mutation);
          if (seenObs.contains(key)) {
            DUPLICATE_OBS_COUNTER.inc();
            continue;
          }
          seenObs.add(key);
        }
        out.output(mutation);
      }
    }
  }

  static class ImportGroupTransform extends PTransform<PCollection<String>, PCollection<Void>> {
    private final CacheReader cacheReader;
    private final SpannerClient spannerClient;
    private final SkipProcessing skipProcessing;
    private final boolean writeObsGraph;

    public ImportGroupTransform(
        CacheReader cacheReader,
        SpannerClient spannerClient,
        SkipProcessing skipProcessing,
        boolean writeObsGraph) {
      this.cacheReader = cacheReader;
      this.spannerClient = spannerClient;
      this.skipProcessing = skipProcessing;
      this.writeObsGraph = writeObsGraph;
    }

    @Override
    public PCollection<Void> expand(PCollection<String> cacheRows) {
      // While a separate method is not required here, doing so makes it easier to develop and test
      // with other strategies.

      return groupByGraphOnly(cacheRows);
    }

    private PCollection<Void> groupByGraphOnly(PCollection<String> cacheRows) {
      var observationTag = new TupleTag<KV<String, Mutation>>() {};
      var graphTag = new TupleTag<KV<String, Mutation>>() {};
      var kvs =
          cacheRows.apply(
              "CreateMutations",
              ParDo.of(
                      new CacheRowKVMutationsDoFn(
                          cacheReader,
                          spannerClient,
                          skipProcessing,
                          writeObsGraph,
                          graphTag,
                          observationTag))
                  .withOutputTags(graphTag, TupleTagList.of(observationTag)));

      var observations =
          kvs.get(observationTag).apply("ExtractObservationMutations", Values.create());
      // TODO: Explore emitting protos instead of mutations to reduce shuffle cost.
      var graph =
          kvs.get(graphTag)
              .apply("GroupGraphMutations", GroupByKey.create())
              .apply("ExtractGraphMutations", ParDo.of(new ExtractKVMutationsDoFn(spannerClient)));

      var write =
          PCollectionList.of(graph)
              .and(observations)
              .apply("MergeMutations", Flatten.<Mutation>pCollections())
              .apply("WriteToSpanner", spannerClient.getWriteTransform());
      return write.getOutput();
    }
  }

  static void buildImportGroupPipeline(
      Pipeline pipeline,
      String importGroupVersion,
      CacheReader cacheReader,
      SpannerClient spannerClient) {
    var options = pipeline.getOptions().as(IngestionPipelineOptions.class);
    var importGroupName = ImportGroupVersions.getImportGroupName(importGroupVersion);
    var importGroupFilePath = cacheReader.getImportGroupCachePath(importGroupVersion);
    pipeline
        .apply("Read: " + importGroupName, TextIO.read().from(importGroupFilePath))
        .apply(
            "Ingest: " + importGroupName,
            new Transforms.ImportGroupTransform(
                cacheReader,
                spannerClient,
                options.getSkipProcessing(),
                options.getWriteObsGraph()));
  }

  static void buildIngestionPipeline(
      Pipeline pipeline,
      List<String> importGroupVersions,
      CacheReader cacheReader,
      SpannerClient spannerClient) {
    for (var importGroupVersion : importGroupVersions) {
      buildImportGroupPipeline(pipeline, importGroupVersion, cacheReader, spannerClient);
    }
  }

  /**
   * A simple LRU cache with a fixed capacity. Note:
   *
   * <p>Not thread-safe. For single-threaded use only.
   *
   * <p>To check for key existence in an `LRUCache` and maintain LRU order, use `get(key) != null`.
   * This method updates the key's usage, unlike `containsKey()`, which doesn't and would therefore
   * disrupt the LRU sequence.
   */
  private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCache(int capacity) {
      super(capacity, 0.75f, true); // accessOrder = true
      this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
      return size() > capacity;
    }
  }
}
