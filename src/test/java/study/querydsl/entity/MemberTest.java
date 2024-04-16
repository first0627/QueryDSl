package study.querydsl.entity;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional

class MemberTest {

  @Autowired EntityManager em;

  @Test
  public void testEntity() {
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);

    // 초기화
    em.flush();
    em.clear();

    // 확인
    Member findMember1 = em.find(Member.class, member1.getId());
    Member findMember2 = em.find(Member.class, member2.getId());
    Member findMember3 = em.find(Member.class, member3.getId());
    Member findMember4 = em.find(Member.class, member4.getId());

    assertEquals(findMember1.getTeam().getName(), "teamA");
    assertEquals(findMember2.getTeam().getName(), "teamA");
    assertEquals(findMember3.getTeam().getName(), "teamB");
    assertEquals(findMember4.getTeam().getName(), "teamB");

    for (Member member : findMember1.getTeam().getMembers()) {
      System.out.println("member = " + member);
    }
  }
}
