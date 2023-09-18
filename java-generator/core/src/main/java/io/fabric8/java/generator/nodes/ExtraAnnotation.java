package io.fabric8.java.generator.nodes;

import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class ExtraAnnotation {
    public static ExtraAnnotationProvider provider() {
      return provider(null);
    }

    //provider by name
    public static ExtraAnnotationProvider provider(String providerName) {
      ServiceLoader<ExtraAnnotationProvider> loader = ServiceLoader.load(ExtraAnnotationProvider.class);
      Iterator<ExtraAnnotationProvider> it = loader.iterator();
      while (it.hasNext()) {
        ExtraAnnotationProvider provider = it.next();
        if (providerName == null || providerName.equals(provider.getClass().getName())) {
          return provider;
        }
      }
      throw new ProviderNotFoundException("Extra annotation provider " + providerName + " not found");
    }
  }
}
