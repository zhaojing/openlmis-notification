/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.testutils.DigestConfigurationDataBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

public class DigestConfigurationRepositoryIntegrationTest
    extends BaseCrudRepositoryIntegrationTest<DigestConfiguration, UUID> {

  private static final int COUNT = 10;

  @Autowired
  private DigestConfigurationRepository repository;

  @Override
  CrudRepository<DigestConfiguration, UUID> getRepository() {
    return repository;
  }

  @Override
  DigestConfiguration generateInstance() {
    return new DigestConfigurationDataBuilder().buildAsNew();
  }

  private List<DigestConfiguration> configurations;

  @Before
  public void setUp() {
    configurations = IntStream
        .range(0, COUNT)
        .mapToObj(idx -> generateInstance())
        .peek(repository::save)
        .collect(Collectors.toList());
  }

  @Test
  public void shouldFindDigestConfigurationBySingleTag() {
    for (int i = 0; i < COUNT; ++i) {
      // given
      String tag = configurations.get(i).getTag();

      // when
      DigestConfiguration found = repository.findByTag(tag);

      // then
      assertThat(found).isEqualTo(configurations.get(i));
    }
  }

  @Test
  public void shouldNotFindDigestConfigurationIfTagIsIncorrect() {
    assertThat(repository.findByTag("integration-test-incorrect-tag")).isNull();
  }

}
