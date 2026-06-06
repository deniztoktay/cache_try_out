package tech.pardus.newdesign.l1;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * L1 holds all entities in an id-keyed map ({@code id -> entity}). Lookups use ids or predicates,
 * not parent keys. Ids match L2 member ids ({@link tech.pardus.newdesign.cachekey.CacheEntityId}).
 */
public interface L1EntityMapCache<T> {

  Optional<T> getById(String id);

  Map<String, T> asMap();

  void put(String id, T value);

  void putAll(Map<String, T> entries);

  void remove(String id);

  void clear();

  default List<T> find(Predicate<T> predicate) {
    return asMap().values().stream().filter(predicate).toList();
  }
}
