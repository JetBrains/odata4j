package org.odata4j.core;

/**
 * Marks atom entity to provide custom content in atom stream.
 * @since 0.6
 */
public interface OAtomStreamEntity extends OExtension<OEntity> {

  /**
   * @return content-type for the entity. @NotNull
   * @since 0.6
   */
  String getAtomEntityType();

  /**
  * @param baseUrl generating feed base uri ending with '/'
  * @return src attribute value. This should be a url to fetch binary content. @NotNull
  * @since 0.6
  */
  String getAtomEntitySource(String baseUrl);
}