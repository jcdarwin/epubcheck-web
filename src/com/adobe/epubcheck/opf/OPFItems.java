package com.adobe.epubcheck.opf;

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.mola.galimatias.URL;

/**
 * Represents the set of Publication Resources in a Package Document (OPF).
 */
public final class OPFItems
{

  private final List<OPFItem> items;
  private final List<OPFItem> spine;
  private final Map<String, OPFItem> itemsById;
  private final Map<URL, OPFItem> itemsByURL;

  /**
   * Search the item with the given ID.
   * 
   * @param id
   *        the ID of the item to search, can be <code>null</code>.
   * @return An {@link Optional} containing the item if found, or
   *           {@link Optional#absent()} if not found.
   */
  public Optional<OPFItem> getItemById(String id)
  {
    return Optional.fromNullable(itemsById.get(id));
  }

  /**
   * Search the item with the given path.
   * 
   * @param id
   *        the URL of the item to search, can be <code>null</code>.
   * @return An {@link Optional} containing the item if found, or
   *           {@link Optional#absent()} if not found.
   */
  public Optional<OPFItem> getItemByURL(URL url)
  {
    return Optional.fromNullable(itemsByURL.get(url));
  }

  /**
   * Returns the list of items in the spine. A single {@link OPFItem} instance
   * can appear multiple times in the list.
   * 
   * @return the list of items in the spine.
   */
  public List<OPFItem> getSpineItems()
  {
    return spine;
  }

  /**
   * Returns the list of items in this set, in document order.
   * 
   * @return the list of items in this set.
   */
  public List<OPFItem> getItems()
  {
    return items;
  }

  private OPFItems(Iterable<OPFItem> items, Iterable<String> spineIDs)
  {
    this.items = ImmutableList.copyOf(Preconditions.checkNotNull(items));
    // Build the by-ID and by-Paths maps
    // We use temporary HashMaps to ignore potential duplicate keys
    Map<String, OPFItem> itemsById = Maps.newHashMap();
    Map<URL, OPFItem> itemsByURL = Maps.newHashMap();
    for (OPFItem item : this.items)
    {
      itemsById.put(item.getId(), item);
      itemsByURL.put(item.getURL(), item);
    }
    this.itemsById = ImmutableMap.copyOf(itemsById);
    this.itemsByURL = ImmutableMap.copyOf(itemsByURL);
    // Build the spine view
    this.spine = FluentIterable.from(spineIDs).transform(new Function<String, OPFItem>()
    {
      @Override
      public OPFItem apply(final String id)
      {
        return OPFItems.this.itemsById.get(id.trim());
      }
    }).filter(Predicates.notNull()).toList();
  }

  /**
   * Creates a consolidated set of {@link OPFItem} from item builders and a list
   * of spine item IDs.
   * 
   * @param itemBuilders
   *        the builders of the {@link OPFItem} in the set.
   * @param spineIDs
   *        the IDs of the items in the spine.
   * @param context
   * @return a consolidated set of {@link OPFItem}s.
   */
  public static OPFItems build(Map<String, OPFItem.Builder> itemBuilders, Iterable<String> spineIDs,
      ValidationContext context)
  {
    return new OPFItems(new FallbackChainResolver(itemBuilders, context).resolve(),
        Preconditions.checkNotNull(spineIDs));
  }
}
