package study.querydsl.controller;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

  //

  private final InitMemberService initMemberService;

  @PostConstruct
  // 이 PostConstruct와 아래 Transactional은 Spring LifeCycle 때문에 이렇게 따로 빼준 것이다.
  // @PostConstruct는 해당 빈 자체만 생성되었다고 가정하고 호출됩니다. 해당 빈에 관련된 AOP등을 포함한, 전체 스프링 애플리케이션 컨텍스트가 초기화 된 것을
  // 의미하지는 않습니다.
  // 트랜잭션을 처리하는 AOP등은 스프링의 후 처리기(post processer)가 완전히 동작을 끝내서, 스프링 애플리케이션 컨텍스트의 초기화가 완료되어야 적용됩니다.
  // 정리하면 @PostConstruct는 해당빈의 AOP 적용을 보장하지 않습니다.
  // 이런 것을 우회하는 여러가지 방법이있는데요. 제가 보여드린 방법(다른 스프링 빈을 호출해서 사용하는 방법)을 포함해서, AOP를 사용하지 않고 트랜잭션을 직접 코딩하는
  // 방법, 애플리케이션 컨텍스트가 완전히 초기화 된 이벤트를 받아서 호출하는 방법 등이 있습니다.
  public void init() {
    initMemberService.init();
  }

  @Component
  static class InitMemberService {

    @PersistenceContext private EntityManager em;

    @Transactional
    public void init() {
      Team teamA = new Team("teamA");
      Team teamB = new Team("teamB");
      em.persist(teamA);
      em.persist(teamB);

      for (int i = 0; i < 100; i++) {
        Team selectedTeam = i % 2 == 0 ? teamA : teamB;
        em.persist(new Member("member" + i, i, selectedTeam));
      }
    }
  }
}
