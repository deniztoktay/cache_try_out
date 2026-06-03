package tech.pardus.cache.tier;

/** Cache capability level for an entity's view and save services. */
public enum CacheServiceTier {
  /** View and save always use the database. */
  DB_ONLY,
  /** Shared Redis (L2) cache; read: L2 then DB; write: update L2 after commit. */
  L2_ONLY,
  /** Local L1 + Redis L2 + DB; write publishes change stream for L1 projection on all pods. */
  L1_L2
}
