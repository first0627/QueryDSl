package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberTeamDto {

  private Long memberId;
  private String username;
  private int age;
  private Long teamId;
  private String teamName;

  // QueryProjection을 사용하면 DTO에 Querydsl의 Q타입을 의존하게 되는데, 이는 DTO가 Querydsl에 의존하게 되는 단점이 있다.
  @QueryProjection
  public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
    this.memberId = memberId;
    this.username = username;
    this.age = age;
    this.teamId = teamId;
    this.teamName = teamName;
  }
}
