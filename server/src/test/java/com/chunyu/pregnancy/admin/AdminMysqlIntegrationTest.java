package com.chunyu.pregnancy.admin;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.common.ratelimit.AdminLoginRateLimiter;
import com.chunyu.pregnancy.admin.config.AdminAuthProperties;
import com.chunyu.pregnancy.admin.config.AdminBootstrap;
import com.chunyu.pregnancy.admin.service.support.AuditWriter;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 真实MySQL/Flyway/Redis/Security流程测试。只允许清理专用测试库，默认不运行。 这里创建的账号和内容都是测试夹具，不会由生产接口自动生成，更不代表临床审核样本。 */
@EnabledIfEnvironmentVariable(named = "RUN_ADMIN_MYSQL_IT", matches = "true")
@ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:mysql://localhost:3306/cy_pregnancy_admin_test?serverTimezone=UTC",
      "spring.datasource.username=root",
      "spring.datasource.password=root",
      "app.auth.secret=integration-admin-test-only-secret-with-32-bytes",
      "app.auth.bootstrap-enabled=false",
      "CONTENT_READ_KEY=integration-content-only-secret-32-bytes"
    })
@AutoConfigureMockMvc
class AdminMysqlIntegrationTest {
  @Test
  void publicProjectionOnlyReadsCurrentPublishedVersionAndWithdrawsImmediately() throws Exception {
    String key = "integration-content-only-secret-32-bytes";
    mvc.perform(get("/internal/content/knowledge")).andExpect(status().isForbidden());
    String editToken = login(editor),
        reviewToken = login(reviewer),
        publishToken = login(publisher);
    JsonNode created = draft(editToken);
    String version = created.get("uuid").asText();
    String article =
        jdbc.queryForObject(
            "SELECT client_uuid FROM admin_knowledge_article LIMIT 1", String.class);
    mvc.perform(get("/internal/content/knowledge/" + article).header("X-Content-Read-Key", key))
        .andExpect(status().isNotFound());
    submit(editToken, version, 1);
    review(reviewToken, version, "approve");
    call(
        write(
            post("/admin/api/v1/knowledge/versions/" + version + "/publish"),
            publishToken,
            3,
            UUID.randomUUID().toString(),
            null),
        200);
    mvc.perform(get("/internal/content/knowledge/" + article).header("X-Content-Read-Key", key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.proContent").isString())
        .andExpect(jsonPath("$.data.references").isArray())
        .andExpect(jsonPath("$.data.publishedAt").value(org.hamcrest.Matchers.endsWith("Z")))
        .andExpect(jsonPath("$.data.authorUuid").doesNotExist())
        .andExpect(jsonPath("$.data.reviewNote").doesNotExist());
    mvc.perform(
            get("/internal/content/knowledge")
                .header("X-Content-Read-Key", key)
                .param("q", "中文%_!"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records.length()").value(0));
    mvc.perform(
            get("/internal/content/knowledge")
                .header("X-Content-Read-Key", key)
                .param("limit", "101"))
        .andExpect(status().isUnprocessableEntity());
    long revision =
        jdbc.queryForObject(
            "SELECT revision FROM admin_knowledge_article WHERE client_uuid=?",
            Long.class,
            article);
    call(
        write(
            post("/admin/api/v1/knowledge/" + article + "/withdraw"),
            publishToken,
            revision,
            UUID.randomUUID().toString(),
            null),
        200);
    mvc.perform(get("/internal/content/knowledge/" + article).header("X-Content-Read-Key", key))
        .andExpect(status().isNotFound());
    mvc.perform(get("/internal/content/knowledge").header("X-Content-Read-Key", key))
        .andExpect(jsonPath("$.data.records.length()").value(0));
  }

  private static final String PASSWORD = "Fixture-only-password-2026!";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired PasswordEncoder passwords;
  @Autowired AdminLoginRateLimiter limiter;
  @Autowired AdminBootstrap bootstrap;
  @Autowired AdminAuthProperties properties;
  @SpyBean AuditWriter audit;
  private String security;
  private String editor;
  private String reviewer;
  private String publisher;
  private final Map<String, String> uuids = new HashMap<>();

  @BeforeEach
  void fixtures() {
    assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class))
        .isEqualTo("cy_pregnancy_admin_test");
    properties.setBootstrapEnabled(false);
    // 显式按外键顺序清理，绝不关闭外键检查或对开发库执行清空。
    jdbc.update("UPDATE admin_knowledge_article SET published_version_id=NULL");
    jdbc.update("DELETE FROM admin_knowledge_version");
    jdbc.update("DELETE FROM admin_knowledge_article");
    jdbc.update("DELETE FROM admin_idempotency");
    jdbc.update("DELETE FROM admin_audit_event");
    jdbc.update("DELETE FROM admin_session");
    jdbc.update("DELETE FROM admin_operator_role");
    jdbc.update("DELETE FROM admin_operator");
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String encoded = passwords.encode(PASSWORD);
    security = fixture("sec_" + suffix, encoded, "security_admin");
    editor = fixture("edit_" + suffix, encoded, "content_editor");
    reviewer = fixture("review_" + suffix, encoded, "clinical_reviewer");
    publisher = fixture("pub_" + suffix, encoded, "publisher");
  }

  @Test
  void webReadQueriesFilterBeforePaginationAndRespectPermissions() throws Exception {
    String editToken = login(editor),
        reviewToken = login(reviewer),
        securityToken = login(security);
    JsonNode first = draft(editToken);
    draft(editToken);
    // ASCII标识列与UTF-8中文标题/姓名混合搜索，不能因参数字符集转换而报500。
    call(
        get("/admin/api/v1/knowledge")
            .param("q", "孕期中文")
            .header("Authorization", "Bearer " + editToken),
        200);
    call(
        get("/admin/api/v1/operators")
            .param("q", "测试夹具")
            .header("Authorization", "Bearer " + securityToken),
        200);
    call(
        get("/admin/api/v1/audits")
            .param("q", "测试夹具")
            .header("Authorization", "Bearer " + securityToken),
        200);
    assertThat(
            call(
                    get("/admin/api/v1/knowledge")
                        .param("q", "%_!")
                        .header("Authorization", "Bearer " + editToken),
                    200)
                .get("records")
                .size())
        .isZero();
    JsonNode page =
        call(
            get("/admin/api/v1/knowledge")
                .param("q", "no-matching-title")
                .param("limit", "1")
                .header("Authorization", "Bearer " + editToken),
            200);
    assertThat(page.get("records").size()).isZero();
    JsonNode listing =
        call(
            get("/admin/api/v1/knowledge")
                .param("limit", "1")
                .header("Authorization", "Bearer " + editToken),
            200);
    assertThat(listing.get("records").size()).isEqualTo(1);
    assertThat(listing.get("records").get(0).get("currentlyPublished").isBoolean()).isTrue();
    assertThat(listing.get("nextBeforeId").asLong()).isPositive();
    submit(editToken, first.get("uuid").asText(), first.get("revision").asLong());
    assertThat(
            call(
                    get("/admin/api/v1/knowledge/review-queue")
                        .header("Authorization", "Bearer " + reviewToken),
                    200)
                .get("records")
                .size())
        .isEqualTo(1);
    call(
        get("/admin/api/v1/knowledge/review-queue").header("Authorization", "Bearer " + editToken),
        403);
    call(get("/admin/api/v1/users").header("Authorization", "Bearer " + editToken), 403);
    JsonNode account =
        call(
            get("/admin/api/v1/operators/" + uuids.get(editor))
                .header("Authorization", "Bearer " + securityToken),
            200);
    assertThat(account.has("passwordHash")).isFalse();
    assertThat(account.get("roleCodes").get(0).asText()).isEqualTo("content_editor");
    call(
        get("/admin/api/v1/audits")
            .param("start", "2026-10-20")
            .param("end", "2026-10-19")
            .header("Authorization", "Bearer " + securityToken),
        422);
    call(
        get("/admin/api/v1/knowledge")
            .param("limit", "101")
            .header("Authorization", "Bearer " + editToken),
        422);
    assertThat(
            call(
                    get("/admin/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + editToken),
                    200)
                .get("articles")
                .asInt())
        .isEqualTo(2);
  }

  private String fixture(String username, String encoded, String role) {
    String uuid = UUID.randomUUID().toString();
    uuids.put(username, uuid);
    jdbc.update(
        "INSERT INTO admin_operator(client_uuid,username,password_hash,display_name,state,revision,created_at) VALUES(?,?,?,?,'active',1,UTC_TIMESTAMP(6))",
        uuid,
        username,
        encoded,
        "测试夹具");
    jdbc.update(
        "INSERT INTO admin_operator_role(operator_id,role_code) SELECT id,? FROM admin_operator WHERE username=?",
        role,
        username);
    return username;
  }

  private String login(String username) throws Exception {
    return call(
            post("/admin/api/v1/auth/login")
                .with(
                    request -> {
                      request.setRemoteAddr("test-" + UUID.randomUUID());
                      return request;
                    })
                .contentType("application/json")
                .content(json.writeValueAsString(map("username", username, "password", PASSWORD))),
            200)
        .get("accessToken")
        .asText();
  }

  private JsonNode call(MockHttpServletRequestBuilder request, int status) throws Exception {
    String output =
        mvc.perform(request)
            .andExpect(status().is(status))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(output).get("data");
  }

  private MockHttpServletRequestBuilder write(
      MockHttpServletRequestBuilder request, String token, long revision, String key, Object body)
      throws Exception {
    request
        .header("Authorization", "Bearer " + token)
        .header("Idempotency-Key", key)
        .header("If-Match", "\"" + revision + "\"");
    if (body != null) {
      request.contentType("application/json").content(json.writeValueAsString(body));
    }
    return request;
  }

  private Map<String, Object> map(Object... pairs) {
    Map<String, Object> output = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      output.put((String) pairs[i], pairs[i + 1]);
    }
    return output;
  }

  private Map<String, Object> content() {
    return map(
        "title",
        "测试知识草稿",
        "category",
        "术语",
        "proContent",
        "仅用于工程测试的专业表达",
        "plainContent",
        "仅用于工程测试的白话表达",
        "references",
        Collections.singletonList("测试引用，不是临床证据"),
        "evidenceNote",
        "工程测试用途");
  }

  private Map<String, Object> newArticle() {
    Map<String, Object> body = content();
    body.put("clientUuid", UUID.randomUUID().toString());
    body.put("versionUuid", UUID.randomUUID().toString());
    body.put("slug", "fixture-" + UUID.randomUUID());
    return body;
  }

  private JsonNode draft(String token) throws Exception {
    return call(
        write(
            post("/admin/api/v1/knowledge"), token, 1, UUID.randomUUID().toString(), newArticle()),
        200);
  }

  private JsonNode submit(String token, String uuid, long revision) throws Exception {
    return call(
        write(
            post("/admin/api/v1/knowledge/versions/" + uuid + "/submit"),
            token,
            revision,
            UUID.randomUUID().toString(),
            null),
        200);
  }

  private JsonNode review(String token, String uuid, String decision) throws Exception {
    return call(
        write(
            post("/admin/api/v1/knowledge/versions/" + uuid + "/review"),
            token,
            2,
            UUID.randomUUID().toString(),
            map("decision", decision, "note", "测试人员的审核说明")),
        200);
  }

  @Test
  void anonymousCannotReadManagementResources() throws Exception {
    mvc.perform(get("/admin/api/v1/operators"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  void loginReturnsMinimalIdentityAndLogoutRevokesJwt() throws Exception {
    String token = login(security);
    mvc.perform(get("/admin/api/v1/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.operator.passwordHash").doesNotExist());
    call(post("/admin/api/v1/auth/logout").header("Authorization", "Bearer " + token), 200);
    call(get("/admin/api/v1/me").header("Authorization", "Bearer " + token), 401);
  }

  @Test
  void wrongPasswordDoesNotCreateSessionAndDoesNotLeakAccountExistence() throws Exception {
    mvc.perform(
            post("/admin/api/v1/auth/login")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        map("username", security, "password", "incorrect-password"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID"));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_session", Integer.class)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_event WHERE action='login_failed'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void rolesAreSeparatedByPermission() throws Exception {
    String editToken = login(editor);
    String adminToken = login(security);
    String reviewToken = login(reviewer);
    call(get("/admin/api/v1/operators").header("Authorization", "Bearer " + editToken), 403);
    call(get("/admin/api/v1/knowledge").header("Authorization", "Bearer " + adminToken), 403);
    call(
        write(
            post("/admin/api/v1/knowledge"),
            reviewToken,
            1,
            UUID.randomUUID().toString(),
            newArticle()),
        403);
  }

  @Test
  void duplicateOperatorCreationIsIdempotentAndDifferentPayloadConflicts() throws Exception {
    String token = login(security);
    String key = UUID.randomUUID().toString();
    Map<String, Object> request =
        map(
            "clientUuid",
            UUID.randomUUID().toString(),
            "username",
            "extra_" + UUID.randomUUID().toString().substring(0, 8),
            "displayName",
            "新编辑",
            "password",
            PASSWORD,
            "roleCodes",
            Collections.singletonList("content_editor"));
    JsonNode first = call(write(post("/admin/api/v1/operators"), token, 1, key, request), 200);
    JsonNode repeated = call(write(post("/admin/api/v1/operators"), token, 1, key, request), 200);
    assertThat(repeated).isEqualTo(first);
    request.put("displayName", "已更改");
    call(write(post("/admin/api/v1/operators"), token, 1, key, request), 409);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_operator", Integer.class))
        .isEqualTo(5);
  }

  @Test
  void unknownRoleRollsBackAccountAndIdempotencyRecord() throws Exception {
    String token = login(security);
    Map<String, Object> request =
        map(
            "clientUuid",
            UUID.randomUUID().toString(),
            "username",
            "unknown_role_user",
            "displayName",
            "错误角色",
            "password",
            PASSWORD,
            "roleCodes",
            Collections.singletonList("super_everything"));
    call(
        write(post("/admin/api/v1/operators"), token, 1, UUID.randomUUID().toString(), request),
        422);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_operator", Integer.class))
        .isEqualTo(4);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_idempotency", Integer.class))
        .isZero();
  }

  @Test
  void lastSecurityAdminCannotBeDisabled() throws Exception {
    String token = login(security);
    Map<String, Object> request =
        map(
            "displayName",
            "安全管理员",
            "state",
            "disabled",
            "roleCodes",
            Collections.singletonList("security_admin"));
    mvc.perform(
            write(
                patch("/admin/api/v1/operators/" + uuids.get(security)),
                token,
                1,
                UUID.randomUUID().toString(),
                request))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LAST_SECURITY_ADMIN"));
  }

  @Test
  void staleOperatorRevisionDoesNotOverwrite() throws Exception {
    String token = login(security);
    call(
        write(
            patch("/admin/api/v1/operators/" + uuids.get(editor)),
            token,
            2,
            UUID.randomUUID().toString(),
            map(
                "displayName",
                "编辑",
                "state",
                "active",
                "roleCodes",
                Collections.singletonList("content_editor"))),
        409);
  }

  @Test
  void disableOperatorInvalidatesExistingJwtImmediately() throws Exception {
    String editorToken = login(editor);
    String token = login(security);
    call(
        write(
            patch("/admin/api/v1/operators/" + uuids.get(editor)),
            token,
            1,
            UUID.randomUUID().toString(),
            map(
                "displayName",
                "编辑",
                "state",
                "disabled",
                "roleCodes",
                Collections.singletonList("content_editor"))),
        200);
    call(get("/admin/api/v1/me").header("Authorization", "Bearer " + editorToken), 401);
  }

  @Test
  void passwordResetRevokesOldSessionAndOldPassword() throws Exception {
    String old = login(editor);
    String token = login(security);
    call(
        write(
            post("/admin/api/v1/operators/" + uuids.get(editor) + "/password"),
            token,
            1,
            UUID.randomUUID().toString(),
            map("password", "A-new-fixture-password-2026!")),
        200);
    call(get("/admin/api/v1/me").header("Authorization", "Bearer " + old), 401);
    call(
        post("/admin/api/v1/auth/login")
            .contentType("application/json")
            .content(json.writeValueAsString(map("username", editor, "password", PASSWORD))),
        401);
    call(
        post("/admin/api/v1/auth/login")
            .contentType("application/json")
            .content(
                json.writeValueAsString(
                    map("username", editor, "password", "A-new-fixture-password-2026!"))),
        200);
  }

  @Test
  void selfPasswordChangeRequiresOldPasswordAndRevokesSession() throws Exception {
    String token = login(editor);
    call(
        write(
            post("/admin/api/v1/auth/password"),
            token,
            1,
            UUID.randomUUID().toString(),
            map("oldPassword", "wrong", "newPassword", "new-password-2026!")),
        401);
    call(
        write(
            post("/admin/api/v1/auth/password"),
            token,
            1,
            UUID.randomUUID().toString(),
            map("oldPassword", PASSWORD, "newPassword", "new-password-2026!")),
        200);
    call(get("/admin/api/v1/me").header("Authorization", "Bearer " + token), 401);
  }

  @Test
  void submittedContentCannotBeEditedInPlace() throws Exception {
    String token = login(editor);
    String uuid = draft(token).get("uuid").asText();
    submit(token, uuid, 1);
    call(
        write(
            patch("/admin/api/v1/knowledge/versions/" + uuid),
            token,
            2,
            UUID.randomUUID().toString(),
            content()),
        409);
    assertThat(
            jdbc.queryForObject(
                "SELECT revision FROM admin_knowledge_version WHERE client_uuid=?",
                Long.class,
                uuid))
        .isEqualTo(2);
  }

  @Test
  void authorCannotReviewOwnVersionEvenWithReviewerRole() throws Exception {
    jdbc.update(
        "INSERT INTO admin_operator_role(operator_id,role_code) SELECT id,'clinical_reviewer' FROM admin_operator WHERE username=?",
        editor);
    String token = login(editor);
    String uuid = draft(token).get("uuid").asText();
    submit(token, uuid, 1);
    mvc.perform(
            write(
                post("/admin/api/v1/knowledge/versions/" + uuid + "/review"),
                token,
                2,
                UUID.randomUUID().toString(),
                map("decision", "approve", "note", "试图自审")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SELF_REVIEW_FORBIDDEN"));
  }

  @Test
  void unapprovedVersionCannotBePublished() throws Exception {
    String uuid = draft(login(editor)).get("uuid").asText();
    call(
        write(
            post("/admin/api/v1/knowledge/versions/" + uuid + "/publish"),
            login(publisher),
            1,
            UUID.randomUUID().toString(),
            null),
        409);
  }

  @Test
  void independentReviewAndPublishAreAtomicAndIdempotent() throws Exception {
    String editorToken = login(editor);
    JsonNode draft = draft(editorToken);
    String uuid = draft.get("uuid").asText();
    submit(editorToken, uuid, 1);
    review(login(reviewer), uuid, "approve");
    String publishToken = login(publisher);
    String key = UUID.randomUUID().toString();
    JsonNode first =
        call(
            write(
                post("/admin/api/v1/knowledge/versions/" + uuid + "/publish"),
                publishToken,
                3,
                key,
                null),
            200);
    JsonNode replay =
        call(
            write(
                post("/admin/api/v1/knowledge/versions/" + uuid + "/publish"),
                publishToken,
                3,
                key,
                null),
            200);
    assertThat(first).isEqualTo(replay);
    mvc.perform(
            get("/admin/api/v1/knowledge/versions/" + uuid)
                .header("Authorization", "Bearer " + editorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.currentlyPublished").value(true));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_event WHERE action='published'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void newDraftDoesNotOverwritePublishedSnapshot() throws Exception {
    String e = login(editor);
    JsonNode initial = draft(e);
    String uuid = initial.get("uuid").asText();
    String article = initial.get("parentUuid").asText();
    submit(e, uuid, 1);
    review(login(reviewer), uuid, "approve");
    call(
        write(
            post("/admin/api/v1/knowledge/versions/" + uuid + "/publish"),
            login(publisher),
            3,
            UUID.randomUUID().toString(),
            null),
        200);
    Map<String, Object> request = content();
    request.put("clientUuid", UUID.randomUUID().toString());
    request.put("title", "新的草稿");
    call(
        write(
            post("/admin/api/v1/knowledge/" + article + "/versions"),
            e,
            2,
            UUID.randomUUID().toString(),
            request),
        200);
    mvc.perform(
            get("/admin/api/v1/knowledge/versions/" + uuid).header("Authorization", "Bearer " + e))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("测试知识草稿"))
        .andExpect(jsonPath("$.data.currentlyPublished").value(true));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_knowledge_version", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void rejectedVersionIsImmutableAndCanHaveNewDraft() throws Exception {
    String e = login(editor);
    JsonNode initial = draft(e);
    String uuid = initial.get("uuid").asText();
    submit(e, uuid, 1);
    review(login(reviewer), uuid, "reject");
    call(
        write(
            patch("/admin/api/v1/knowledge/versions/" + uuid),
            e,
            3,
            UUID.randomUUID().toString(),
            content()),
        409);
    Map<String, Object> request = content();
    request.put("clientUuid", UUID.randomUUID().toString());
    call(
        write(
            post("/admin/api/v1/knowledge/" + initial.get("parentUuid").asText() + "/versions"),
            e,
            1,
            UUID.randomUUID().toString(),
            request),
        200);
  }

  @Test
  void withdrawActuallyClearsNullablePublishedPointer() throws Exception {
    String e = login(editor);
    JsonNode initial = draft(e);
    String uuid = initial.get("uuid").asText();
    String article = initial.get("parentUuid").asText();
    submit(e, uuid, 1);
    review(login(reviewer), uuid, "approve");
    String p = login(publisher);
    call(
        write(
            post("/admin/api/v1/knowledge/versions/" + uuid + "/publish"),
            p,
            3,
            UUID.randomUUID().toString(),
            null),
        200);
    call(
        write(
            post("/admin/api/v1/knowledge/" + article + "/withdraw"),
            p,
            2,
            UUID.randomUUID().toString(),
            null),
        200);
    assertThat(
            jdbc.queryForObject(
                "SELECT published_version_id FROM admin_knowledge_article WHERE client_uuid=?",
                Long.class,
                article))
        .isNull();
    mvc.perform(
            get("/admin/api/v1/knowledge/versions/" + uuid).header("Authorization", "Bearer " + e))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.currentlyPublished").value(false))
        .andExpect(jsonPath("$.data.state").value("withdrawn"));
  }

  @Test
  void emptyReferencesCannotBeSubmitted() throws Exception {
    String e = login(editor);
    Map<String, Object> request = newArticle();
    request.put("references", Collections.emptyList());
    String uuid =
        call(
                write(post("/admin/api/v1/knowledge"), e, 1, UUID.randomUUID().toString(), request),
                200)
            .get("uuid")
            .asText();
    call(
        write(
            post("/admin/api/v1/knowledge/versions/" + uuid + "/submit"),
            e,
            1,
            UUID.randomUUID().toString(),
            null),
        422);
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM admin_knowledge_version WHERE client_uuid=?",
                String.class,
                uuid))
        .isEqualTo("draft");
  }

  @Test
  void otherEditorCannotModifyOriginalAuthorsDraft() throws Exception {
    String uuid = draft(login(editor)).get("uuid").asText();
    String other =
        fixture(
            "other_" + UUID.randomUUID().toString().substring(0, 8),
            passwords.encode(PASSWORD),
            "content_editor");
    call(
        write(
            patch("/admin/api/v1/knowledge/versions/" + uuid),
            login(other),
            1,
            UUID.randomUUID().toString(),
            content()),
        403);
  }

  @Test
  void missingVersionAndIdempotencyHeadersAreRejected() throws Exception {
    String e = login(editor);
    String uuid = draft(e).get("uuid").asText();
    call(
        post("/admin/api/v1/knowledge/versions/" + uuid + "/submit")
            .header("Authorization", "Bearer " + e)
            .header("Idempotency-Key", UUID.randomUUID().toString()),
        428);
    call(
        post("/admin/api/v1/knowledge")
            .header("Authorization", "Bearer " + e)
            .contentType("application/json")
            .content(json.writeValueAsString(newArticle())),
        400);
  }

  @Test
  void duplicateSlugDoesNotCreateOrphanVersion() throws Exception {
    String e = login(editor);
    Map<String, Object> body = newArticle();
    call(write(post("/admin/api/v1/knowledge"), e, 1, UUID.randomUUID().toString(), body), 200);
    body.put("clientUuid", UUID.randomUUID().toString());
    body.put("versionUuid", UUID.randomUUID().toString());
    call(write(post("/admin/api/v1/knowledge"), e, 1, UUID.randomUUID().toString(), body), 409);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_knowledge_version", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void paginationIsStableAndReturnsNoBodyText() throws Exception {
    String e = login(editor);
    draft(e);
    draft(e);
    draft(e);
    JsonNode first =
        call(get("/admin/api/v1/knowledge?limit=1").header("Authorization", "Bearer " + e), 200);
    assertThat(first.get("records").size()).isEqualTo(1);
    assertThat(first.get("records").get(0).has("proContent")).isFalse();
    JsonNode second =
        call(
            get("/admin/api/v1/knowledge?limit=1&beforeId=" + first.get("nextBeforeId").asLong())
                .header("Authorization", "Bearer " + e),
            200);
    assertThat(second.get("records").get(0).get("uuid"))
        .isNotEqualTo(first.get("records").get(0).get("uuid"));
    call(get("/admin/api/v1/knowledge?limit=101").header("Authorization", "Bearer " + e), 422);
  }

  @Test
  void auditFailureRollsBackArticleVersionAndCommandReceipt() throws Exception {
    String e = login(editor);
    doThrow(new IllegalStateException("fixture audit failure"))
        .when(audit)
        .record(anyLong(), eq("knowledge_version"), anyString(), eq("draft_created"), eq("OK"));
    call(
        write(post("/admin/api/v1/knowledge"), e, 1, UUID.randomUUID().toString(), newArticle()),
        500);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_knowledge_article", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_knowledge_version", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_idempotency", Integer.class))
        .isZero();
  }

  @Test
  void auditPermissionAndMetadataAreRestricted() throws Exception {
    String e = login(editor);
    draft(e);
    call(get("/admin/api/v1/audits").header("Authorization", "Bearer " + e), 403);
    JsonNode result =
        call(get("/admin/api/v1/audits").header("Authorization", "Bearer " + login(security)), 200);
    String output = result.toString();
    assertThat(output)
        .doesNotContain("password", "proContent", "plainContent", "accessToken", PASSWORD);
  }

  @Test
  void concurrentSameKeyCreatesExactlyOneArticle() throws Exception {
    String e = login(editor);
    Map<String, Object> request = newArticle();
    String key = UUID.randomUUID().toString();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<JsonNode> attempt =
        () -> {
          start.await();
          return call(write(post("/admin/api/v1/knowledge"), e, 1, key, request), 200);
        };
    try {
      Future<JsonNode> first = pool.submit(attempt);
      Future<JsonNode> second = pool.submit(attempt);
      start.countDown();
      assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(second.get(15, TimeUnit.SECONDS));
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_knowledge_article", Integer.class))
          .isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM admin_audit_event WHERE action='draft_created'",
                  Integer.class))
          .isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void redisAccountLimitRejectsEleventhRequest() {
    String name = "rate_" + UUID.randomUUID();
    String ip = "fixture_" + UUID.randomUUID();
    for (int i = 0; i < 10; i++) {
      limiter.check(ip, name);
    }
    assertThatThrownBy(() -> limiter.check(ip, name))
        .isInstanceOf(BizException.class)
        .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(429));
  }

  @Test
  void bootstrapNeverResetsExistingOperators() {
    properties.setBootstrapEnabled(true);
    properties.setBootstrapUsername(security);
    properties.setBootstrapPassword("different-bootstrap-password");
    bootstrap.run(new DefaultApplicationArguments());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_operator", Integer.class))
        .isEqualTo(4);
    assertThat(
            passwords.matches(
                PASSWORD,
                jdbc.queryForObject(
                    "SELECT password_hash FROM admin_operator WHERE username=?",
                    String.class,
                    security)))
        .isTrue();
  }
}
