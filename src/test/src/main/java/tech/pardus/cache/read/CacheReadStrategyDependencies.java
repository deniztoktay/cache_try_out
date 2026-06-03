package tech.pardus.cache.read;

import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.l1.TieredReactiveCacheReader;
import tech.pardus.cache.l2.L2SingleValueLoader;
import tech.pardus.redis.api.CacheIndexStore;
import tech.pardus.redis.api.RedisValueStore;
import tech.pardus.redis.cache.CacheValueCodec;
import tech.pardus.redis.cache.Identifiable;
import tech.pardus.redis.cache.ReactiveCacheReader;
import tech.pardus.redis.coordination.CacheL2ReadyMarker;

/** Optional beans used by {@link CacheReadStrategyFactory}. */
public record CacheReadStrategyDependencies<ID, M extends Identifiable<ID>>(
    ReactiveCacheReader<M> l2Reader,
    TieredReactiveCacheReader<M> tieredReader,
    ResizableL1Cache<String, M> l1Cache,
    L2SingleValueLoader<M> l2Loader,
    CacheL2ReadyMarker l2ReadyMarker,
    RedisValueStore valueStore,
    CacheIndexStore indexStore,
    CacheValueCodec<M> codec) {

  public static <ID, M extends Identifiable<ID>> Builder<ID, M> builder() {
    return new Builder<>();
  }

  public static final class Builder<ID, M extends Identifiable<ID>> {
    private ReactiveCacheReader<M> l2Reader;
    private TieredReactiveCacheReader<M> tieredReader;
    private ResizableL1Cache<String, M> l1Cache;
    private L2SingleValueLoader<M> l2Loader;
    private CacheL2ReadyMarker l2ReadyMarker;
    private RedisValueStore valueStore;
    private CacheIndexStore indexStore;
    private CacheValueCodec<M> codec;

    public Builder<ID, M> l2Reader(ReactiveCacheReader<M> l2Reader) {
      this.l2Reader = l2Reader;
      return this;
    }

    public Builder<ID, M> tieredReader(TieredReactiveCacheReader<M> tieredReader) {
      this.tieredReader = tieredReader;
      return this;
    }

    public Builder<ID, M> l1Cache(ResizableL1Cache<String, M> l1Cache) {
      this.l1Cache = l1Cache;
      return this;
    }

    public Builder<ID, M> l2Loader(L2SingleValueLoader<M> l2Loader) {
      this.l2Loader = l2Loader;
      return this;
    }

    public Builder<ID, M> l2ReadyMarker(CacheL2ReadyMarker l2ReadyMarker) {
      this.l2ReadyMarker = l2ReadyMarker;
      return this;
    }

    public Builder<ID, M> valueStore(RedisValueStore valueStore) {
      this.valueStore = valueStore;
      return this;
    }

    public Builder<ID, M> indexStore(CacheIndexStore indexStore) {
      this.indexStore = indexStore;
      return this;
    }

    public Builder<ID, M> codec(CacheValueCodec<M> codec) {
      this.codec = codec;
      return this;
    }

    public CacheReadStrategyDependencies<ID, M> build() {
      return new CacheReadStrategyDependencies<>(
          l2Reader, tieredReader, l1Cache, l2Loader, l2ReadyMarker, valueStore, indexStore, codec);
    }
  }
}
