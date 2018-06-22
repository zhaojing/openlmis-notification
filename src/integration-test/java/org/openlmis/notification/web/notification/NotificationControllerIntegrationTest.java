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

package org.openlmis.notification.web.notification;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_CONTENT_REQUIRED;
import static org.openlmis.notification.i18n.MessageKeys.PERMISSION_MISSING_GENERIC;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.jayway.restassured.response.Response;
import guru.nidi.ramltester.junit.RamlMatchers;
import java.util.UUID;
import javax.mail.MessagingException;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.UserContactDetailsRepository;
import org.openlmis.notification.service.NotificationService;
import org.openlmis.notification.service.referencedata.UserDto;
import org.openlmis.notification.service.referencedata.UserReferenceDataService;
import org.openlmis.notification.testutils.UserDataBuilder;
import org.openlmis.notification.util.UserContactDetailsDataBuilder;
import org.openlmis.notification.web.BaseWebIntegrationTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;

@SuppressWarnings("PMD.TooManyMethods")
public class NotificationControllerIntegrationTest extends BaseWebIntegrationTest {
  private static final String RESOURCE_URL = "/api/notifications";
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String SUBJECT = "subject";
  private static final String CONTENT = "content";

  @MockBean
  private NotificationService notificationService;

  @MockBean
  private UserContactDetailsRepository userContactDetailsRepository;

  @MockBean
  private UserReferenceDataService userReferenceDataService;

  @Value("${email.noreply}")
  private String defaultFrom;

  private UserContactDetails contactDetails = new UserContactDetailsDataBuilder()
      .withReferenceDataUserId(USER_ID)
      .build();
  private UserDto user = new UserDataBuilder().build();

  @Before
  public void setUp() {
    given(userContactDetailsRepository.findOne(USER_ID)).willReturn(contactDetails);
    given(userReferenceDataService.findOne(USER_ID)).willReturn(user);
  }

  @Test
  public void shouldSendMessageForValidNotification() throws MessagingException {
    String from = "example@test.org";

    send(from, CONTENT, getServiceTokenHeader())
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    verify(notificationService)
        .sendNotification(from, contactDetails.getEmailAddress(), SUBJECT, CONTENT);
  }

  @Test
  public void shouldSendMessageForValidNotificationWithNullFrom() throws MessagingException {
    send(null, CONTENT, getServiceTokenHeader())
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    verify(notificationService)
        .sendNotification(defaultFrom, contactDetails.getEmailAddress(), SUBJECT, CONTENT);
  }

  @Test
  public void shouldNotSendMessageForInvalidNotification() {
    send(null, null, getServiceTokenHeader())
        .then()
        .statusCode(400)
        .body(MESSAGE_KEY, is(ERROR_CONTENT_REQUIRED));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.validates());

    verifyZeroInteractions(
        notificationService, userContactDetailsRepository, userReferenceDataService
    );
  }

  @Test
  public void shouldNotSendMessageIfUserContactDetailsDoesNotExist() {
    given(userContactDetailsRepository.findOne(USER_ID)).willReturn(null);

    send(null, CONTENT, getServiceTokenHeader())
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());

    verifyZeroInteractions(notificationService, userReferenceDataService);
  }

  @Test
  public void shouldNotSendMessageIfUserEmailIsNotVerified() {
    contactDetails.getEmailDetails().setEmailVerified(false);

    send(null, CONTENT, getServiceTokenHeader())
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());

    verifyZeroInteractions(notificationService, userReferenceDataService);
  }

  @Test
  public void shouldNotSendMessageIfUserIsNotActive() {
    user.setActive(false);

    send(null, CONTENT, getServiceTokenHeader())
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());

    verifyZeroInteractions(notificationService);
  }

  @Test
  public void shouldNotSendMessageForUserRequest() {
    send(null, CONTENT, getUserTokenHeader())
        .then()
        .statusCode(403)
        .body(MESSAGE_KEY, is(PERMISSION_MISSING_GENERIC));
  }

  @Test
  public void shouldNotSendMessageIfRequestTokenIsInvalid() {
    send(null, CONTENT, null)
        .then()
        .statusCode(401);
  }

  private Response send(String from, String content, String token) {
    return startRequest(token)
        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .body(new NotificationDto(from, USER_ID, SUBJECT, content))
        .when()
        .post(RESOURCE_URL);
  }

}