package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

public class MemberRepositoryImpl implements MemberRepositoryCustom {
  private final JPAQueryFactory queryFactory;

  public MemberRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }

  private JPAQuery<Long> getTotal(MemberSearchCondition condition) {

    JPAQuery<Long> result =
        queryFactory
            .select(member.count()) // SQL 상으로는 count(member.id)와 동일
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()));

    return result;
  }

  public List<MemberTeamDto> search(MemberSearchCondition conditionition) {
    return queryFactory
        .select(
            new QMemberTeamDto(
                member.id,
                member.username,
                member.age,
                team.id.as("teamId"),
                team.name.as("teamName")))
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(conditionition.getUsername()),
            teamNameEq(conditionition.getTeamName()),
            ageGoe(conditionition.getAgeGoe()),
            ageLoe(conditionition.getAgeLoe()))
        .fetch();
  }

  @Override
  public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
    // 데이터 조회 쿼리 (페이징 적용)
    List<MemberTeamDto> content = getMemberTeamDtos(condition, pageable);

    // count 쿼리 (조건에 부합하는 로우의 총 개수를 얻는 것이기 때에 페이징 미적용)
    JPAQuery<Long> countQuery = getTotal(condition);

    // getPage에서 컨텐츠 사이즈가 0이면 countQuery를 실행하지 않는다.
    // 컨텐츠 사이즈가 페이지사이즈보다 작거나 마지막 페이지거나 이러면 count query 호출안함
    // 이걸쓰면 된다는말~~Spring data JPA가 제공하는 기능이다.
    // 여기서 판단한다.
    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }

  private List<MemberTeamDto> getMemberTeamDtos(
      MemberSearchCondition condition, Pageable pageable) {
    return queryFactory
        .select(
            new QMemberTeamDto(
                member.id.as("memberId"),
                member.username,
                member.age,
                team.id.as("teamId"),
                team.name.as("teamName")))
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  private BooleanExpression usernameEq(String username) {
    return hasText(username) ? member.username.eq(username) : null;
  }

  private BooleanExpression teamNameEq(String teamName) {
    return hasText(teamName) ? team.name.eq(teamName) : null;
  }

  private BooleanExpression ageGoe(Integer ageGoe) {
    return ageGoe != null ? member.age.goe(ageGoe) : null;
  }

  private BooleanExpression ageLoe(Integer ageLoe) {
    return ageLoe != null ? member.age.loe(ageLoe) : null;
  }
}
