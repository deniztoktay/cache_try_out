package tech.pardus.jdbc.tag.view;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.projection.CacheChangeStreamMessage;
import tech.pardus.cache.projection.L1ProjectionHandler;
import tech.pardus.cache.projection.L1ProjectionSupport;
import tech.pardus.cache.read.CacheReadStrategy;
import tech.pardus.tag.model.TagModel;

public class TagL1ProjectionHandler implements L1ProjectionHandler {

  private static final Duration PROJECTION_TIMEOUT = Duration.ofSeconds(15);

  private final CacheReadStrategy<Integer, TagModel> readStrategy;
  private final ResizableL1Cache<String, TagModel> l1Cache;

  public TagL1ProjectionHandler(
      CacheReadStrategy<Integer, TagModel> readStrategy,
      ResizableL1Cache<String, TagModel> l1Cache) {
    this.readStrategy = readStrategy;
    this.l1Cache = l1Cache;
  }

  @Override
  public void onChange(CacheChangeStreamMessage message) {
    L1ProjectionSupport.applyChange(
        message,
        l1Cache,
        Integer::parseInt,
        readStrategy::loadForL1Projection,
        TagL1ProjectionHandler::l1Keys,
        PROJECTION_TIMEOUT);
  }

  static List<String> l1Keys(TagModel model) {
    var keys = new ArrayList<String>();
    keys.add(model.getStringId());
    if (model.name() != null && !model.name().isBlank()) {
      keys.add(model.nameAliasStringId());
    }
    return keys;
  }
}
