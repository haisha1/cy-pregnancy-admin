package com.chunyu.pregnancy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;
import com.chunyu.pregnancy.admin.entity.*;
import com.chunyu.pregnancy.admin.mapper.*;
import com.chunyu.pregnancy.admin.service.KnowledgeService;
import com.chunyu.pregnancy.admin.service.support.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 知识状态机实现。业务状态变化、发布指针、幂等回执和成功审计在同一事务提交。 已送审内容不编辑；拒绝、撤回或已发布后，需要创建新的完整草稿版本。 */
// 业务层组件：由 Spring 管理，负责组织业务步骤；事务边界仍需由事务代码明确控制。
@Service
public class KnowledgeServiceImpl implements KnowledgeService {
  private final KnowledgeArticleMapper articles;
  private final KnowledgeVersionMapper versions;
  private final AdminOperatorMapper operators;
  private final CommandExecutor commands;
  private final AuditWriter audit;
  private final ObjectMapper json;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public KnowledgeServiceImpl(
      KnowledgeArticleMapper articles,
      KnowledgeVersionMapper versions,
      AdminOperatorMapper operators,
      CommandExecutor commands,
      AuditWriter audit,
      ObjectMapper json) {
    this.articles = articles;
    this.versions = versions;
    this.operators = operators;
    this.commands = commands;
    this.audit = audit;
    this.json = json;
  }

  @Override
  // 游标分页按 ID 倒序；多查一条判断是否还有下一页，不伪造总页数。
  public PageResponse<KnowledgeSummaryResponse> list(Long beforeId, int limit) {
    checkLimit(limit);
    List<KnowledgeArticle> rows =
        articles.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .lt(beforeId != null, KnowledgeArticle::getId, beforeId)
                .orderByDesc(KnowledgeArticle::getId)
                .last("LIMIT " + (limit + 1)));
    boolean more = rows.size() > limit;
    if (more) {
      rows = rows.subList(0, limit);
    }
    List<KnowledgeSummaryResponse> records = new ArrayList<>();
    for (KnowledgeArticle article : rows) {
      KnowledgeVersion latest =
          versions.selectOne(
              summaryQuery()
                  .eq(KnowledgeVersion::getArticleId, article.getId())
                  .orderByDesc(KnowledgeVersion::getVersionNo)
                  .last("LIMIT 1"));
      records.add(summary(article, latest));
    }
    return new PageResponse<>(records, more ? rows.get(rows.size() - 1).getId() : null);
  }

  @Override
  public PageResponse<KnowledgeSummaryResponse> versions(
      String articleUuid, Long beforeId, int limit) {
    checkLimit(limit);
    KnowledgeArticle article = article(articleUuid);
    List<KnowledgeVersion> rows =
        versions.selectList(
            summaryQuery()
                .eq(KnowledgeVersion::getArticleId, article.getId())
                .lt(beforeId != null, KnowledgeVersion::getId, beforeId)
                .orderByDesc(KnowledgeVersion::getId)
                .last("LIMIT " + (limit + 1)));
    boolean more = rows.size() > limit;
    if (more) {
      rows = rows.subList(0, limit);
    }
    return new PageResponse<>(
        rows.stream().map(version -> summary(article, version)).collect(Collectors.toList()),
        more ? rows.get(rows.size() - 1).getId() : null);
  }

  @Override
  public KnowledgeResponse detail(String versionUuid) {
    KnowledgeVersion version = version(versionUuid);
    KnowledgeArticle article = articles.selectById(version.getArticleId());
    KnowledgeResponse result = new KnowledgeResponse();
    result.setArticleUuid(article.getClientUuid());
    result.setSlug(article.getSlug());
    result.setArticleRevision(article.getRevision());
    result.setUuid(version.getClientUuid());
    result.setVersionNo(version.getVersionNo());
    result.setTitle(version.getTitle());
    result.setCategory(version.getCategory());
    result.setProContent(version.getProContent());
    result.setPlainContent(version.getPlainContent());
    result.setReferences(readReferences(version.getReferencesJson()));
    result.setEvidenceNote(version.getEvidenceNote());
    result.setState(version.getState());
    result.setRevision(version.getRevision());
    result.setReviewNote(version.getReviewNote());
    result.setAuthorUuid(operators.selectById(version.getAuthorId()).getClientUuid());
    if (version.getReviewerId() != null) {
      result.setReviewerUuid(operators.selectById(version.getReviewerId()).getClientUuid());
    }
    result.setPublishedAt(version.getPublishedAt());
    result.setCurrentlyPublished(version.getId().equals(article.getPublishedVersionId()));
    return result;
  }

  @Override
  // 在同一写事务中建立条目身份和首个草稿，避免只保存一半。
  public MutationResponse create(Long actor, String key, CreateArticleRequest request) {
    CommandExecutor.validateUuid(request.clientUuid);
    CommandExecutor.validateUuid(request.versionUuid);
    return commands.execute(
        actor,
        "knowledge:edit",
        "knowledge.create",
        key,
        request,
        () -> {
          if (articles.selectCount(
                  new LambdaQueryWrapper<KnowledgeArticle>()
                      .eq(KnowledgeArticle::getClientUuid, request.clientUuid)
                      .or()
                      .eq(KnowledgeArticle::getSlug, request.slug))
              > 0) {
            throw conflict("条目标识或slug已存在");
          }
          ensureVersionUuidUnused(request.versionUuid);
          KnowledgeArticle article = new KnowledgeArticle();
          article.setClientUuid(request.clientUuid);
          article.setSlug(request.slug);
          article.setRevision(1L);
          article.setCreatedAt(Instant.now());
          articles.insert(article);
          KnowledgeVersion version = draft(article, request.versionUuid, 1, actor, request);
          versions.insert(version);
          audit.record(actor, "knowledge_version", version.getClientUuid(), "draft_created", "OK");
          return mutation(article, version);
        });
  }

  @Override
  // 保留原版本，创建新的完整草稿；历史审核和已发布正文不会被就地覆盖。
  public MutationResponse newVersion(
      Long actor, String articleUuid, long expected, String key, NewVersionRequest request) {
    CommandExecutor.validateUuid(request.clientUuid);
    return commands.execute(
        actor,
        "knowledge:edit",
        "knowledge.new_version",
        key,
        new Object[] {articleUuid, expected, request},
        () -> {
          KnowledgeArticle article = article(articleUuid);
          CommandExecutor.requireRevision(article.getRevision(), expected);
          ensureVersionUuidUnused(request.clientUuid);
          KnowledgeVersion latest =
              versions.selectOne(
                  new LambdaQueryWrapper<KnowledgeVersion>()
                      .eq(KnowledgeVersion::getArticleId, article.getId())
                      .orderByDesc(KnowledgeVersion::getVersionNo)
                      .last("LIMIT 1"));
          KnowledgeVersion version =
              draft(article, request.clientUuid, latest.getVersionNo() + 1, actor, request);
          versions.insert(version);
          article.setRevision(article.getRevision() + 1);
          articles.updateById(article);
          audit.record(actor, "knowledge_version", version.getClientUuid(), "draft_created", "OK");
          return mutation(article, version);
        });
  }

  @Override
  public MutationResponse edit(
      Long actor, String uuid, long expected, String key, DraftContentRequest request) {
    return commands.execute(
        actor,
        "knowledge:edit",
        "knowledge.edit",
        key,
        new Object[] {uuid, expected, request},
        () -> {
          KnowledgeVersion version = ownedDraft(actor, uuid, expected);
          applyContent(version, request);
          bump(version);
          audit.record(actor, "knowledge_version", uuid, "draft_edited", "OK");
          return mutation(articles.selectById(version.getArticleId()), version);
        });
  }

  @Override
  // 只允许作者提交自己的草稿，并在进入 submitted 状态前检查来源依据。
  public MutationResponse submit(Long actor, String uuid, long expected, String key) {
    return commands.execute(
        actor,
        "knowledge:edit",
        "knowledge.submit",
        key,
        new Object[] {uuid, expected},
        () -> {
          KnowledgeVersion version = ownedDraft(actor, uuid, expected);
          if (readReferences(version.getReferencesJson()).isEmpty()) {
            throw new BizException(422, "REFERENCES_REQUIRED", "送审前必须补充来源和审核依据");
          }
          version.setState("submitted");
          bump(version);
          audit.record(actor, "knowledge_version", uuid, "submitted", "OK");
          return mutation(articles.selectById(version.getArticleId()), version);
        });
  }

  @Override
  // 只有 submitted 版本可审核；比较作者 ID，明确禁止给自己的稿件审核。
  public MutationResponse review(
      Long actor, String uuid, long expected, String key, ReviewRequest request) {
    return commands.execute(
        actor,
        "knowledge:review",
        "knowledge.review",
        key,
        new Object[] {uuid, expected, request},
        () -> {
          KnowledgeVersion version = version(uuid);
          CommandExecutor.requireRevision(version.getRevision(), expected);
          requireState(version, "submitted");
          if (actor.equals(version.getAuthorId())) {
            throw new BizException(403, "SELF_REVIEW_FORBIDDEN", "作者不能审核自己的内容版本");
          }
          version.setReviewerId(actor);
          version.setReviewNote(request.note);
          version.setState("approve".equals(request.decision) ? "approved" : "rejected");
          bump(version);
          audit.record(actor, "knowledge_version", uuid, version.getState(), "OK");
          return mutation(articles.selectById(version.getArticleId()), version);
        });
  }

  @Override
  // 通过独立审核后才能发布；将条目的当前发布指针切到此版本，用户侧只读链路跟随该指针。
  public MutationResponse publish(Long actor, String uuid, long expected, String key) {
    return commands.execute(
        actor,
        "knowledge:publish",
        "knowledge.publish",
        key,
        new Object[] {uuid, expected},
        () -> {
          KnowledgeVersion version = version(uuid);
          CommandExecutor.requireRevision(version.getRevision(), expected);
          requireState(version, "approved");
          if (version.getReviewerId() == null
              || version.getReviewerId().equals(version.getAuthorId())) {
            throw new BizException(409, "REVIEW_REQUIRED", "缺少独立审核记录");
          }
          KnowledgeArticle article = articles.selectById(version.getArticleId());
          version.setState("published");
          version.setPublisherId(actor);
          version.setPublishedAt(Instant.now());
          bump(version);
          // published 状态记录版本历史；这条指针才决定哪一版是当前对外可见的内容。
          article.setPublishedVersionId(version.getId());
          article.setRevision(article.getRevision() + 1);
          articles.updateById(article);
          audit.record(actor, "knowledge_version", uuid, "published", "OK");
          return mutation(article, version);
        });
  }

  @Override
  // 清空条目的当前发布指针，并保留版本与审计历史，不把下架等同于物理删除。
  public MutationResponse withdraw(Long actor, String articleUuid, long expected, String key) {
    return commands.execute(
        actor,
        "knowledge:publish",
        "knowledge.withdraw",
        key,
        new Object[] {articleUuid, expected},
        () -> {
          KnowledgeArticle article = article(articleUuid);
          CommandExecutor.requireRevision(article.getRevision(), expected);
          if (article.getPublishedVersionId() == null) {
            throw conflict("该条目当前没有发布版本");
          }
          KnowledgeVersion published = versions.selectById(article.getPublishedVersionId());
          published.setState("withdrawn");
          bump(published);
          // MyBatis-Plus默认不更新null字段，必须显式set为NULL，不能只调用updateById。
          articles.update(
              null,
              new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                      KnowledgeArticle>()
                  .eq(KnowledgeArticle::getId, article.getId())
                  .set(KnowledgeArticle::getPublishedVersionId, null)
                  .set(KnowledgeArticle::getRevision, article.getRevision() + 1));
          audit.record(actor, "knowledge_article", articleUuid, "withdrawn", "OK");
          return new MutationResponse(articleUuid, article.getRevision() + 1, "withdrawn", null);
        });
  }

  private KnowledgeVersion draft(
      KnowledgeArticle article, String uuid, int number, Long author, DraftContentRequest request) {
    KnowledgeVersion version = new KnowledgeVersion();
    version.setClientUuid(uuid);
    version.setArticleId(article.getId());
    version.setVersionNo(number);
    version.setAuthorId(author);
    version.setState("draft");
    version.setRevision(1L);
    version.setCreatedAt(Instant.now());
    applyContent(version, request);
    return version;
  }

  private void applyContent(KnowledgeVersion version, DraftContentRequest request) {
    version.setTitle(request.title);
    version.setCategory(request.category);
    version.setProContent(request.proContent);
    version.setPlainContent(request.plainContent);
    version.setEvidenceNote(request.evidenceNote);
    try {
      version.setReferencesJson(json.writeValueAsString(request.references));
    } catch (Exception ex) {
      throw new IllegalStateException("无法序列化知识引用");
    }
  }

  private KnowledgeVersion ownedDraft(Long actor, String uuid, long expected) {
    KnowledgeVersion version = version(uuid);
    CommandExecutor.requireRevision(version.getRevision(), expected);
    requireState(version, "draft");
    if (!actor.equals(version.getAuthorId())) {
      throw new BizException(403, "AUTHOR_REQUIRED", "只有该草稿作者可以编辑或送审");
    }
    return version;
  }

  private void bump(KnowledgeVersion version) {
    version.setRevision(version.getRevision() + 1);
    versions.updateById(version);
  }

  private KnowledgeArticle article(String uuid) {
    KnowledgeArticle article =
        articles.selectOne(
            new LambdaQueryWrapper<KnowledgeArticle>().eq(KnowledgeArticle::getClientUuid, uuid));
    if (article == null) {
      throw new BizException(404, "RESOURCE_NOT_FOUND", "知识条目不存在");
    }
    return article;
  }

  private KnowledgeVersion version(String uuid) {
    KnowledgeVersion version =
        versions.selectOne(
            new LambdaQueryWrapper<KnowledgeVersion>().eq(KnowledgeVersion::getClientUuid, uuid));
    if (version == null) {
      throw new BizException(404, "RESOURCE_NOT_FOUND", "知识版本不存在");
    }
    return version;
  }

  private void ensureVersionUuidUnused(String uuid) {
    if (versions.selectCount(
            new LambdaQueryWrapper<KnowledgeVersion>().eq(KnowledgeVersion::getClientUuid, uuid))
        > 0) {
      throw conflict("知识版本标识已存在");
    }
  }

  private void requireState(KnowledgeVersion version, String state) {
    if (!state.equals(version.getState())) {
      throw new BizException(409, "STATE_CONFLICT", "当前内容状态不允许此操作");
    }
  }

  private MutationResponse mutation(KnowledgeArticle article, KnowledgeVersion version) {
    return new MutationResponse(
        version.getClientUuid(),
        version.getRevision(),
        version.getState(),
        article.getClientUuid());
  }

  private List<String> readReferences(String value) {
    try {
      return json.readValue(value, new TypeReference<List<String>>() {});
    } catch (Exception ex) {
      throw new IllegalStateException("知识引用数据无效");
    }
  }

  private LambdaQueryWrapper<KnowledgeVersion> summaryQuery() {
    return new LambdaQueryWrapper<KnowledgeVersion>()
        .select(
            KnowledgeVersion::getId,
            KnowledgeVersion::getClientUuid,
            KnowledgeVersion::getVersionNo,
            KnowledgeVersion::getTitle,
            KnowledgeVersion::getCategory,
            KnowledgeVersion::getState,
            KnowledgeVersion::getRevision);
  }

  private KnowledgeSummaryResponse summary(KnowledgeArticle article, KnowledgeVersion version) {
    return new KnowledgeSummaryResponse(
        article.getClientUuid(),
        article.getSlug(),
        article.getRevision(),
        version.getClientUuid(),
        version.getVersionNo(),
        version.getTitle(),
        version.getCategory(),
        version.getState(),
        version.getRevision(),
        version.getId().equals(article.getPublishedVersionId()));
  }

  private void checkLimit(int limit) {
    if (limit < 1 || limit > 100) {
      throw new BizException(422, "PAGE_INVALID", "每页数量必须为1–100");
    }
  }

  private BizException conflict(String message) {
    return new BizException(409, "RESOURCE_CONFLICT", message);
  }
}
