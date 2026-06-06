package tech.pardus.newdesign.read;

/** How a cache partition is read. */
public enum ReadTier {
  DB_ONLY,
  L2_ONLY,
  L1_L2
}
