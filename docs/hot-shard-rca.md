# Hot Shard RCA

## Definition

The idea behind HotShard RCA is to identify a hot shard within an index. A Hot shard is a resource consuming outlier within its counterparts. The RCA subscribes to following metrics:

1. `CPU_Utilization`
2. `Heap_AllocRate`.

The RCA looks at the above two metric data and compares the values against the threshold for each resource and if the usage for any of the resources is greater than their individual threshold, we mark the context as 'UnHealthy' and create a `HotShardResourceSummary` for the shard.

In order to have a full picture of the index-level shard stats, and to detect outlies, cluster variant - `HotShardClusterRCA` is to be used as an downstream RCA to the `HotShardRCA`s running on each Data node.

In the future, some metric dependant on disk read/write intensity is to be included, potential candidates being:
1. `Thread_Blocked_Time`
2. `IO_TotThroughput `


## Implementation overview

In every RCA period, all existing shards are taken into account within this RCA. As this number can go up to 1000 per Node and thus can create huge memory footprint for both the Cluster Manager and Data nodes, if implementation is handled naively.

To minimize the core of the footprint (memory allocated for the whole duration of each RCA period which directly scales with number of shards), a single map, mapping unique Index - Shard combination to the highly specific strucutre, called `SummarizedWindow`, is used.

HotShardRCA period consists of `SLIDING_WINDOW_IN_SECONDS/5` (default being `12`) operate ticks, each of them consuming metric aggregations from previous `5` seconds, more precisely the `SUM` aggregation of each metric. `SummarizedWindow` accumulates these aggregation over the period of `SLIDING_WINDOW_IN_SECONDS` (by default 1 minute) and is later used to calculate the over time average of these accumulated metrics. Note that summarization would give us a little bit less information than the average value as some shards may start or stop being active anywhere inside the RCA period. This is why more general case structures like `SlidingWindow` offer us a granularity that we don't need and by omitting them, considerable amount of heap is saved. Also, inside the same single `SummarizedWindow`, all metrics are getting aggregated at once, this way eliminating duplicated timestamps and shard identification data.

## _Picture placeholder - structure layout_

In high workload enviroments, there is a possibility of a worst case scenario, where all of the shards cross the thresholds and must be sent to the Cluster Manager and thus potentially creating high network traffic as well as very high heap usage on the Cluster Manager node that is supposed to collect all the cluster data. To account for this possiblity, Top-K consumers heuristic is used.

Top-K consumers is setting a limit to a maximum number of Hot Shards being sent over the network to the cluster Manager. This is also configurable through .conf files, default being 50. The idea is that we have a K << N for extreme N's where N represents number of shards on Node.

The Top-K heuristic is done by triage-like process on map iteration. Each shard that crosses a certain metrics threshold goes into one of two bounded MinMaxPriorityQueues (a queue for each metric, and their capacities limited by K parameter), note that one shard can end up in both queues if both of its metrics cross their respective thresholds. This way, during the process, only the consumers that have a chance of being sent to the Cluster Manager are being stored plus saving memory and having a memory size - fixed structure under the queues' implementations. We also pass references to the same SummarizedWindow structures to both queues and only give them different comparator objects, this also saves heap. At the end, union of queues' elements is being created and it will form the summaries sent to the HotShardClusterRCA. Note that due to reference sharing all metric info is preserved even if consumer is present only in one queue after the triage.

## _Tuning placeholder_
