package tech.pardus.jdbc.attributetag.view;

import java.time.Duration;
import java.util.List;
import tech.pardus.attributetag.model.AttributeTagModel;
import tech.pardus.cache.l1.ResizableL1Cache;
import tech.pardus.cache.projection.CacheChangeStreamMessage;
import tech.pardus.cache.projection.L1ProjectionHandler;
import tech.pardus.cache.projection.L1ProjectionSupport;
import tech.pardus.cache.read.CacheReadStrategy;

public class AttributeTagL1ProjectionHandler implements L1ProjectionHandler {

  private static final Duration PROJECTION_TIMEOUT = Duration.ofSeconds(15);

  private final CacheReadStrategy<String, AttributeTagModel> readStrategy;
  private final ResizableL1Cache<String, AttributeTagModel> l1Cache;

  public AttributeTagL1ProjectionHandler(
      CacheReadStrategy<String, AttributeTagModel> readStrategy,
      ResizableL1Cache<String, AttributeTagModel> l1Cache) {
    this.readStrategy = readStrategy;
    this.l1Cache = l1Cache;
  }

  @Override
  public void onChange(CacheChangeStreamMessage message) {
    L1ProjectionSupport.applyChange(
        message,
        l1Cache,
        memberId -> memberId,
        readStrategy::loadForL1Projection,
        model -> List.of(model.getStringId()),
        PROJECTION_TIMEOUT);
  }
}
