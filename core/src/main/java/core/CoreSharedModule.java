package core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import core.metadata.DiffWithMetadata;
import core.snippet.SnippetCacheKey;
import dagger.Module;
import dagger.Provides;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

@Module
public class CoreSharedModule {

  @Provides
  @Singleton
  public static Cache<SnippetCacheKey, DiffWithMetadata> provideSnippetCache() {
    return CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(1, TimeUnit.MINUTES).build();
  }
}
